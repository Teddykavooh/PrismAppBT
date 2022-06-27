package com.edit.prismappbt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class MainActivity extends AppCompatActivity implements AppsDialog.OnAppSelectedListener {
    ArrayList<String> smsMessagesList = new ArrayList<>();
    ListView messages;
    ArrayAdapter<String> arrayAdapter;
    @SuppressLint("StaticFieldLeak")
    private static MainActivity inst;
    private Context mContext;
    private Activity mActivity;
    private static final int MY_PERMISSIONS_REQUEST_CODE = 123;
    private Button prnAct;
    private Button prnDea;
    private String deviceId;
    int powerLaunch = 0;
    SharedPreferences myData;
    private String spHeader;
    private String spFooter;

    // android built in classes for bluetooth operations
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;

    // needed for communication to bluetooth device / network
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    RadioGroup myGroup2;
    private RadioButton radioButton;
    TextView myLabel;
    EditText myTextBox;
    RelativeLayout myLay2;
    String msg;
    int btStatus = 0;
    int btConnStatus = 0;
    private String newText;

    /*Intercepting messages.*/

    public static MainActivity instance() {
        return inst;
    }

    //MENU

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem icnOn = menu.findItem(R.id.prnOnIcon);
        MenuItem icnOff = menu.findItem(R.id.prnOffIcon);
        MenuItem printerOn = menu.findItem(R.id.printerOn);
        MenuItem printerOff = menu.findItem(R.id.printerOff);
        MenuItem bt = menu.findItem(R.id.bt);
        MenuItem bt2 = menu.findItem(R.id.bt2);
        MenuItem bt3 = menu.findItem(R.id.bt3);
        if (btConnStatus == 1) {
            icnOff.setVisible(false);
            icnOn.setVisible(true);
            printerOn.setVisible(false);
            printerOff.setVisible(true);
            bt.setVisible(false);
            bt3.setVisible(true);
        }
        if (btStatus == 1) {
            bt2.setVisible(true);
            bt.setVisible(false);
        }

        if (btStatus == 1 && btConnStatus == 1) {
            bt2.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.bt:
                Toast.makeText(getApplicationContext(), "Bluetooth is OFF",
                        Toast.LENGTH_SHORT).show();
                return  true;
            case R.id.bt2:
                Toast.makeText(getApplicationContext(), "Bluetooth is ON",
                        Toast.LENGTH_SHORT).show();
                return  true;
            case R.id.bt3:
                Toast.makeText(getApplicationContext(), "Bluetooth is connected to: " +
                        mmDevice.getName(), Toast.LENGTH_SHORT).show();
                return  true;
            case R.id.delete:
                //delete alert Dialog
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete")
                        .setMessage("All threads will be deleted")
                        .setPositiveButton("DELETE", (dialogInterface, i) -> {
                            //code that will be run if someone chooses delete
                            if (deleteAll()) {
                                smsMessagesList.clear();
                                arrayAdapter.notifyDataSetChanged();
                            } else {
                                Toast.makeText(getApplicationContext(), "Delete all failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("CANCEL", null)
                        .show();
                return true;
            case R.id.exit:
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.ic_baseline_exit_to_app_24)
                        .setTitle("EXIT")
                        .setMessage("Quit all application processes?")
                        .setPositiveButton("EXIT", (dialogInterface, i) -> {
                            onDestroy();
                            finishAffinity();
                            System.exit(0);
                        })
                        .setNegativeButton("BACK", null)
                        .show();
            default:
                return false;
        }
    }

    public void btConn() {
        powerLaunch = 1;
        btConnStatus = 1;
        prnAct.setVisibility(View.INVISIBLE);
        prnDea.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
    }

    public void iconOff(MenuItem i) {
        Toast.makeText(getApplicationContext(), "Click activate printing to enable.",
                Toast.LENGTH_SHORT).show();
    }

    public void iconOn(MenuItem i) {
        Toast.makeText(getApplicationContext(), "Click deactivate printing to disable.",
                Toast.LENGTH_SHORT).show();
    }

    public void licenceCheck(MenuItem i) {
        if (deviceId.equals("")) {
            Toast.makeText(getApplicationContext(), "You are licensed.",
                    Toast.LENGTH_SHORT).show();

            //System.out.println("Device ID: " + deviceId);
        } else {
            Toast.makeText(getApplicationContext(), "You are unlicensed.\nYour ID is: " +
                            deviceId,
                    Toast.LENGTH_SHORT).show();
            //System.out.println("Device ID: " + deviceId);
        }
    }

    public void refreshInbox(MenuItem i) {
        refreshSmsInbox();
    }

    //Printer Activation and Deactivation
    public void printOn(MenuItem i) {
        if (deviceId.equals("")) {
            /*set BT Conn ON*/

            //Privileged
            myLay2.setVisibility(View.VISIBLE);
            /*findBT();
            openBT();*/
//            Intent intent=new Intent(MainActivity.this,BTConnActivity.class);
//            startActivity(intent);

            if (btConnStatus == 1) {
                Toast.makeText(getApplicationContext(), "Printing activated.",
                        Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu();
//            b1.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(getApplicationContext(), "You are not verified for this service.",
                    Toast.LENGTH_SHORT).show();
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("VERIFICATION")
                    .setMessage("In need of activation?\n" + "Call 0721555001, 0726465617\n"
                            + "or\n" + "email: androidposkenya.co.ke")
                    .setNegativeButton("BACK", null)
                    .show();
        }
    }

    public void printOff(MenuItem i) {
        /*set BT Conn OFF*/
        closeBT();
        powerLaunch = 0;
        myLay2.setVisibility(View.GONE);
        prnDea.setVisibility(View.INVISIBLE);
        prnAct.setVisibility(View.VISIBLE);
        Toast.makeText(getApplicationContext(), "Printing deactivated.",
                Toast.LENGTH_SHORT).show();
        invalidateOptionsMenu();
//        b1.setVisibility(View.INVISIBLE);

    }

    public void onClickBmp(MenuItem i) {
        /*if (powerLaunch == 1) {

        } else {
            Toast.makeText(getApplicationContext(), "Activate Print to continue",
                    Toast.LENGTH_SHORT).show();
        }*/
    }

    public void onAbout(MenuItem i) {
        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }

    public void mkDefault(MenuItem i) {
        msgAppChooser();
    }

    public void onStyle(MenuItem i) {
        Intent aboutIntent = new Intent(MainActivity.this, ReceiptStyleActivity.class);
        startActivity(aboutIntent);
    }

    public void onArrBack(View v) {
        myLay2.setVisibility(View.GONE);
    }

//    public void onArrBack2(View v) {
//        setContentView(R.layout.activity_main);
//    }

    public void onPrnOpen() {
        if (powerLaunch == 1) {
            msg = "\n" + "\n" + "\n" + "\n";
            sendData();
        } else {
            Toast.makeText(getApplicationContext(), "Activate Print to continue",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        inst = this;
//        System.out.println("onStart method power launch value: " + powerLaunch);
    }

    public void onFind(View v) {
        // find BT Devices
        findBT();
    }

    public void onConn(View v) {
// Make connection to BT Device
        openBT();
    }

    public void onTest(View v) {
        // send data typed by the user to be printed
        msg = myTextBox.getText().toString() + "\n" + "\n" + "\n" + "\n";
        sendData();
    }

    public void onCloseBT(View v) {
        // close bluetooth connection
        closeBT();
    }

    //Bt Broadcast
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //Bluetooth off;
                        Toast.makeText(getApplicationContext(), "Bluetooth OFF",
                                Toast.LENGTH_SHORT).show();
                        checkBT();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Turning Bluetooth off...
                        Toast.makeText(getApplicationContext(), "Turning Bluetooth OFF...",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        //Bluetooth on;
                        Toast.makeText(getApplicationContext(), "Bluetooth ON",
                                Toast.LENGTH_SHORT).show();
                        checkBT();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Turning Bluetooth on...
                        Toast.makeText(getApplicationContext(), "Turning Bluetooth ON...",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };

    public void checkBT() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Missing bluetooth adapter.", Toast.LENGTH_SHORT).show();
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                btStatus = 1;
                //Log.e("checkBT Method", "Was started");
            } else {
                btStatus = 0;
            }
            invalidateOptionsMenu();
        }
    }

    //Delete all functionality
    private boolean deleteAll() {
        boolean isDeleted = false;
        Uri inboxUri = Uri.parse("content://sms/inbox");
        Cursor c = getApplicationContext().getContentResolver().query(inboxUri , null, null, null, null);
        while (c.moveToNext()) {
            try {
                // Delete the SMS
                String pid = c.getString(0); // Get id;
                String uri = "content://sms/" + pid;
                getApplicationContext().getContentResolver().delete(Uri.parse(uri),
                        null, null);
                isDeleted = true;
            } catch (Exception e) {
                isDeleted = false;
            }
        }
        c.close();
        return isDeleted;
    }

    public void onGetSp() {
        myData = getSharedPreferences("com.prisms.smsapp1", MODE_PRIVATE);
        //Log.e("onGetSp: ", "Initiated");
        String spHeaderI = myData.getString("Header", "");
        String spFooterI = myData.getString("Footer", "");
        if ((spHeaderI != null) || (spFooterI != null)) {
            if (!(spHeaderI.equals("")) || !(spFooterI.equals(""))) {
                spHeader = myData.getString("Header", "");
                if (!(spHeaderI.equals(""))) {
                    spHeader = myData.getString("Header", "");
                } else {
                    Toast.makeText(getApplicationContext(), "Header is empty.", Toast.LENGTH_SHORT).show();
                    spHeader = "________________________________\n" + "M-PESA PAYMENTS DETAILS\n" +
                            "________________________________\n";
                }
                if (!(spFooterI.equals(""))) {
                    spFooter = myData.getString("Footer", "");
                } else {
                    Toast.makeText(getApplicationContext(), "Footer is empty.", Toast.LENGTH_SHORT).show();
                    spFooter = "================================\n" + "Thank you!!!\n";
                }
            } else {
                Toast.makeText(getApplicationContext(), "No saved receipt format.", Toast.LENGTH_SHORT).show();
                spHeader = "________________________________\n" + "M-PESA PAYMENTS DETAILS\n" +
                        "________________________________\n";
                spFooter = "================================\n" + "Thank you!!!\n";
                //Log.e("onGetSp: ", "Toast should happen");
            }
        } else {
            spHeader = "________________________________\n" + "M-PESA PAYMENTS DETAILS\n" +
                    "________________________________\n";
            spFooter = "================================\n" + "Thank you!!!\n";
        }
    }

    /* Displaying messages.*/

    @SuppressLint({"HardwareIds", "ObsoleteSdkInt"})
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter1);

     /*   View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions); */

        //Get Verifier
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.e("DEVICE ID", "onCreate: " + deviceId );
        setContentView(R.layout.activity_main);

        // Get the application context
        mContext = getApplicationContext();
        mActivity = MainActivity.this;

        messages = findViewById(R.id.messages);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                smsMessagesList);
        messages.setAdapter(arrayAdapter);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        checkBT();

        //Log.e("BT Status", "btStatus: " + btStatus);
        //Log.e("Icon Status", "btConnStatus: " + btConnStatus);

        // text label and input box
        myLabel = findViewById(R.id.label);
        myLay2 = findViewById(R.id.lay2);
        myTextBox = findViewById(R.id.entry);

        /* All Permissions*/
        //Determine if the current Android version is >=23
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            //initViews();
            Toast.makeText(this, "Android version is not supported.",
                    Toast.LENGTH_SHORT).show();
        }
        /*print functionality*/
        messages.setOnItemClickListener((adapterView, view, i, l) -> {
            if (powerLaunch == 1) {
                new AlertDialog.Builder(MainActivity.this)
                        //Use ResourceCompat.getDrawable
                        .setIcon(ResourcesCompat.getDrawable(getResources(),
                                R.drawable.ic_baseline_local_printshop_24, null))
                        .setTitle("PRINT")
                        .setMessage("Print this message?")
                        .setPositiveButton("PRINT", (dialogInterface, i12) -> {
                            /* Converting listView element to string. */
                            msg = spHeader + "\n" + "COPY\n" + ((TextView) view).getText() + "\n" + spFooter + "\n" +
                                    "\n" + "\n" + "\n" + "\n" + "\n";
                            //Log.e("My clicked sms", "Content: \n" + msg);
                            //Debug
                            /*
                            System.out.println("Hello my sms: " + text);
                            System.out.println("Printer Status: " + powerLaunch);
                            System.out.println("Selected sms: " + i);
                             */
                            sendData();
                            //onClickBmp2();
                        })
                        .setNegativeButton("QUIT", null)
                        .show();
            } else {
                //view.getContext() sub of getApplicationContext()
                Toast.makeText(getApplicationContext(),
                        "Please, Activate print to continue.", Toast.LENGTH_SHORT).show();
            }
        });

        /*delete functionality*/
        messages.setOnItemLongClickListener((adapterView, view, i, l) -> {
            final int arrToDelete = i;
            new AlertDialog.Builder(MainActivity.this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Delete")
                    .setMessage("This message will be deleted")
                    .setPositiveButton("DELETE", (dialogInterface, i1) -> {
                        String msgToDelete = (String) ((TextView) view).getText();
                        String newStrId = StringUtils.substringBetween(msgToDelete, "REF: ", "From: ");
//                                Log.e("onLongClick i ", "This is idStr: " + newStrId);
                        int newId = Integer.parseInt(newStrId.trim());
//                                Log.e("onLongClick i ", "This is idInt: " + newId);
                        ContentResolver contentResolver = getContentResolver();
                        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"),
                                null, null, null, null);
                        assert smsInboxCursor != null;
                        int indexId = smsInboxCursor.getColumnIndex("_id");
                        if (indexId < 0 || !smsInboxCursor.moveToFirst()) return;
                        try {
                            do {
                                int id = smsInboxCursor.getInt(indexId);
                                if (id == newId) {
                                    contentResolver.delete(Uri.parse("content://sms/" + id), null, null);
                                    smsMessagesList.remove(arrToDelete);
                                    arrayAdapter.notifyDataSetChanged();
                                }
                            } while (smsInboxCursor.moveToNext());
                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(),"Deleting message failed.", Toast.LENGTH_SHORT).show();
                        }
                        smsInboxCursor.close();
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();
            return true;
        });

        /*Feed paper functionality*/
        prnAct = findViewById(R.id.printOn);
        prnDea = findViewById(R.id.printOff);
        //b1 = findViewById(R.id.prnOpen);
        prnAct.setOnClickListener(view -> onPrnOpen());

        prnDea.setOnClickListener(view -> onPrnOpen());

        onGetSp();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub

        disableFunctionLaunch(true);
        /*getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);*/

        super.onResume();
        /*IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        receiver = new BatteryReceiver();
        registerReceiver(receiver, filter);*/
    }

    //Check for default
    /**
     * method checks to see if app is currently set as default launcher
     * @return boolean true means currently set as default, otherwise false
     */
    private boolean isMyAppMsgDefault() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        List<IntentFilter> filters = new ArrayList<IntentFilter>();
        filters.add(filter);

        final String myPackageName = getPackageName();
        List<ComponentName> activities = new ArrayList<>();
        final PackageManager packageManager = getPackageManager();

        packageManager.getPreferredActivities(filters, activities, null);

        for (ComponentName activity : activities) {
            if (myPackageName.equals(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * method starts an intent that will bring up a prompt for the user
     * to select their default launcher. It comes up each time method is called.
     */
    private void msgAppChooser() {
        RoleManager roleManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleManager = getApplicationContext().getSystemService(RoleManager.class);
            if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                    Toast.makeText(getApplicationContext(), "PrismApp set as default.", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
                    startActivity(i);
//                     Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                     intent.setData(Uri.parse("package:" + getPackageName()));
//                     startActivity(intent);
                } else {
                    Intent roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS);
                    startActivityForResult(roleRequestIntent, 2);
                }
            }
        } else {
            //If android version is prior to Android 10
            //selectDefaultSmsPackage();
            //String myPackageName = getPackageName();
            Intent setSmsAppIntent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
            //setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
            startActivity(setSmsAppIntent);
        }
//             Log.e("msgAppChooser: ", "MsgAppChooser() initiated, isNotDefault," +
//                     " Package name: " + myPackageName);
        /*String myPackageName = getPackageName();
        Intent setSmsAppIntent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        setSmsAppIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, myPackageName);
        startActivity(setSmsAppIntent);*/

    }

    //Longer Method
    private static  final int DEF_SMS_REQ = 0;
    private AppInfo selectedApp;
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void selectDefaultSmsPackage() {
        @SuppressLint("QueryPermissionsNeeded") final List<ResolveInfo> receivers = getPackageManager().queryBroadcastReceivers(new
                Intent(Telephony.Sms.Intents.SMS_DELIVER_ACTION), 0);
        final ArrayList<AppInfo> apps = new ArrayList<>();
        for (ResolveInfo info : receivers) {
            final String packageName = info.activityInfo.packageName;
            final String appName = getPackageManager().getApplicationLabel(info.activityInfo.applicationInfo).toString();
            final Drawable icon = getPackageManager().getApplicationIcon(info.activityInfo.applicationInfo);
            apps.add(new AppInfo(packageName, appName, icon));
        }
        apps.sort(new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo app1, AppInfo app2) {
                return app1.appName.compareTo(app2.appName);
            }
        });
        new AppsDialog(this, apps).show();
    }

    public void onAppSelected(AppInfo selectedApp) {
        this.selectedApp = selectedApp;
        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, selectedApp.packageName);
        startActivityForResult(intent, DEF_SMS_REQ);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DEF_SMS_REQ) {
            String currentDefault = Telephony.Sms.getDefaultSmsPackage(this);
            boolean isDefault = selectedApp.packageName.equals(currentDefault);

            String msg = selectedApp.appName + (isDefault ?
                    " successfully set as default" :
                    " not set as default");

            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public static class AppInfo {
        String appName;
        String packageName;
        Drawable icon;

        public AppInfo(String packageName, String appName, Drawable icon) {
            this.packageName = packageName;
            this.appName = appName;
            this.icon = icon;
        }

        @NonNull
        @Override
        public String toString() {
            return appName;
        }
    }

    public void updateInbox(final String smsMessage) {
        arrayAdapter.insert(smsMessage, 0);
        arrayAdapter.notifyDataSetChanged();
    }

    /*Bringing up our runtime permission requests.*/
    protected void checkPermission(){
        if(ContextCompat.checkSelfPermission(mActivity,Manifest.permission.READ_SMS)
                + ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                + ContextCompat.checkSelfPermission(mActivity, Manifest.permission.RECEIVE_SMS) !=
                PackageManager.PERMISSION_GRANTED){

            // Do something, when permissions not granted
            if(ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity,Manifest.permission.READ_SMS)
//                    || ActivityCompat.shouldShowRequestPermissionRationale(
//                    mActivity,Manifest.permission.READ_CONTACTS)
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(
                    mActivity,Manifest.permission.RECEIVE_SMS)){
                // If we should give explanation of requested permissions

                // Show an alert dialog here with request explanation
                AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                builder.setMessage("PrismApp requires SMS and Write External" +
                        " Storage permissions to do the task.");
                builder.setTitle("Please grant these permissions:");
                builder.setPositiveButton("OK", (dialogInterface, i) -> ActivityCompat.requestPermissions(
                        mActivity,
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_SMS,
                                Manifest.permission.RECEIVE_SMS
                        },
                        MY_PERMISSIONS_REQUEST_CODE
                ));
                builder.setNeutralButton("Cancel",null);
                AlertDialog dialog = builder.create();
                dialog.show();
            }else{
                /* Directly request for required permissions, without explanation */
                ActivityCompat.requestPermissions(
                        mActivity,
                        new String[]{
                                Manifest.permission.READ_SMS,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        MY_PERMISSIONS_REQUEST_CODE
                );
            }
        }else {
            /* Do something, when permissions are already granted */
            Toast.makeText(mContext,"Permissions already granted",Toast.LENGTH_SHORT).show();
            refreshSmsInbox();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
//        System.out.println("Hello my new request code is: "+ requestCode );
//        System.out.println("Hello my new grantResult is: "+ grantResults.length);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_CODE) {
            // When request is cancelled, the results array are empty
            if ((grantResults.length > 0) && (grantResults[0] +
                    grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                /* Permissions are granted */
                Toast.makeText(mContext, "Permissions granted.", Toast.LENGTH_SHORT).show();
                refreshSmsInbox();
            } else {
                /* Permissions are denied */
                Toast.makeText(mContext, "Permissions denied.", Toast.LENGTH_SHORT).show();
                checkPermission();
            }
        }
    }

    @SuppressLint("Recycle")
    public void refreshSmsInbox() {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"),
                null, null, null, null);
        assert smsInboxCursor != null;
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        int indexDate = smsInboxCursor.getColumnIndex("date");
        int indexId = smsInboxCursor.getColumnIndex("_id");
//        System.out.println("Hapa Debug: " + smsInboxCursor.getColumnIndex("_id") );
//        System.out.println("Hapa Debug2: " + smsInboxCursor.getString(indexId) );
        /*Save index and top position*/
        int index = messages.getFirstVisiblePosition();
        View v = messages.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - messages.getPaddingTop());
        /*end*/
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;
        arrayAdapter.clear();
        do {
            long timeMillis = smsInboxCursor.getLong(indexDate);
            Date date = new Date(timeMillis);
            String str = "REF: " + smsInboxCursor.getString(indexId) + "\n"
                    + "From: " + smsInboxCursor.getString(indexAddress) + "\n"
                    + smsInboxCursor.getString(indexBody) + "\n"
                    + "Date: " + date + "\n";
            arrayAdapter.add(str);
//            System.out.println(str);
//            System.out.println("My bloody Id: " + smsInboxCursor.getString(indexId));
            //System.out.println("My count: " + arrayAdapter.getItem(1));
        } while (smsInboxCursor.moveToNext());
        //messages.setSelection(arrayAdapter.getCount() - 1);
        /*Cont.*/
        messages.setSelectionFromTop(index, top);

//        System.out.println("New data: " + messages.getItemAtPosition(indexId));/*REF: 114*/
//        System.out.println("New dataTwo: " + arrayAdapter.getItem(0));
//        System.out.println("New dataThree: " + arrayAdapter.getItemId(118));
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        disableFunctionLaunch(false);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onPause();
        /*unregisterReceiver(receiver);*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disableFunctionLaunch(false);
        unregisterReceiver(mBroadcastReceiver);
    }

    // Back button customization
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(true);

            return true;
        }
        return false;
    }

    // disable the power key when the device is boot from alarm but not ipo boot
    private static final String DISABLE_FUNCTION_LAUNCH_ACTION =
            "android.intent.action.DISABLE_FUNCTION_LAUNCH";
    private void disableFunctionLaunch(boolean state) {
        Intent disablePowerKeyIntent = new Intent(DISABLE_FUNCTION_LAUNCH_ACTION);
        if (state) {
            disablePowerKeyIntent.putExtra("state", true);
        } else {
            disablePowerKeyIntent.putExtra("state", false);
        }
        sendBroadcast(disablePowerKeyIntent);
    }

    /**
     * New auto print logic
     */
    public void autoP(long timestamp) {
        refreshSmsInbox();
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"),
                null, /*String.valueOf(timestamp)*/null, null, null);
        assert smsInboxCursor != null;
        smsInboxCursor.moveToFirst();
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        int indexDate = smsInboxCursor.getColumnIndex("date");
        int indexId = smsInboxCursor.getColumnIndex("_id");
        do {
            long timeMillis = smsInboxCursor.getLong(indexDate);
            Date date = new Date(timeMillis);
            if (timeMillis == timestamp) {
                newText = "REF: " + smsInboxCursor.getString(indexId) + "\n"
                        + "From: " + smsInboxCursor.getString(indexAddress) + "\n"
                        + smsInboxCursor.getString(indexBody) + "\n"
                        + "Date: " + date + "\n";
            }
        } while (smsInboxCursor.moveToNext());
        smsInboxCursor.close();
//        Log.e("autoP: ", "AutoP "+ newText);
        if (powerLaunch == 1 && !newText.isEmpty()) {
            msg = spHeader + "\n" + "ORIGINAL\n" + newText + "\n" + spFooter + "\n" + "\n" + "\n" + "\n" + "\n" + "\n";
            sendData();
        } else {
            Toast.makeText(getApplicationContext(), "Activate print to engage auto-printing.",
                    Toast.LENGTH_SHORT).show();
        }
    }

//    public void autoPrint() {
////        System.out.println("New dataFour: " + arrayAdapter.getItem(0)); /*Working well*/
//        refreshSmsInbox();
//        String newText = arrayAdapter.getItem(0);
//        refreshSmsInbox();
//        if (powerLaunch == 1) {
//            refreshSmsInbox();
//            refreshSmsInbox();
//            refreshSmsInbox();
//            msg = spHeader + "\n" + newText + "\n" + spFooter + "\n" + "\n" + "\n" + "\n";
//            sendData();
//        } else {
//            Toast.makeText(getApplicationContext(), "Activate print to engage auto-printing.",
//                    Toast.LENGTH_SHORT).show();
//            refreshSmsInbox();
//        }
//
//    }

    //BT Configs
    // this will find a bluetooth printer device
    void findBT() {

        try {

            if(mBluetoothAdapter == null) {
                myLabel.setText(R.string.btM1);
            }

            if(!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetooth.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Intent chooserIntent = Intent.createChooser(enableBluetooth, "Open BT...");
                startActivity(chooserIntent);

                //startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),1);

                Toast.makeText(getApplicationContext(), "Turn Bluetooth ON.",
                        Toast.LENGTH_LONG).show();
            } else {
                //Log.e("Debug FindBT", "Method was initiated!!");
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

                //Privileged
                myGroup2 = findViewById(R.id.radioG);
                if (pairedDevices.size() > 0) {
                    myLabel.setText("Paired devices found.");
                    //RadioGroup
                    myGroup2.removeAllViews();
                    for (BluetoothDevice device : pairedDevices) {
                        String deviceName = device.getName();
                        RadioButton rb = new RadioButton(this);
                        rb.setText(deviceName);
                        myGroup2.addView(rb);
                    }

                    myGroup2.setOnCheckedChangeListener((group, checkedId) -> {
                        // checkedId is the RadioButton selected
                        radioButton = findViewById(checkedId);
                        Toast.makeText(getApplicationContext(), "My Device: "
                                + radioButton.getText(), Toast.LENGTH_SHORT).show();
                        for (BluetoothDevice device : pairedDevices) {
                            if (device.getName().contentEquals(radioButton.getText())) {
                                mmDevice = device;
                            }
                        }
                    });
                } else {
                    myLabel.setText(R.string.btM2);
                }

//                myLabel.setText(R.string.btM2);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // tries to open a connection to the bluetooth printer device
    void openBT() {
        try {

            // Standard SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            //Log.e("Check Connection", "Connection Status: " + mmSocket.isConnected());
            if (!mmSocket.isConnected()) {
                Toast.makeText(getApplicationContext(), "Connection Error, Press open again", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Connected Device is " + mmDevice.getName(), Toast.LENGTH_LONG).show();
                mmOutputStream = mmSocket.getOutputStream();
                //Log.e("mmOutputStream", String.valueOf(mmOutputStream));
                mmInputStream = mmSocket.getInputStream();

                beginListenForData();

                myLabel.setText(R.string.btM3);
                btConn();
                myGroup2.removeAllViews();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * after opening a connection to bluetooth printer device,
     * we have to listen and check if a data were sent to be printed.
     */
    void beginListenForData() {
        try {
            final Handler handler = new Handler();

            // this is the ASCII code for a newline character
            final byte delimiter = 10;

            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            workerThread = new Thread(() -> {

                while (!Thread.currentThread().isInterrupted() && !stopWorker) {

                    try {

                        int bytesAvailable = mmInputStream.available();

                        if (bytesAvailable > 0) {

                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {

                                byte b = packetBytes[i];
                                if (b == delimiter) {

                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(
                                            readBuffer, 0,
                                            encodedBytes, 0,
                                            encodedBytes.length
                                    );

                                    // specify US-ASCII encoding
                                    final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                    readBufferPosition = 0;

                                    // tell the user data were sent to bluetooth printer device
                                    handler.post(() -> myLabel.setText(data));

                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }

                    } catch (IOException ex) {
                        stopWorker = true;
                    }

                }
            });

            workerThread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // this will send text data to be printed by the bluetooth printer
    void sendData() {
        try {

            // the text typed by the user
            //msg += "\n";
            //Toast.makeText(getApplicationContext(), "My data" + msg, Toast.LENGTH_LONG).show();
            //Log.e("My text", "sendData: " + msg);
            //Log.e("My outStream", "out: " + Arrays.toString(msg.getBytes()));
            mmOutputStream.write(msg.getBytes());
            Thread.sleep(100);    // added this line

            // tell the user data were sent
            myLabel.setText(R.string.datasnt);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // close the connection to bluetooth printer.
    void closeBT() {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            myLabel.setText(R.string.btM4);
            btConnStatus = 0;
            invalidateOptionsMenu();
            Toast.makeText(getApplicationContext(), "Connection Closed", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
package com.edit.prismappbt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class BTConnActivity extends AppCompatActivity {
    MainActivity inst = MainActivity.instance();
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
    String msg;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        IntentFilter filter1 = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver, filter1);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        inst.checkBT();
        // text label and input box
        myLabel = findViewById(R.id.labelT);
        myTextBox = findViewById(R.id.entryT);
        //inst.checkBT();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    public void onArrBack4(View v) {
        Intent printIntent = new Intent(BTConnActivity.this, MainActivity.class);
        printIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(printIntent);
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
                        inst.checkBT();
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
                        inst.checkBT();
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
                myGroup2 = findViewById(R.id.radioGT);
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
                inst.btConn();
                myGroup2.removeAllViews();
            }

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
            inst.btConnStatus = 0;
            invalidateOptionsMenu();
            Toast.makeText(getApplicationContext(), "Connection Closed", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
}

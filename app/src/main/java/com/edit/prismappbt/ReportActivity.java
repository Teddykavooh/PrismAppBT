package com.edit.prismappbt;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    File rep_folder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/PrismApp/Reports");
    CSVWriter writer;
    String ID;
    String body;
    String date;
    String address;
    Toast myToast;
    public Float trn_amt;
    public Float trn_blc;
    private final String text1 = "QAI1S35S7Z Confirmed.on 18/1/22 at 3:57 PMKsh40.00 received from 254721863742 MERCY MUGOIRI WAITHAKA. New Account balance is Ksh440.00. Transaction cost, Ksh0.00.";
    private final String text2 = "QAI9S2XEPH Confirmed.on 18/1/22 at 3:54 PMKsh200.00 received from 254797847747 james kamau. New Account balance is Ksh400.00. Transaction cost, Ksh0.00.";
    private final String text3 = "RAQ788BUHD Confirmed on 26/1/23 at 12:47 PM Ksh45,200.00 received from 488519-NCBA Bank M- pesa:254780429439. New Account balance is Ksh45,087.00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);
        myToast = Toast.makeText(this, "Populating...", Toast.LENGTH_SHORT);
        TextView folderLink = findViewById(R.id.folderLink);
        folderLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/PrismApp/Reports";
                Uri uri = Uri.parse(path);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                Log.e("onClick: ", "Path: " + path);
                intent.setDataAndType(uri, "*/*");
                startActivity(intent);
            }
        });
    }

    public void ret_Home(View v) {
        Intent printIntent = new Intent(ReportActivity.this, MainActivity.class);
        printIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(printIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Get the messages
     *
     * INFO/COLUMNS: [_id, thread_id, address, person, date, protocol, read, status, type,
     *          reply_path_present, subject, body, service_center, locked]
     */
    public void get_sms(View v) {
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"),
                null, null, null, null);
        if (smsInboxCursor != null) {
            int indexBody = smsInboxCursor.getColumnIndex("body");
            int indexAddress = smsInboxCursor.getColumnIndex("address");
            int indexDate = smsInboxCursor.getColumnIndex("date");
            int indexId = smsInboxCursor.getColumnIndex("_id");

            //        System.out.println("Hapa Debug: " + smsInboxCursor.getColumnIndex("_id") );
            //        System.out.println("Hapa Debug2: " + smsInboxCursor.getString(indexId) );

            /*Save index and top position
            int index = messages.getFirstVisiblePosition();
            View v = messages.getChildAt(0);
            int top = (v == null) ? 0 : (v.getTop() - messages.getPaddingTop());
            /*end*/

            if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;
            //arrayAdapter.clear();

            //Initialize CSV
            dir_func();

            while (smsInboxCursor.moveToNext()) {
                long timeMillis = smsInboxCursor.getLong(indexDate);
                Date my_date = new Date(timeMillis);
                if (smsInboxCursor.getString(indexAddress).equals("MPESA")) {
                    //Populate the possible view and CSV
                    ID = smsInboxCursor.getString(indexId);
                    body = smsInboxCursor.getString(indexBody);
                    address = smsInboxCursor.getString(indexAddress);
                    date = my_date.toString();
                    pop_CSV();
                }
            }
            smsInboxCursor.close();

            //Close CSV when done
            close_CSV();
        } else {
            Toast.makeText(this, "Inbox Cursor Error!", Toast.LENGTH_SHORT).show();
        }
    }

    /*public void tester(View v) throws IOException {
        dir_func();
        init_CSV();
        pop_CSV();
        close_CSV();
    }*/

    /** Make CSV dir */
    public void dir_func() {
        try {
            // Check if folder exists if not create
            //Log.e("dir_func ", "folder: " + rep_folder);
            if (!rep_folder.exists() && !rep_folder.isDirectory()) {
                Toast.makeText(this, "Creating folders...", Toast.LENGTH_SHORT).show();
                if(rep_folder.mkdirs()) {
                    Toast.makeText(this, "Folder Created", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error creating folder", Toast.LENGTH_SHORT).show();
                }
            }
            init_CSV();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "ERROR occurred", Toast.LENGTH_SHORT).show();
        }
    }

    /** Save CSV */
    public void init_CSV() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HH_mm_ss", Locale.ENGLISH);
            String currentTime = sdf.format(new Date());
            String my_file_name = currentTime + ".csv";
            //String my_file_name = "rep.csv";
            String my_csv = rep_folder + "/" + my_file_name;
            Log.d("init_CSV ", "Target file: " + my_csv);
            // create FileWriter object with file as parameter
            FileWriter output_file = new FileWriter(my_csv);

            // create CSVWriter object filewriter object as parameter
            writer = new CSVWriter(output_file);

            // adding header to csv
            String[] header = { "ID", "Address", "Body", "Date" };
            writer.writeNext(header);

            Toast.makeText(this, "CSV init successful", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "CSV init unsuccessful", Toast.LENGTH_SHORT).show();
        }
    }

    public void pop_CSV() {
        try {
            // add data to csv
            myToast.show();
            String[] data = { ID, address, body, date, };
            writer.writeNext(data);
            /*String[] data2 = { "Suraj", "10", "630" };
            writer.writeNext(data2);*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close_CSV() {
        try {
            myToast.cancel();
            // closing writer connection
            writer.close();
            Toast.makeText(this, "Operation Done", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Read CSV for more func */
    public void readCSV(View v) {
        CSVReader reader;
        try {
            reader = new CSVReaderBuilder(new FileReader(rep_folder + "/rep.csv"))
                    .withSkipLines(1).build();
            String[] nextLine;
            System.out.print("My shit is up:");
            System.out.print("\n");
            while ((nextLine = reader.readNext())!= null) {
                /*for (String token : nextLine) {
                   System.out.print(token[1]);
                }*/
                System.out.println(nextLine[0] + nextLine[1] + "etc...");
                System.out.print("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Data striping */
    public void stripperTester(View v) {
        String trn_code = StringUtils.substringBefore(text3, " Confirmed");
        String trn_date = getDate();
        trn_amt = getAmt();
        String trn_name = StringUtils.substringBetween(text3, "received from ", ". New ");
        trn_blc = getBlc();
        Float trn_cost = getTrnCst();
        Log.e("stripperTester ", " \n" + "code: " + trn_code + "\n" + "date: " + trn_date +
                "\n" + "amt: " + trn_amt + "\n" + "name: " + trn_name + "\n" + "balance: " + trn_blc
                + "\n" +"cost: " + trn_cost + "\n");
    }

    public String getDate() {
        String v1 = StringUtils.substringBetween(text3, "Confirmed on ", " at");
        String v2 = StringUtils.substringBetween(text3, "Confirmed.on ", " at");
        String trn_time = " " + StringUtils.substringBetween(text3, "at ", "Ksh").replaceAll("\\s", "");
        if (v1 != null) {
            return v1 + trn_time;
        } else {
            return v2 + trn_time;
        }
    }

    public Float getTrnCst() {
        String v1 = StringUtils.substringBetween(text3, "Transaction cost, Ksh", ".");
        if (v1 == null) {
            return trn_amt - trn_blc;
        } else {
            return Float.parseFloat(v1);
        }
    }

    public Float getBlc() {
        String v1 = StringUtils.substringBetween(text3, "balance is Ksh", ". T");
        String v2 = StringUtils.substringAfter(text3, "balance is Ksh");
        float my_int;
        if (v1 == null) {
            //get integer
            try {
                my_int = Integer.parseInt(v2);
            } catch (Exception e) {
                my_int = Float.parseFloat(v2.replaceAll(",", ""));
                e.printStackTrace();
            }
            //Log.e("getBlc ", String.valueOf(my_int));
            return my_int;
        } else {
            //get integer
            try {
                my_int = Integer.parseInt(v1);
            } catch (Exception e) {
                my_int = Float.parseFloat(v1.replaceAll(",", ""));
                e.printStackTrace();
            }
            //Log.e("getBlc ", String.valueOf(my_int));
            return my_int;
        }
    }

    public Float getAmt() {
        String str_amt = StringUtils.substringBetween(text3, "Ksh", " received from");
        float flt_amt;
        //Get int
        try {
            flt_amt = Float.parseFloat(str_amt);
            return flt_amt;
        } catch (Exception e) {
            flt_amt = Float.parseFloat(str_amt.replaceAll(",", ""));
            return flt_amt;
            //e.printStackTrace();
        }
    }
}

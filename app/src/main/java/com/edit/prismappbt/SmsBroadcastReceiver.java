package com.edit.prismappbt;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.util.Date;
import java.util.Objects;

//Responsible for message intercepting.
public class SmsBroadcastReceiver extends BroadcastReceiver {
    public static final String SMS_BUNDLE = "pdus";


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();

        if (intentExtras != null) {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            StringBuilder smsMessageStr = new StringBuilder();
            for (Object sm : Objects.requireNonNull(sms)) {
                String format = intentExtras.getString("format");
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) sm, format);

                String smsBody = smsMessage.getMessageBody();
                String address = smsMessage.getOriginatingAddress();
                int smsIndex = smsMessage.getIndexOnIcc();
                long smsTime = smsMessage.getTimestampMillis();
                Date date = new Date(smsTime);

                smsMessageStr.append("REF: ").append(smsIndex).append("\n");
                smsMessageStr.append("From: ").append(address).append("\n");
                smsMessageStr.append(smsBody).append("\n");
                smsMessageStr.append("Date: ").append(date).append("\n");
                Log.e("Auto Print SMS Broad...", "New sms ref: " + smsBody);

                MainActivity inst = MainActivity.instance();
                inst.updateInbox(smsMessageStr.toString());
                inst.refreshSmsInbox();
                inst.refreshSmsInbox();
            }
        }
        /*Auto Print Comes in*/
        MainActivity inst = MainActivity.instance();
        inst.refreshSmsInbox();
        inst.autoPrint();
    }
}
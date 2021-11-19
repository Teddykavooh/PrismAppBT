package com.edit.prismappbt;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
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
    private static final String ACTION_SMS_NEW = "android.provider.Telephony.SMS_DELIVER";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void onReceive(Context context, Intent intent) {
        Bundle intentExtras = intent.getExtras();

        if (intentExtras != null) {
            Object[] sms = (Object[]) intentExtras.get(SMS_BUNDLE);
            StringBuilder smsMessageStr = new StringBuilder();
            ContentValues values = new ContentValues();
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
//                Log.e("Auto Print SMS Broad...", "New sms ref: " + smsBody);

                //Save to inbox if message is delivered
                final String action = intent.getAction();
                if (ACTION_SMS_NEW.equals(action)) {
                    values.put("address", address); // phone number to send
                    values.put("date", smsTime);
                    values.put("read", "1"); // if you want to mark it as unread set to 0
                    values.put("type", "1"); // 2 means sent message
                    values.put("body", smsBody);
                    Uri uri = Uri.parse("content://sms/");
                    context.getContentResolver().insert(uri, values);
                }

                MainActivity inst = MainActivity.instance();
                inst.updateInbox(smsMessageStr.toString());
                inst.refreshSmsInbox();
                inst.refreshSmsInbox();
            }

            /* Notification Tone */
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*Auto Print Comes in*/
        MainActivity inst = MainActivity.instance();
        inst.refreshSmsInbox();
        inst.autoPrint();
    }
}
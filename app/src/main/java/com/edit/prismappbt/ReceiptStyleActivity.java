package com.edit.prismappbt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class ReceiptStyleActivity extends AppCompatActivity {
    EditText header;
    EditText footer;
    String myHeader;
    String myFooter;
    SharedPreferences myData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);
        header = findViewById(R.id.header);
        footer = findViewById(R.id.footer);
    }

    public void onArrBack2(View v) {
        Intent printIntent = new Intent(ReceiptStyleActivity.this, MainActivity.class);
//        FLAG_ACTIVITY_REORDER_TO_FRONT
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

    public void headFootFunc(View v) {
        myData = getSharedPreferences("com.prisms.smsapp1", MODE_PRIVATE);
        myHeader = header.getText().toString();
        myFooter = footer.getText().toString();
        SharedPreferences.Editor editor = myData.edit();
        editor.putString("Header", myHeader);
        editor.putString("Footer", myFooter);
        editor.apply();
        Intent printIntent = new Intent(ReceiptStyleActivity.this, MainActivity.class);
        startActivity(printIntent);

    }
}

package com.edit.prismappbt;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashScreenActivity extends AppCompatActivity {
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
//        finish();
        handler=new Handler();
        handler.postDelayed(() -> {
            Intent intent=new Intent(SplashScreenActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
        },2000);
    }
}
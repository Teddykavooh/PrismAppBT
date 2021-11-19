package com.edit.prismappbt;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_about);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*Intent aboutIntent = new Intent(AboutActivity.this, MainActivity.class);
        startActivity(aboutIntent);*/
        MainActivity.instance().onResume();
        return super.onKeyDown(keyCode, event);
    }

    public void onArrBack2(View v) {
        Intent aboutIntent = new Intent(AboutActivity.this, MainActivity.class);
        startActivity(aboutIntent);
    }
}

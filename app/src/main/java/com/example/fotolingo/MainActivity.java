package com.example.fotolingo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button translatePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        translatePhoto = findViewById(R.id.translatePhoto);
    }

    public void translatePhotoClickHandler(View v) {
        Log.d("HomePage", "Translate Photo button has been clicked!");
        Intent intent = new Intent(MainActivity.this, CameraPage.class);
        startActivity(intent);
    }
}

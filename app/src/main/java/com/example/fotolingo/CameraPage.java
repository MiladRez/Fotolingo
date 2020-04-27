package com.example.fotolingo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.OutputStream;

public class CameraPage extends AppCompatActivity {

    Button launchCamera;
    ImageView capturedPhoto;
    int REQUEST_IMAGE_CAPTURE = 1;
    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_page);

        launchCamera = findViewById(R.id.launchCameraButton);
        capturedPhoto = findViewById(R.id.capturedPhoto);
    }

    public void launchCameraClickHandler(View v) {
        Log.d("CameraPage", "Launch Camera button has been clicked!");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            //Checks for storage permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.d("CameraPage", "You do not have permissions");
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_CAMERA);
                }
            }
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            capturedPhoto.setImageBitmap(imageBitmap);

            MediaStore.Images.Media.insertImage(this.getContentResolver(), imageBitmap, "CapturedImage", "image_desc");
        }
    }
}

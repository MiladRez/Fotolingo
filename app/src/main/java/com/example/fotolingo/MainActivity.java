package com.example.fotolingo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Button translatePhoto;
    int camera_permission = 1;
    int storage_permission = 2;
    String selectedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Spinner spinner = findViewById(R.id.languageSelector);
        spinner.setOnItemSelectedListener(this);

        // Spinner drop down elements
        List<String> languages = new ArrayList<String>();
        languages.add("French");
        languages.add("German");
        languages.add("Chinese");
        languages.add("Japanese");
        languages.add("Italian");
        languages.add("Korean");
        languages.add("Persian");

        // creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, languages);

        //drop down style
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (getFromPref(this, "ALLOWED", "camera_pref")) {
                showAlert(camera_permission);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (getFromPref(this, "ALLOWED", "storage_pref")) {
                showAlert(storage_permission);
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
            }
        }

        translatePhoto = findViewById(R.id.translatePhoto);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // on selecting a spinner item
        selectedLanguage = parent.getItemAtPosition(position).toString();

        // showing selected spinner item
        Toast.makeText(parent.getContext(), "Selected: " + selectedLanguage, Toast.LENGTH_SHORT).show();
    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // auto-generated from spinner
    }

    public void translatePhotoClickHandler(View v) {
        Log.d("HomePage", "Translate Photo button has been clicked!");
        Intent intent = new Intent(MainActivity.this, CameraPage.class);
        intent.putExtra("SelectedLanguage", selectedLanguage);
        startActivity(intent);
    }

    // save camera permission preferences
    public static void saveToPreferences(Context context, String key, Boolean allowed, String whichPref) {
        SharedPreferences myPrefs = context.getSharedPreferences(whichPref, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(key, allowed);
        prefsEditor.commit();
    }

    // get app permission preferences
    public static Boolean getFromPref(Context context, String key, String whichPref) {
        SharedPreferences myPrefs = context.getSharedPreferences(whichPref, Context.MODE_PRIVATE);
        return (myPrefs.getBoolean(key, false));
    }

    // show alert asking for camera permission
    private void showAlert(final int whichPermission) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Alert");

        if (whichPermission == 1) {
            alertDialog.setMessage("App needs to access the Camera.");
        } else {
            alertDialog.setMessage("App needs to access the Storage.");
        }

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "DONT ALLOW",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "ALLOW",
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        if (whichPermission == 1) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 100);
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                        }
                    }
                });

        alertDialog.show();
    }

    // Camera permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];

                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);

                        if (showRationale) {
                            showAlert(1);
                        } else if (!showRationale) {
                            saveToPreferences(this, "ALLOWED", true, "camera_pref");
                        }
                    }
                }
            }
            case 101: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];

                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);

                        if (showRationale) {
                            showAlert(2);
                        } else if (!showRationale) {
                            saveToPreferences(this, "ALLOWED", true, "storage_pref");
                        }
                    }
                }
            }
        }
    }
}

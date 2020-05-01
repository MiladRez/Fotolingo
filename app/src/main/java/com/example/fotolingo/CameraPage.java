package com.example.fotolingo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.request.ClarifaiRequest;
import clarifai2.api.request.model.PredictRequest;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.Model;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import clarifai2.exception.ClarifaiException;

public class CameraPage extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private File pictureFile;
    private ClarifaiClient client;
    private TextToSpeech t1;
    private SortedMap<Float, String> accurateResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_page);

        startCameraPreview();
        startTextToSpeech();

        ClarifaiBuilder builder = new ClarifaiBuilder("35df5fef563a41b183583e6c55ab314a");
        client = builder.buildSync();

        accurateResults = new TreeMap<Float, String>(Collections.reverseOrder());
    }

    private void startCameraPreview() {
        if (checkCameraHardware(this)) {
            // Create an instance of Camera
            mCamera = getCameraInstance();
            Log.d("CAMERA_PAGE", "Camera instance: " + mCamera);

            mCamera.setDisplayOrientation(90);

            // Create the Preview view and set it as the content of this activity
            mPreview = new CameraPreview(this, mCamera);
            FrameLayout preview = findViewById(R.id.camera_preview);
            preview.addView(mPreview);

            // Rotate the final saved image
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
            // check what the current rotation is in degrees
            int degrees = this.getWindowManager().getDefaultDisplay().getRotation();
            // rotate to landscape calculation
            int rotate = (info.orientation - degrees + 360) % 360;

            // Set the rotation parameter
            Camera.Parameters params = mCamera.getParameters();
            params.setRotation(rotate);
            mCamera.setParameters(params);

        } else {
            Log.d("CAMERA_PAGE", "Device does not have a camera");
        }
    }

    // Add a listener to the Capture button
    public void captureButtonClickHandler(View v) throws IOException {
        Log.d("CAMERA_PAGE", "Capture button has been clicked!");
        mCamera.takePicture(null,null, mPicture);
    }

    // Check to see if the device has a camera
    private boolean checkCameraHardware(Context context) {
        // returns true if the device has a camera
        // returns false if the device does NOT have a camera
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
    }

    // get an instance of the Camera object
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get the Camera instance
        } catch (Exception e) {
            // Camera is not available
            Log.d("CAMERA_PAGE", "Camera is not available!");
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

            pictureFile = getOutputMediaFile();
            Log.d("CAMERA_PAGE", "pictureFile: " + pictureFile);
            if (pictureFile == null) {
                Log.d("CAMERA_PAGE", "Error creating image file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bytes);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("CAMERA_PAGE", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("CAMERA_PAGE", "Error accessing file: " + e.getMessage());
            }

            runGeneralModel();
            runFoodModel();
            runApparelModel();
            runTravelModel();

            try {
                TimeUnit.SECONDS.sleep(5);
                printResults();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    // Create a file for saving an image
    private File getOutputMediaFile() {

        // location in device to save images
        File mediaStorageDir = new File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Fotolingo");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CAMERA_PAGE", "Failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    private void startTextToSpeech() {
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.CANADA);
                }
            }
        });
    }

    // if application is paused, the camera is released
    @Override
    protected void onPause() {
        releaseCamera();

        if(t1 != null) {
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

    // if application is resumed, the camera preview starts again
    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
        startTextToSpeech();
    }

    // releases camera resources once app is closed
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
    }

    // Release camera resource
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void runGeneralModel() {
        client.getDefaultModels().generalModel().predict()
                .withInputs(ClarifaiInput.forImage(pictureFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
//                                          Log.d("CAMERA_PAGE", "Added: " + outputs.get(0).data().get(i).name());
//                                          Log.d("CAMERA_PAGE", "Inside map: " + accurateResults.get(outputs.get(0).data().get(i).value()));
                                      }
                                      Log.d("CAMERA_PAGE", "General model ran!");
                                  }
                              }
                );
    }

    private void runFoodModel() {
        client.getDefaultModels().foodModel().predict()
                .withInputs(ClarifaiInput.forImage(pictureFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                      }
                                      Log.d("CAMERA_PAGE", "Food model ran!");
                                  }
                              }
                );
    }

    private void runApparelModel() {
        client.getDefaultModels().apparelModel().predict()
                .withInputs(ClarifaiInput.forImage(pictureFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                      }
                                      Log.d("CAMERA_PAGE", "Apparel model ran!");
                                  }
                              }
                );
    }

    private void runTravelModel() {
        client.getDefaultModels().travelModel().predict()
                .withInputs(ClarifaiInput.forImage(pictureFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                      }
                                      Log.d("CAMERA_PAGE", "Travel model ran!");
                                  }
                              }
                );
    }

    public void printResults() {
        Log.d("CAMERA_PAGE", "I RUN");
        int i = 0;
        Iterator iterator = accurateResults.entrySet().iterator();
        while(iterator.hasNext() && i < 5) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Log.d("CAMERA_PAGE", entry.getValue() + ": " + entry.getKey());
            t1.speak(entry.getValue().toString(), TextToSpeech.QUEUE_ADD, null);
            i++;
        }
    }

    public void backButtonClickHandler(View v) {
        Log.d("CAMERA_PAGE", "Back button has been clicked!");
        Intent intent = new Intent(CameraPage.this, MainActivity.class);
        startActivity(intent);
    }

}

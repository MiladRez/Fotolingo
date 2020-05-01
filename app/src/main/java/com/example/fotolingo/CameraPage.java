package com.example.fotolingo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_page);

        startCameraPreview();
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
            runVisionAnalysis();
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

    // if application is paused, the camera is released
    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    // if application is resumed, the camera preview starts again
    @Override
    protected void onResume() {
        super.onResume();
        startCameraPreview();
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

    private void runVisionAnalysis() {
        ClarifaiBuilder builder = new ClarifaiBuilder("35df5fef563a41b183583e6c55ab314a");
        ClarifaiClient client = builder.buildSync();

        client.getDefaultModels().generalModel().predict()
                .withInputs(ClarifaiInput.forImage(pictureFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      Log.d("CAMERA_PAGE", outputs.get(0).data().get(0).name());
                                  }
                              }
                );
    }

}

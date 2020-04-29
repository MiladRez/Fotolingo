package com.example.fotolingo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraPage extends AppCompatActivity {

    private Camera mCamera;
    private CameraPreview mPreview;
    private File pictureFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_page);

        startCameraPreview();

        Vision.Builder visionBuilder = new Vision.Builder( new NetHttpTransport(), new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyCGjUg6OfJ_YrBEHup0PhvoPJDgIcYQFc0"));

        Vision vision = visionBuilder.build();
    }

    private byte[] convertImageToByteArray(final File imageFile) {
        final byte[][] fileContent = new byte[1][1];
        AsyncTask.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                try {
                    fileContent[0] = Files.readAllBytes(imageFile.toPath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        return fileContent[0];
    }

    private void startCameraPreview() {
        if (checkCameraHardware(this)) {
            // Create an instance of Camera
            mCamera = getCameraInstance();
            Log.d("CAMERA PAGE", "Camera instance: " + mCamera);

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
            Log.d("CAMERA PAGE", "Device does not have a camera");
        }
    }

    // Add a listener to the Capture button
    private void captureButtonClickHandler(View v) {
        Log.d("CAMERA PAGE", "Capture button has been clicked!");
        mCamera.takePicture(null,null, mPicture);
    }

    // Check to see if the device has a camera
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            // the device has a camera
            return true;
        } else {
            // the device does NOT have a camera
            return false;
        }
    }

    // get an instance of the Camera object
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get the Camera instance
        } catch (Exception e) {
            // Camera is not available
            Log.d("CAMERA PAGE", "Camera is not available!");
        }
        return c; // returns null if camera is unavailable
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {

            pictureFile = getOutputMediaFile();
            Log.d("CAMERA PAGE", "pictureFile: " + pictureFile);
            if (pictureFile == null) {
                Log.d("CAMERA PAGE", "Error creating image file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bytes);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d("CAMERA PAGE", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("CAMERA PAGE", "Error accessing file: " + e.getMessage());
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
                Log.d("CAMERA PAGE", "Failed to create directory");
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

}

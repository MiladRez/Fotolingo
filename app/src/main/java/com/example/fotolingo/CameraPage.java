package com.example.fotolingo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private File imgFile;

    private ClarifaiClient client;
    private TextToSpeech t1, t2;
    private Locale lang1, lang2;
    private SortedMap<Float, String> accurateResults;
    private HashMap<Locale, String> supportedLanguages;
    private ArrayList<String> wordsToIgnore;

    private TableLayout translatedResultsLayout;
    private TextView lang1tv1, lang2tv1, lang1tv2, lang2tv2, lang1tv3, lang2tv3, lang1tv4, lang2tv4, lang1tv5, lang2tv5;
    private TextView lang1Title, lang2Title;

    Translate translate;

    String selectedLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_page);

        selectedLanguage = getIntent().getStringExtra("SelectedLanguage");

        startCameraPreview();
        startTextToSpeech();

        // setting up Clarifai api key
        ClarifaiBuilder builder = new ClarifaiBuilder("35df5fef563a41b183583e6c55ab314a");
        client = builder.buildSync();

        // array containing the top 5 most accurate results from all models
        accurateResults = new TreeMap<Float, String>(Collections.reverseOrder());

        // words to ignore from the models
        wordsToIgnore = new ArrayList<String>();
        wordsToIgnore.add("no person");
        wordsToIgnore.add("indoors");
        wordsToIgnore.add("outdoors");

        // supported languages from Google Translate API
        supportedLanguages = new HashMap<Locale, String>();
        supportedLanguages.put(Locale.ENGLISH, "en");
        supportedLanguages.put(Locale.FRENCH, "fr");
        supportedLanguages.put(Locale.GERMAN, "de");
        supportedLanguages.put(Locale.CHINESE, "zh");
        supportedLanguages.put(Locale.JAPANESE, "ja");
        supportedLanguages.put(Locale.ITALIAN, "it");
        supportedLanguages.put(Locale.KOREAN, "ko");

        //original language
        lang1 = Locale.ENGLISH;

        //translated language
        int i = 0;
        Iterator iterator = supportedLanguages.entrySet().iterator();
        while(iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();

            Locale entryLocale = (Locale) entry.getKey();

            if (entryLocale.getDisplayLanguage().equals(selectedLanguage)) {
                lang2 = entryLocale;
            }
        }

        translatedResultsLayout = findViewById(R.id.translatedResultsLayout);

        lang1Title = findViewById(R.id.lang1Title);
        lang2Title = findViewById(R.id.lang2Title);

        lang1tv1 = findViewById(R.id.lang1TextView1);
        lang1tv2 = findViewById(R.id.lang1TextView2);
        lang1tv3 = findViewById(R.id.lang1TextView3);
        lang1tv4 = findViewById(R.id.lang1TextView4);
        lang1tv5 = findViewById(R.id.lang1TextView5);

        lang2tv1 = findViewById(R.id.lang2TextView1);
        lang2tv2 = findViewById(R.id.lang2TextView2);
        lang2tv3 = findViewById(R.id.lang2TextView3);
        lang2tv4 = findViewById(R.id.lang2TextView4);
        lang2tv5 = findViewById(R.id.lang2TextView5);
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

            // creating a clone of pictureFile so that pictureFile can be safely deleted afterwards from user's internal device storage
            imgFile = pictureFile;

            // run all the models once the pictureFile has been recieved
            runGeneralModel();
            runFoodModel();
            runApparelModel();
            runTravelModel();

            try {
                TimeUnit.SECONDS.sleep(4);
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
        // start TTS on lang1
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i != TextToSpeech.ERROR) {
                    t1.setLanguage(lang1);
                }
            }
        });

        // proprietary to include Persian language
        if(!selectedLanguage.equals("Persian")) {
            // start TTS on lang2
            t2 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {
                    if (i != TextToSpeech.ERROR) {
                        t2.setLanguage(lang2);
                    }
                }
            });
        }
    }

    // if application is paused, the camera is released
    @Override
    protected void onPause() {
        releaseCamera();
        translatedResultsLayout.setVisibility(View.INVISIBLE);

        if(t1 != null || t2 != null) {
            t1.stop();
            t1.shutdown();
            // proprietary to include Persian language
            if(!selectedLanguage.equals("Persian")) {
                t2.stop();
                t2.shutdown();
            }
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
        translatedResultsLayout.setVisibility(View.INVISIBLE);
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
                .withInputs(ClarifaiInput.forImage(imgFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          if (!wordsToIgnore.contains(outputs.get(0).data().get(i).name())) {
                                              accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                          }
                                      }
                                      Log.d("CAMERA_PAGE", "General model ran!");
                                  }
                              }
                );
    }

    private void runFoodModel() {
        client.getDefaultModels().foodModel().predict()
                .withInputs(ClarifaiInput.forImage(imgFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          if (!wordsToIgnore.contains(outputs.get(0).data().get(i).name())) {
                                              accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                          }
                                      }
                                      Log.d("CAMERA_PAGE", "Food model ran!");
                                  }
                              }
                );
    }

    private void runApparelModel() {
        client.getDefaultModels().apparelModel().predict()
                .withInputs(ClarifaiInput.forImage(imgFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          if (!wordsToIgnore.contains(outputs.get(0).data().get(i).name())) {
                                              accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                          }
                                      }
                                      Log.d("CAMERA_PAGE", "Apparel model ran!");
                                  }
                              }
                );
    }

    private void runTravelModel() {
        client.getDefaultModels().travelModel().predict()
                .withInputs(ClarifaiInput.forImage(imgFile))
                .executeAsync(new ClarifaiRequest.OnSuccess<List<ClarifaiOutput<Concept>>>() {
                                  @Override
                                  public void onClarifaiResponseSuccess(List<ClarifaiOutput<Concept>> outputs) {
                                      for(int i = 0; i < 5; i++) {
                                          if (!wordsToIgnore.contains(outputs.get(0).data().get(i).name())) {
                                              accurateResults.put(outputs.get(0).data().get(i).value(), outputs.get(0).data().get(i).name());
                                          }
                                      }
                                      Log.d("CAMERA_PAGE", "Travel model ran!");
                                  }
                              }
                );
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void printResults() {
        Log.d("CAMERA_PAGE", "I RUN");

        getTranslateService();

        int i = 0;
        Iterator iterator = accurateResults.entrySet().iterator();
        while(iterator.hasNext() && i < 5) {
            Map.Entry entry = (Map.Entry) iterator.next();
            Log.d("CAMERA_PAGE", entry.getValue() + ": " + entry.getKey());

            String translatedText;

            // proprietary to include Persian language
            if(selectedLanguage.equals("Persian")) {
                translatedText = translate(entry.getValue().toString(), "fa");
            } else {
                translatedText = translate(entry.getValue().toString(), supportedLanguages.get(lang2));
            }

            Log.d("CAMERA_PAGE", translatedText);

//            Toast.makeText(getApplicationContext(), entry.getValue().toString() + ": " + translatedText, Toast.LENGTH_SHORT).show();

            translatedResultsLayout.setVisibility(View.VISIBLE);

            lang1Title.setText(lang1.getDisplayLanguage());

            // proprietary to include Persian language
            if(selectedLanguage.equals("Persian")) {
                lang2Title.setText("Persian");
            } else {
                lang2Title.setText(lang2.getDisplayLanguage());
            }

            // populate results table
            if(i == 0) {
                lang1tv1.setText(entry.getValue().toString());
                lang2tv1.setText(translatedText);
            } else if(i == 1) {
                lang1tv2.setText(entry.getValue().toString());
                lang2tv2.setText(translatedText);
            } else if(i == 2) {
                lang1tv3.setText(entry.getValue().toString());
                lang2tv3.setText(translatedText);
            } else if(i == 3) {
                lang1tv4.setText(entry.getValue().toString());
                lang2tv4.setText(translatedText);
            } else {
                lang1tv5.setText(entry.getValue().toString());
                lang2tv5.setText(translatedText);
            }

            try {
                TimeUnit.MILLISECONDS.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // initiate TTS
            t1.speak(entry.getValue().toString(), TextToSpeech.QUEUE_ADD, null);

            // proprietary to include Persian language
            if(!selectedLanguage.equals("Persian")) {
                t2.speak(translatedText, TextToSpeech.QUEUE_ADD, null);
            }

            i++;
        }
        pictureFile.delete();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void getTranslateService() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try (InputStream is = getResources().openRawResource(R.raw.credentials)) {
            final GoogleCredentials myCredentials = GoogleCredentials.fromStream(is);

            TranslateOptions translateOptions = TranslateOptions.newBuilder().setCredentials(myCredentials).build();
            translate = translateOptions.getService();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // takes in as arguments: original text (lang1) to translate and the countryCode to translate to
    public String translate(String textToTranslate, String countryCode) {
        Translation translation = translate.translate(textToTranslate, Translate.TranslateOption.targetLanguage(countryCode), Translate.TranslateOption.model("base"));
        String translatedText = translation.getTranslatedText();

        return translatedText;
    }

    public void backButtonClickHandler(View v) {
        Log.d("CAMERA_PAGE", "Back button has been clicked!");
        Intent intent = new Intent(CameraPage.this, MainActivity.class);
        startActivity(intent);
    }
}

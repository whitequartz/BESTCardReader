/*
 * Copyright (C) The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.gms.samples.vision.ocrreader;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.CameraSource;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.CameraSourcePreview;
import com.google.android.gms.samples.vision.ocrreader.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Activity for the multi-tracker app.  This app detects text and displays the value with the
 * rear facing camera. During detection overlay graphics are drawn to indicate the position,
 * size, and contents of each TextBlock.
 */
public final class OcrCaptureActivity extends AppCompatActivity {

    public static AppCompatActivity activity;


    public static TextView date, bankCardNumber, user, number;




    private static final String TAG = "OcrCaptureActivity";

    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;

    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    // Constants used to pass extra data in the intent
    public static final String AutoFocus = "AutoFocus";
    public static final String UseFlash = "UseFlash";
    public static final String TextBlockObject = "String";

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<OcrGraphic> mGraphicOverlay;

    // Helper objects for detecting taps and pinches.
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    /**
     * Initializes the UI and creates the detector pipeline.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.ocr_capture);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.about_card);

        activity = this;
        TextTemplate.templates = new ArrayList<>();

        String bonus ="|b|o|O|D|B|H";
        final String bonusNumbers ="|-";

        TextTemplate textTemplate = new TextTemplate("Date", true);
        ArrayList<String> strings = new ArrayList<>();
        strings.add("[0-1]");
        strings.add("[0-9]");
        strings.add("/");
        strings.add("[0-9]");
        strings.add("[0-9]");
        textTemplate.add(strings);

        TextTemplate backCardNumber = new TextTemplate("Bank card ID", true);
        ArrayList<String> cardNumbers = new ArrayList<>();
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);

        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);

        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);

        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        cardNumbers.add("[0-9]"+bonus);
        backCardNumber.add(cardNumbers);

        ArrayList<String> cardNumbers1 = new ArrayList<>();
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add(" ");
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add(" ");
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add(" ");
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        cardNumbers1.add("[0-9]"+bonus);
        backCardNumber.add(cardNumbers1);


        TextTemplate userId=new TextTemplate("Card ID", true);
        ArrayList<String> userIdTempl=new ArrayList<>();
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add(" ");
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userIdTempl.add("[0-9]"+bonus);
        userId.add(userIdTempl);


        TextTemplate userName=new TextTemplate("Owner", false){
            @Override
            public double evalFunc(String str) {
                if(str.length() > 4) return 1;
                return 0;
            }

            @Override
            public String getBestMatch(String str) {
                String bestString = "";
                Pattern sample = Pattern.compile("[A-Z]"+bonusNumbers);
                for(int i = 0; i < str.length(); i++){
                    int j = i;
                    int space = 0;
                    while(str.length() > j && ((space == 0 && str.charAt(j) == ' ') || sample.matcher("" + str.charAt(j)).matches())){
                        if(str.charAt(j) == ' ') space++;
                        j++;
                    }
                    if(j - i < 1) continue;
                    String substring = str.substring(i, j);
                    if(substring.length() > bestString.length()) bestString = substring;
                }
                return (bestString.length() < 1) ? null : bestString;
            }

            @Override
            public String getTotalBestMatch() {
                String bestMatch = null;
                int bestVal = 0;
                HashMap<String, Integer> counts = new HashMap<>();
                for (String str : matches) {

                    if (str != null) {
                        boolean flag = false;
                        for (Map.Entry<String, Integer> i : counts.entrySet())
                            if (i.getKey().equals(str)) {
                                flag = true;
                                i.setValue(i.getValue() + 1);
                            }
                        if (!flag)
                            counts.put(str, 1);

                        int tlen = str.length();
                        if (bestVal < tlen) {
                            bestMatch = str;
                            bestVal = tlen;
                        }
                        else if (bestVal == tlen  && counts.get(bestMatch) < counts.get(str)) {
                            bestMatch = str;
                        }
                    }
                }
                if(bestFound == null || (bestVal > bestFound.length() || bestVal == bestFound.length()))
                    bestFound = bestMatch;

                return bestFound;
            }
        };

        Button button = OcrCaptureActivity.activity.findViewById(R.id.save);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(OcrCaptureActivity.this, AboutCard.class);
                CardInstance cardInstance = new CardInstance((TextTemplate.templates.get(1).enabled) ? ((TextView) TextTemplate.templates.get(1).card.findViewById(R.id.editProperty)).getText().toString() : null,(TextTemplate.templates.get(2).enabled) ?  ((TextView) TextTemplate.templates.get(2).card.findViewById(R.id.editProperty)).getText().toString() : null, (TextTemplate.templates.get(3).enabled) ? ((TextView) TextTemplate.templates.get(3).card.findViewById(R.id.editProperty)).getText().toString() : null,(TextTemplate.templates.get(0).enabled) ?  ((TextView) TextTemplate.templates.get(0).card.findViewById(R.id.editProperty)).getText().toString() : null);
                intent.putExtra("id", cardInstance.getId());
//                try{
//                    Thread.currentThread().sleep(100);
//                }catch (Exception e){
//
//                }
                finish();
                startActivity(intent);
            }
        });


        LinearLayout cardPreview = findViewById(R.id.cardRecognizerLayout);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<OcrGraphic>) findViewById(R.id.graphicOverlay);

        // read parameters from the intent used to launch the activity.
        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }

        LinearLayout linearLayout = findViewById(R.id.propertiesLinLay);
        for(TextTemplate tt : TextTemplate.templates){
            Log.d("Template", "ADDING CARD");
            tt.toAddPropertyCard(this, linearLayout);
        }
    }

    /**
     * Handles the requesting of the camera permission.  This includes
     * showing a "Snackbar" message of why the permission is needed then
     * sending the request.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the ocr detector to detect small text samples
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated processor instance
        // is set to receive the text recognition results and display graphics for each text block
        // on screen.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay));

        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        LinearLayout cardPreview = findViewById(R.id.cardRecognizerLayout);

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(2560, 2048)
                .setRequestedFps(2.0f)
                .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode  The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions  The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *                     which is either {@link PackageManager#PERMISSION_GRANTED}
     *                     or {@link PackageManager#PERMISSION_DENIED}. Never null.
     * @see #requestPermissions(String[], int)
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // We have permission, so create the camerasource
            boolean autoFocus = getIntent().getBooleanExtra(AutoFocus,false);
            boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);
            createCameraSource(autoFocus, useFlash);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Multitracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    /**
     * Настройка шапки
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Обработка нажатия на элементы шапки
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, MainActivity.class);
                finish();
                startActivity(intent);
                return true;
            case R.id.basket:
                recreate();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

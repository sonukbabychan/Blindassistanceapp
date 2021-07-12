package com.example.blindassistanceapp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CameraService extends Service implements TextToSpeech.OnInitListener, JsonResponse {

    //Activity acti;
    TextToSpeech t1;

    //Camera variables
    //a surface holder
    private SurfaceHolder sHolder;
    //a variable to control the camera
    private Camera mCamera;
    //the camera parameters
    private Parameters parameters;

    byte[] imgData = null;
    boolean killMe = true;

    Handler hd;

    @Override
    public void onCreate() {
        super.onCreate();
        t1 = new TextToSpeech(getApplicationContext(), CameraService.this);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        killMe = true;

        hd = new Handler();
        hd.post(r);

        mCamera = Camera.open();
    }

    public Runnable r = new Runnable() {
        @Override
        public void run() {

            Toast.makeText(getApplicationContext(), "Start cam ser", Toast.LENGTH_LONG).show();

            if (killMe) {
                SurfaceView sv = new SurfaceView(getApplicationContext());

                try {
                    mCamera.setPreviewDisplay(sv.getHolder());
                    parameters = mCamera.getParameters();
//                    parameters.setFlashMode(Parameters.FLASH_MODE_ON);
//                    parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);

                    SurfaceTexture st = new SurfaceTexture(MODE_PRIVATE);
                    mCamera.setPreviewTexture(st);

                    //set camera parameters
                    mCamera.setParameters(parameters);
                    mCamera.startPreview();
                    mCamera.takePicture(null, null, mCall);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                //Get a surface
                sHolder = sv.getHolder();
                //tells Android that this surface will have its data constantly replaced
                sHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }
//            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
            hd.postDelayed(r, 15000);
        }
    };

    Camera.PictureCallback mCall = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            //decode the data obtained by the camera into a Bitmap

            FileOutputStream outStream = null;
            try {
                imgData = data;

//                SimpleDateFormat sdf = new SimpleDateFormat("hh_mm_ss");
//                String dateVal = sdf.format(new Date());
//                outStream = new FileOutputStream("/sdcard/" + dateVal + ".jpg");
//                outStream.write(imdData);
//                outStream.close();

                Toast.makeText(getApplicationContext(), "Captured", Toast.LENGTH_LONG).show();
//                speakResult("Captured");
                sendAttach();
            } catch (Exception e) {
                Log.d("CAMERA", e.getMessage());
                Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        killMe = false;
        hd.removeCallbacks(r);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendAttach() {
        try {
            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
            SharedPreferences sh = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

//            Toast.makeText(getApplicationContext(), "Length : " + imgData.length, Toast.LENGTH_SHORT).show();
            String q = "http://" + sh.getString("ip", "") + "/api/face_check/";

            Map<String, byte[]> aa = new HashMap<>();
            aa.put("imei", sh.getString("imei", "").getBytes());
            aa.put("image", imgData);

            FileUploadAsync fua = new FileUploadAsync(q);
            fua.json_response = (JsonResponse) CameraService.this;
            fua.execute(aa);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exception upload : " + e, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = t1.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("error", "This Language is not supported");
            } else {
                speakResult("Camera  Open");
            }
        } else {
            Log.e("error", "Initialization Failed.!");
        }
    }

    void speakResult(String voice) {
        t1.speak(voice, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void response(JSONObject jo) {
        try {
            if (jo.getString("status").equals("success")) {
                speakResult("The name is " + jo.getString("data"));
                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
            } else {
                speakResult("Unknown person");
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exc : " + e, Toast.LENGTH_LONG).show();
        }
    }
}
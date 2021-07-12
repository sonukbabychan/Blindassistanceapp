package com.example.blindassistanceapp;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class TrafficService extends Service implements TextToSpeech.OnInitListener {
    //Activity acti;
    TextToSpeech t1;

    //Camera variables
    //a surface holder
    private SurfaceHolder sHolder;
    //a variable to control the camera
    private Camera mCamera;
    //the camera parameters
    private Camera.Parameters parameters;

    byte[] imgData = null;
    boolean killMe = true;

    Handler hd;

    @Override
    public void onCreate() {
        super.onCreate();
        t1 = new TextToSpeech(getApplicationContext(), TrafficService.this);
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
//                outStream.write(imgData);
//                outStream.close();

//                Bitmap bit = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
//                int colour = bit.getPixel(x, y);
//
//                int red = Color.red(colour);
//                int blue = Color.blue(colour);
//                int green = Color.green(colour);
//                int alpha = Color.alpha(colour);

                Bitmap bitmap = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
                long redBucket = 0;
                long greenBucket = 0;
                long blueBucket = 0;
                long pixelCount = 0;

                for (int y = 0; y < bitmap.getHeight(); y++) {
                    for (int x = 0; x < bitmap.getWidth(); x++) {
                        int c = bitmap.getPixel(x, y);

                        pixelCount++;
                        redBucket += Color.red(c);
                        greenBucket += Color.green(c);
                        blueBucket += Color.blue(c);
                        // does alpha matter?
                    }
                }

                int averageColor = Color.rgb(redBucket / pixelCount,
                        greenBucket / pixelCount,
                        blueBucket / pixelCount);

                if (averageColor > 51200 && averageColor < 65280)
                    speakResult("Traffic is green");
                if (averageColor > 11796480 && averageColor < 16711680)
                    speakResult("Traffic is red");

                Toast.makeText(getApplicationContext(), "Captured " + averageColor, Toast.LENGTH_LONG).show();
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
}

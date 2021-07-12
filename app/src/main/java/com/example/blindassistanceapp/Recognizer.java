package com.example.blindassistanceapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class Recognizer extends ListeningActivity implements JsonResponse {

    EditText txt;
    TextToSpeech t1;

    SharedPreferences sh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        text = "";
        setContentView(R.layout.activity_recognizer);

        sh = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        try {
            if (Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
        } catch (Exception e) {
        }

        txt = (EditText) findViewById(R.id.et2);
        txt.setEnabled(false);
        // The following 3 lines are needed in every onCreate method of a ListeningActivity
        context = getApplicationContext(); // Needs to be set
        try {
            VoiceRecognitionListener.getInstance().setListener(this); // Here we set the current listener
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        startListening(); // starts listening
    }

    public static String text = "";

    @Override
    public void processVoiceCommands(String... voiceCommands) {
        //content.removeAllViews();

        if (voiceCommands[0].trim().contains("ok") || voiceCommands[0].trim().contains("OK") || voiceCommands[0].trim().contains("okey") || voiceCommands[0].trim().contains("okay")) {
            forProcess();
            stopListening();
        }

        text += voiceCommands[0] + " ";

        txt.setText(text);
        txt.setTextColor(Color.BLACK);
        txt.setGravity(Gravity.CENTER);
        restartListeningService();
    }

    void forProcess() {
//        Toast.makeText(getApplicationContext(), "BlindApp : " + text, Toast.LENGTH_LONG).show();
        if (text.contains("emergency")) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(sh.getString("emergency", "121"), null, "I'm in a trouble, http://www.google.com/maps?q=" + LocationService.lati + "," + LocationService.logi, null, null);
        } else if (text.contains("location")) {
            getLocation();
        } else if (text.contains("identify")) {
            stopService(new Intent(getApplicationContext(), TrafficService.class));
            startService(new Intent(getApplicationContext(), CameraService.class));
            startActivity(new Intent(getApplicationContext(), BlindHome.class));
        } else if (text.contains("Traffic Light") || text.contains("traffic light") || text.contains("traffic")) {
            stopService(new Intent(getApplicationContext(), CameraService.class));
            startService(new Intent(getApplicationContext(), TrafficService.class));
            startActivity(new Intent(getApplicationContext(), BlindHome.class));
        } else if (text.contains("read") || text.contains("red") || text.contains("READ") || text.contains("Read")) {
            startActivity(new Intent(getApplicationContext(), OCRRead.class));
        } else if (text.contains("send message") || text.contains("Send message")) {
            String[] msg_contents = text.split("message");
            if (msg_contents.length == 2) {
                //send message code
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(sh.getString("care_taker", "121"), null, msg_contents[1], null, null);

//                JsonReq JR = new JsonReq(getApplicationContext());
//                JR.json_response = (JsonResponse) Recognizer.this;
//                String q = "/send_message/?imei=" + sh.getString("imei", "0") + "&message=" + msg_contents[1];
//                JR.execute(q);
            }
        } else if (text.contains("exit")) {
            startActivity(new Intent(getApplicationContext(), BlindHome.class));
        } else if (text.contains("clear")) {
            text = "";
            txt.setText("");
        } else {
            speakResult("no     command    found");
            startActivity(new Intent(getApplicationContext(), BlindHome.class));
        }
    }

//    void weatherCheck() {
//        Function.placeIdTask asyncTask = new Function.placeIdTask(new Function.AsyncResponse() {
//            public void processFinish(String weather_city, String weather_description, String weather_temperature, String weather_humidity, String weather_pressure, String weather_updatedOn, String weather_iconText, String sun_rise) {
//                speakResult("weather condition in      "+weather_city+"   description     "+weather_description+"    temperature     "+weather_temperature+"   humidity    "+weather_humidity+"     pressure    "+weather_pressure);
//            }
//        });
//        asyncTask.execute(LocationService.lati, LocationService.logi);
//    }

    void getLocation() {
        try {
            Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
            List<Address> addresses = gcd.getFromLocation(Double.parseDouble(LocationService.lati), Double.parseDouble(LocationService.logi), 1);
            if (addresses.size() > 0) {
//					System.out.println(addresses.get(0).getLocality());
                String place = addresses.get(0).getSubLocality() + "    " + addresses.get(0).getLocality();
//                Toast.makeText(getApplicationContext(), place, Toast.LENGTH_SHORT).show();
                speakResult("You    are   now   at   " + place);
                Intent home = new Intent(getApplicationContext(), BlindHome.class);
                startActivity(home);
            }
        } catch (Exception e) {
        }
    }

    void speakResult(String voice) {
        t1.speak(voice, TextToSpeech.QUEUE_FLUSH, null);
    }

    @Override
    public void onBackPressed() {
        stopListening();
        Intent back = new Intent(getApplicationContext(), BlindHome.class);
        startActivity(back);
        txt.setText("");
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_UP) {
                    if (event.getEventTime() - event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
                        try {
                            stopListening();
                            Intent rec = new Intent(getApplicationContext(), BlindHome.class);
                            startActivity(rec);
                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }

    @Override
    public void response(JSONObject jo) {
        try {
            if (jo.getString("method").equals("send_message")) {
                if (jo.getString("status").equals("success")) {
                    stopListening();
                    speakResult("Message sent");
                    startActivity(new Intent(getApplicationContext(), BlindHome.class));
                }
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exc : " + e, Toast.LENGTH_LONG).show();
        }
    }
}
package com.example.blindassistanceapp;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements JsonResponse {
    EditText e1;
    Button b1;
    String ip_add;
    String IMEI_Number_Holder;
    TelephonyManager telephonyManager;
    SharedPreferences sh;
    TextToSpeech t1;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            if (Build.VERSION.SDK_INT > 9) {
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
                StrictMode.setThreadPolicy(policy);
            }
        } catch (Exception e) {

        }

        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        sh = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        b1 = findViewById(R.id.btsub);
        e1 = findViewById(R.id.ip1);
        e1.setText(sh.getString("ip", "172.20.10.2:5004"));

        telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        IMEI_Number_Holder = telephonyManager.getDeviceId();
        Toast.makeText(getApplicationContext(), IMEI_Number_Holder + " ", Toast.LENGTH_LONG).show();

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ip_add = e1.getText().toString();
                if (ip_add.equals(""))
                    e1.setError("IP address please");
                else {
                    SharedPreferences.Editor ed = sh.edit();
                    ed.putString("ip", ip_add);
                    ed.putString("imei", IMEI_Number_Holder);
                    ed.commit();
                    checkImei();
                }
            }
        });
    }

    void checkImei() {
        JsonReq JR = new JsonReq(getApplicationContext());
        JR.json_response = (JsonResponse) MainActivity.this;
        String q = "/imei_check/?imei=" + IMEI_Number_Holder;
        JR.execute(q);
    }

    @Override
    public void response(JSONObject jo) {
        try {
            String status = jo.getString("status");
            Log.d("pearl_status", status);

            if (status.equalsIgnoreCase("success")) {
                SharedPreferences.Editor ed = sh.edit();
                ed.putString("care_taker", jo.getString("data"));
                ed.commit();

                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
                t1.speak("Welcome to blind assistance application", TextToSpeech.QUEUE_FLUSH, null);
                String instruction = "Instructions for you                                " +
                        "for accessing this application we have some voice commands for you               " +
                        "to record voice commands, long press volume down button and release it" +
                        "then use commends followed by ok                                   " +
                        "commands are, one read                   " +
                        " command two is send message followed by message                     " +
                        "and last command is emergency";
                t1.speak(instruction, TextToSpeech.QUEUE_FLUSH, null);
                Thread.sleep(5000);
                startService(new Intent(getApplicationContext(), LocationService.class));
                startActivity(new Intent(getApplicationContext(), BlindHome.class));
            } else {
                Toast.makeText(getApplicationContext(), "Failed!!", Toast.LENGTH_LONG).show();
                t1.speak("You have not registered yet, ask someone to help you to register on this application", TextToSpeech.QUEUE_FLUSH, null);
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exc : " + e, Toast.LENGTH_LONG).show();
        }
    }
}
package com.example.blindassistanceapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.widget.Toast;

import org.json.JSONObject;

public class BlindHome extends AppCompatActivity implements JsonResponse {

    SharedPreferences sh;
    public static int SWITCH_FLAG = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blind_home);

        sh = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        getEmergencyNumber();
        if (SWITCH_FLAG == 0) {
            SWITCH_FLAG = 1;
//            startService(new Intent(getApplicationContext(), CameraService.class));
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_UP) {
                    if (event.getEventTime() - event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
                        try {
                            Thread.sleep(2000);
                            stopService(new Intent(getApplicationContext(), CameraService.class));
                            stopService(new Intent(getApplicationContext(), TrafficService.class));
                            Intent rec = new Intent(getApplicationContext(), Recognizer.class);
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

    void getEmergencyNumber() {
        JsonReq JR = new JsonReq(getApplicationContext());
        JR.json_response = (JsonResponse) BlindHome.this;
        String q = "/get_emergency_number/?imei=" + sh.getString("imei", "0");
        JR.execute(q);
    }

    @Override
    public void response(JSONObject jo) {
        try {
            String status = jo.getString("status");
            Log.d("pearl_status", status);

            if (status.equalsIgnoreCase("success")) {
                SharedPreferences.Editor ed = sh.edit();
                ed.putString("emergency", jo.getString("data"));
                ed.commit();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Exc : " + e, Toast.LENGTH_LONG);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SWITCH_FLAG = 0;
        stopService(new Intent(getApplicationContext(), CameraService.class));
        stopService(new Intent(getApplicationContext(), TrafficService.class));
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}

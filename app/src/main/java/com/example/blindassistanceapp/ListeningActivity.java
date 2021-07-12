package com.example.blindassistanceapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;

public abstract class ListeningActivity extends Activity implements IVoiceControl {

    protected SpeechRecognizer sr;
    protected Context context;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if(android.os.Build.VERSION.SDK_INT>9)
        {
            StrictMode.ThreadPolicy p=new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(p);
        }
    }

     // starts the service+
     protected void startListening() {
         try {
             //Toast.makeText(getApplicationContext(), "Recording...", Toast.LENGTH_SHORT).show();
              initSpeech();
             Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
             intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
             if (!intent.hasExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE))
             {
                 intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, "com.dummy");
             }
             // Toast.makeText(getApplicationContext(), "Recording...", Toast.LENGTH_SHORT).show();
             sr.startListening(intent);
         } catch(Exception ex) {
             Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
         }
     }
 
 // stops the service
     protected void stopListening() {
         if (sr != null) {
             sr.stopListening();
             sr.cancel();
             sr.destroy();
         }
         sr = null;
     }

    protected void initSpeech() {
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(this);
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Toast.makeText(context, "Speech Recognition is not available", Toast.LENGTH_LONG).show();
                finish();
            }
            sr.setRecognitionListener(VoiceRecognitionListener.getInstance());
        }
    }

 @Override
 public void finish() {
     stopListening();
     super.finish();
 }
 
 @Override
 protected void onStop() {
     stopListening();
     super.onStop();
 }
 
    @Override
 protected void onDestroy() {
     if (sr != null) {
         sr.stopListening();
         sr.cancel();
         sr.destroy();
        }
     super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        if(sr!=null) {
            sr.stopListening();
            sr.cancel();
            sr.destroy();
        }
        sr = null;
        super.onPause();
    }
    
    //is abstract so the inheriting classes need to implement it. Here you put your code which should be executed once a command was found

 public abstract void processVoiceCommands(String... voiceCommands);
    

 public void restartListeningService() {
     stopListening();
     startListening();
 }
}
package com.example.blindassistanceapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.Locale;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

public class OCRRead extends AppCompatActivity implements TextToSpeech.OnInitListener {

	SurfaceView cameraView;
	TextView textView;
	CameraSource cameraSource;
	final int RequestCameraPermissionID = 1001;

	TextToSpeech t1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ocrread);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_ocrread);

		t1 = new TextToSpeech(getApplicationContext(), OCRRead.this);

		cameraView = (SurfaceView) findViewById(R.id.surface_view);
		textView = (TextView) findViewById(R.id.text_view);

		final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
		if (!textRecognizer.isOperational()) {
			Log.w("MainActivity", "Detector dependencies are not yet available");
		} else {
			cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
					.setFacing(CameraSource.CAMERA_FACING_BACK)
					.setRequestedPreviewSize(1280, 1024)
					.setRequestedFps(2.0f)
					.setAutoFocusEnabled(true)
					.build();
			cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {

				@Override
				public void surfaceCreated(SurfaceHolder surfaceHolder) {
					if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
						ActivityCompat.requestPermissions(OCRRead.this,
								new String[]{Manifest.permission.CAMERA},
								RequestCameraPermissionID);
						return;
					}
					try {
						cameraSource.start(cameraView.getHolder());
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				@Override
				public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

				}

				@Override
				public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
					cameraSource.stop();
				}
			});

			startRecognition(textRecognizer);

//			textView.setOnClickListener(new View.OnClickListener() {
//				@Override
//				public void onClick(View view) {
//
//					textRecognizer.release();
//
//					AlertDialog.Builder alert = new AlertDialog.Builder(OCRRead.this);
//
//					alert.setTitle("Save");
//					alert.setMessage("Do You Want to Save this Data ?");
//
//					// Set an EditText view to get user input
//					final EditText input = new EditText(getApplicationContext());
//					input.setText("Untitled");
//					alert.setView(input);
//
//					alert.setPositiveButton(" OK ", new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int whichButton) {
//							String value = input.getText().toString();
//							File mediaStorage = new File(Environment.getExternalStorageDirectory() + File.separator + "OFFLINE");
//							if (!mediaStorage.mkdir()) {
//								mediaStorage.mkdirs();
//							}
//							try {
//								String data = textView.getText().toString();
//								File txt_file = new File(mediaStorage + File.separator + value + ".txt");
//								FileOutputStream fos = new FileOutputStream(txt_file);
//								fos.write(data.getBytes());
//								fos.close();
//
//								Toast.makeText(getApplicationContext(), "File Saved at : " + txt_file, Toast.LENGTH_LONG).show();
//								startRecognition(textRecognizer);
//							} catch (Exception ex) {
//								ex.printStackTrace();
//								Toast.makeText(getApplicationContext(), "Error : " + ex.getMessage(), Toast.LENGTH_LONG).show();
//							}
//						}
//					});
//
//					alert.setNegativeButton(" Cancel ", new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int whichButton) {
//							startRecognition(textRecognizer);
//						}
//					});
//
//					alert.show();
//				}
//			});
		}
	}

	private void startRecognition(TextRecognizer textRecognizer) {
		textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
			@Override
			public void release() {

			}

			@Override
			public void receiveDetections(Detector.Detections<TextBlock> detections) {
				final SparseArray<TextBlock> items = detections.getDetectedItems();
				if (items.size() != 0) {
					textView.post(new Runnable() {
						@Override
						public void run() {
							StringBuilder stringBuilder = new StringBuilder();
							for (int i = 0; i < items.size(); i++) {
								TextBlock item = items.valueAt(i);
								stringBuilder.append(item.getValue());
								stringBuilder.append("\n");
							}
							textView.setText(stringBuilder.toString());
							speakResult(textView.getText().toString());
						}
					});
				}
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		switch (requestCode) {
			case RequestCameraPermissionID:
				if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
						return;
					} else {
						try {
							cameraSource.start(cameraView.getHolder());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
		}
	}

	@Override
	public void onInit(int status) {
		// TODO Auto-generated method stub
		if (status == TextToSpeech.SUCCESS) {
			int result = t1.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e("error", "This Language is not supported");
			}
		} else {
			Log.e("error", "Initialization Failed!");
		}
	}

	void speakResult(String voice) {
		t1.speak(voice, TextToSpeech.QUEUE_FLUSH, null);
	}

	public boolean dispatchKeyEvent(KeyEvent event) {
		int action = event.getAction();
		int keyCode = event.getKeyCode();
		switch (keyCode) {
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (action == KeyEvent.ACTION_UP) {
					if (event.getEventTime() - event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
						try {
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

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		startActivity(new Intent(getApplicationContext(), Recognizer.class));
	}
}
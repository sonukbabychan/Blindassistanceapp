package com.example.blindassistanceapp;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import java.util.List;
import java.util.Locale;

import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

public class LocationService extends Service {

	private LocationManager locationManager;
	private Boolean locationChanged;

	private Handler handler = new Handler();
	public static Location curLocation;
	public static boolean isService = true;

	public static String place = "", address = "", lati = "", logi = "";
	SharedPreferences sh;

	TextToSpeech t1;

	List<String> locname, loccount;
	LocationListener locationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
//			Toast.makeText(getApplicationContext(), "IN LOCATION SERVICE", Toast.LENGTH_LONG).show();
			if (curLocation == null) {
				curLocation = location;
				locationChanged = true;
			} else if (curLocation.getLatitude() == location.getLatitude() && curLocation.getLongitude() == location.getLongitude()) {
				locationChanged = false;
				return;
			} else
				locationChanged = true;
			curLocation = location;

			if (locationChanged)
				locationManager.removeUpdates(locationListener);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

			if (status == 0)// UnAvailable
			{
			} else if (status == 1)// Trying to Connect
			{
			} else if (status == 2) {// Available
			}
		}
	};

	@RequiresApi(api = Build.VERSION_CODES.M)
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();

		sh = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
			@Override
			public void onInit(int status) {
				if (status != TextToSpeech.ERROR) {
					t1.setLanguage(Locale.UK);
				}
			}
		});

		curLocation = getBestLocation();
//		Toast.makeText(getApplicationContext(), "location service" + curLocation, Toast.LENGTH_LONG).show();
		if (curLocation == null) {
			System.out.println("starting problem.........3...");
			Toast.makeText(this, "GPS problem..........", Toast.LENGTH_SHORT).show();
		} else {
			// Log.d("ssssssssssss", String.valueOf("latitude2.........."+curLocation.getLatitude()));
		}
		isService = true;
	}

	final String TAG = "LocationService";

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override

	public void onLowMemory() {
		super.onLowMemory();
	}


	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, "Start services", Toast.LENGTH_SHORT).show();

		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (!provider.contains("gps")) { //if gps is disabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}
		handler.post(GpsFinder);
	}

	@Override
	public void onDestroy() {
		String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

		if (provider.contains("gps")) { //if gps is enabled
			final Intent poke = new Intent();
			poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
			poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
			poke.setData(Uri.parse("3"));
			sendBroadcast(poke);
		}

		handler.removeCallbacks(GpsFinder);
		handler = null;
		Toast.makeText(this, "Service Stopped..!!", Toast.LENGTH_SHORT).show();
		isService = false;
	}


	public Runnable GpsFinder = new Runnable() {

		@RequiresApi(api = Build.VERSION_CODES.M)
		public void run() {

			String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);

			if (!provider.contains("gps")) { //if gps is disabled
				final Intent poke = new Intent();
				poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
				poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
				poke.setData(Uri.parse("3"));
				sendBroadcast(poke);
			}

//			TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
//			phoneid = telephonyManager.getDeviceId().toString();

			Location tempLoc = getBestLocation();

			if (tempLoc != null) {
				curLocation = tempLoc;
				// Log.d("MyService", String.valueOf("latitude"+curLocation.getLatitude()));

				lati = String.valueOf(curLocation.getLatitude());
				logi = String.valueOf(curLocation.getLongitude());
//				Toast.makeText(getApplicationContext(), "lattitude" + lati + "longitude" + logi, Toast.LENGTH_LONG).show();
				updateLocationOfBlind();
			}
			handler.postDelayed(GpsFinder, 5000);// register again to start after 20 seconds...
		}
	};

	@RequiresApi(api = Build.VERSION_CODES.M)
	private Location getBestLocation() {
		Location gpslocation = null;
		Location networkLocation = null;
		if (locationManager == null) {
			locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		}
		try {
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

				if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
					// TODO: Consider calling
					//    Activity#requestPermissions
					// here to request the missing permissions, and then overriding
					//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
					//                                          int[] grantResults)
					// to handle the case where the user grants the permission. See the documentation
					// for Activity#requestPermissions for more details.
					return gpslocation;
				}
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);// here you can set the 2nd argument time interval also that after how much time it will get the gps location
				gpslocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				//  System.out.println("starting problem.......7.11....");
	              
	            }
	            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
	                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000, 0, locationListener);
	                networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	            }
	        } catch (IllegalArgumentException e) {
	            Log.e("error", e.toString());
	        }
	        if(gpslocation==null && networkLocation==null)
	            return null;

	        if(gpslocation!=null && networkLocation!=null){
	            if(gpslocation.getTime() < networkLocation.getTime()){
	                gpslocation = null;
	                return networkLocation;
	            }else{
	                networkLocation = null;
	                return gpslocation;
	            }
	        }
	        if (gpslocation == null) {
	            return networkLocation;
	        }
	        if (networkLocation == null) {
	            return gpslocation;
	        }
	        return null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	void updateLocationOfBlind() {
		if (sh.getString("start_flag", "0").equals("1") || sh.getString("start_flag", "0").equals("2")) {
			try {

				getReplyMessages();
			} catch (Exception e) {
			}
		}
	}

	void getReplyMessages() {
		try {

		} catch (Exception e) {
		}
	}
}

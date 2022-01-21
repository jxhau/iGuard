package com.jxhau.iguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.huawei.hmf.tasks.OnFailureListener;
import com.huawei.hmf.tasks.OnSuccessListener;
import com.huawei.hmf.tasks.Task;
import com.huawei.hms.api.bean.HwAudioPlayItem;
import com.huawei.hms.audiokit.player.callback.HwAudioConfigCallBack;
import com.huawei.hms.audiokit.player.manager.HwAudioManager;
import com.huawei.hms.audiokit.player.manager.HwAudioManagerFactory;
import com.huawei.hms.audiokit.player.manager.HwAudioPlayerConfig;
import com.huawei.hms.audiokit.player.manager.HwAudioPlayerManager;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.common.ResolvableApiException;
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationAvailability;
import com.huawei.hms.location.LocationCallback;
import com.huawei.hms.location.LocationRequest;
import com.huawei.hms.location.LocationResult;
import com.huawei.hms.location.LocationServices;
import com.huawei.hms.location.LocationSettingsRequest;
import com.huawei.hms.location.LocationSettingsResponse;
import com.huawei.hms.location.LocationSettingsStatusCodes;
import com.huawei.hms.location.SettingsClient;
import com.huawei.hms.maps.CameraUpdate;
import com.huawei.hms.maps.CameraUpdateFactory;
import com.huawei.hms.maps.HuaweiMap;
import com.huawei.hms.maps.MapView;
import com.huawei.hms.maps.MapsInitializer;
import com.huawei.hms.maps.OnMapReadyCallback;
import com.huawei.hms.maps.model.CameraPosition;
import com.huawei.hms.maps.model.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SosActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = "SosActivity";
    private static final String apiKey = "api_key";
    private static final int SEND_SMS_PERMISSION_REQUEST_CODE = 1;

    // creating constant keys for shared preferences.
    public static final String SHARED_PREFS = "shared_prefs";
    // key for storing email.
    public static final String CONTACT_NAME = "contact_name";
    // key for storing password.
    public static final String PHONE_NUMBER = "phone_number";
    // variable for shared preferences.
    SharedPreferences sharedpreferences;
    String smsNumber, message, mAddress;
    String default_message = "[iGuard] Help me! Location: ";

    // Huawei map.
    private HuaweiMap hMap;
    private MapView mMapView;
    private double myLat, myLong;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    FusedLocationProviderClient fusedLocationProviderClient;

    LocationRequest mLocationRequest = new LocationRequest();
    LocationCallback mLocationCallback;
    // loud sound
    private AudioManager audioManager;
    // Audio
    List<HwAudioPlayItem> playItemList = new ArrayList<>();
    private HwAudioPlayerManager mHwAudioPlayerManager;
    private HwAudioManager mHwAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        // initializing our shared preferences.
        sharedpreferences = getSharedPreferences(SHARED_PREFS, this.MODE_PRIVATE);
        // getting data from shared prefs and storing it in our string variable.
        String phoneNumber = sharedpreferences.getString(PHONE_NUMBER, null);
        setSmsNumber(phoneNumber);

        // loud sound
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        checkPermission();
        //checkLocationSettings();
        requestLocationUpdate();

        // alarm
        initAudio();

        // TODO: Hide Api key
        MapsInitializer.setApiKey(apiKey);

        mMapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null){
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationSettings();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHwAudioPlayerManager.stop();
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 5, 0);
    }

    public void setSmsNumber(String smsNumber) {
        this.smsNumber = smsNumber;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public double getMyLat() {
        return myLat;
    }

    public double getMyLong() {
        return myLong;
    }

    public void setMyLat(double myLat) {
        this.myLat = myLat;
    }

    public void setMyLong(double myLong) {
        this.myLong = myLong;
    }

    public void checkPermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Log.i(TAG, "sdk <= 28 Q");
            if (ContextCompat.checkSelfPermission(SosActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                checkLocationSettings();
            }
        } else {
            // Dynamically apply for required permission if the API level is greater than 28
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_BACKGROUND_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            }
        }
    }

    public void checkLocationSettings() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        LocationSettingsRequest locationSettingsRequest = builder.build();
        mLocationRequest = new LocationRequest();
        builder.addLocationRequest(mLocationRequest);
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        // Check the device location settings.
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        fusedLocationProviderClient
                                .requestLocationUpdates(mLocationRequest, mLocationCallback,Looper.getMainLooper());
                        getLastLocation();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException rae = (ResolvableApiException) e;
                            // Call startResolutionForResult to display a pop-up asking the user to enable related permission.
                            rae.startResolutionForResult(SosActivity.this, 0);
                        } catch (IntentSender.SendIntentException sie) {
                        }
                        break;
                }
            }
        });
    }

    private void requestLocationUpdate() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };
        fusedLocationProviderClient
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "request location updates success");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "request location updates failed, error: " + e.getMessage());
                    }
                });
    }

    public void getLastLocation() {
        // get last known location
        Task<Location> task = fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location == null) {
                            return;
                        }
                        // logic for processing the Location object upon success
                        Log.i(TAG,
                                "onLocationResult location[Longitude,Latitude,Accuracy]:"
                                        + location.getLongitude() + "," + location.getLatitude()
                                        + "," + location.getAccuracy());
                        initAudio();
                        setMyLat(location.getLatitude());
                        setMyLong(location.getLongitude());

                        LatLng latLngCurrent = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraPosition build = new CameraPosition.Builder().target(latLngCurrent).zoom(18).build();
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(build);
                        hMap.animateCamera(cameraUpdate);

                        // reverse geocoding
                        Geocoder gc = new Geocoder(SosActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            StringBuilder sb = new StringBuilder();
                            String result = null;
                            if (addresses.size() > 0) {
                                Address address = addresses.get(0);
                                result = address.getAddressLine(0);
                            }
                            setMessage(default_message + result);
                            // send SMS
                            sendSms();
                        }catch (IOException e){e.printStackTrace();}
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(getApplicationContext(), "Failed to retrieve location", Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onMapReady(HuaweiMap huaweiMap) {
        Log.d(TAG, "onMapReady: ");

        float zoom = 18f;
        double latitude = getMyLat();
        double longitude = getMyLong();
        LatLng myLatLng = new LatLng(latitude, longitude);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(myLatLng, zoom);

        hMap = huaweiMap;

        //checkPermission();
        //checkLocationSettings();
        hMap.setMyLocationEnabled(true);
        hMap.getUiSettings().setZoomControlsEnabled(true);
        hMap.getUiSettings().setCompassEnabled(true);
        hMap.getUiSettings().setZoomGesturesEnabled(true);
        hMap.animateCamera(cameraUpdate);
    }

    // Audio Kit
    // Initialize the SDK.
    @SuppressLint("StaticFieldLeak")
    public void initAudio() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                // Create a configuration instance, which contains playback configurations.
                HwAudioPlayerConfig hwAudioPlayerConfig = new HwAudioPlayerConfig(getApplicationContext());
                // Create a control instance.
                HwAudioManagerFactory.createHwAudioManager(hwAudioPlayerConfig, new HwAudioConfigCallBack() {
                    // Return the control instance through callback.
                    @Override
                    public void onSuccess(HwAudioManager hwAudioManager) {
                        mHwAudioManager = hwAudioManager;
                        // Obtain the playback control instance.
                        mHwAudioPlayerManager = hwAudioManager.getPlayerManager();
                        mHwAudioPlayerManager.playList(getOfflinePlayItemList(), 0, 0);
                        mHwAudioPlayerManager.setPlayMode(3);
                        mHwAudioPlayerManager.setVolume(100);
                    }
                    @Override
                    public void onError(int errorCode) { }
                });

                return null;
            }
        }.execute();
    }

    public List<HwAudioPlayItem> getOfflinePlayItemList() {
        HwAudioPlayItem item = new HwAudioPlayItem();
        // Set the path of an audio file in \app\src\main\assets of an Android Studio project.
        item.setFilePath("hms_assets://alarm.mp3");
        playItemList.add(item);
        return playItemList;
    }

    protected void sendSms() {
        String smsNumber = getSmsNumber();
        String message = getMessage();

        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(smsNumber, null, message, null, null);
        Toast.makeText(getApplicationContext(), "SMS sent.",
                Toast.LENGTH_LONG).show();
    }

}

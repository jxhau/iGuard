package com.jxhau.iguard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import com.huawei.hms.location.FusedLocationProviderClient;
import com.huawei.hms.location.LocationServices;
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
    String default_message = "[iGuard] Help me! At location ";

    // Huawei map.
    private HuaweiMap hMap;
    private MapView mMapView;
    private double myLat, myLong;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    FusedLocationProviderClient fusedLocationProviderClient;

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

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // alarm
        initAudio();

        // TODO: Hide Api key
        MapsInitializer.setApiKey("CgB6e3x9ZY65/SgwlUKUZ/DJFf2ti54FoxsOas3nrNvz4fYlObSHbo36AaX4z8zzXdEEo31G+ChsYBEerwpcFFzs");

        mMapView = findViewById(R.id.mapView);
        Bundle mapViewBundle = null;
        if (savedInstanceState != null){
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        getLastLocation();

        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);

    }

    @Override
    protected void onStart() { super.onStart(); }

    @Override
    protected void onResume() {
        super.onResume();
        getLastLocation();
    }

    @Override
    protected void onPause() { super.onPause(); }

    @Override
    protected void onStop() {
        super.onStop();
        mHwAudioPlayerManager.stop();
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

                        // send SMS
                        setMessage(default_message + "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude());
                        sendSms();

                        LatLng latLngCurrent = new LatLng(location.getLatitude(), location.getLongitude());
                        CameraPosition build = new CameraPosition.Builder().target(latLngCurrent).zoom(18).build();
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(build);
                        hMap.animateCamera(cameraUpdate);
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
        hMap.setMyLocationEnabled(true);
        hMap.getUiSettings().setZoomControlsEnabled(true);
        hMap.getUiSettings().setCompassEnabled(true);
        hMap.getUiSettings().setZoomGesturesEnabled(true);
        hMap.animateCamera(cameraUpdate);
        hMap.setOnMapClickListener(new HuaweiMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Toast.makeText(getApplicationContext(), "onMapClick:" + latLng.toString(), Toast.LENGTH_SHORT).show();
            }
        });
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







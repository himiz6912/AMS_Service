package jp.hmproject.ams_service;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by hm on 12/25/2016.
 */
public class AMS_LocationManager implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener {

    protected static final String TAG = "AMS_LocationManager";
    private AMS_LocationManagerListener listener;

    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    protected Boolean mRequestingLocationUpdates;
    protected String mLastUpdateTime;

    public AMS_LocationManager(Context context) {
        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }
    protected void createLocationRequest(String a, int i) {
        long interval = i * 1000;
        long fastest_interval = interval / 2;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(fastest_interval);
        if(a.equals("HIGH_ACCURACY")) {
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }else if(a.equals("BALANCED_POWER_ACCURACY")){
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        }else if(a.equals("LOW_POWER")){
            mLocationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        }else if(a.equals("NO_POWER")){
            mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
        }
    }
    protected void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }catch (SecurityException e){
            Log.e(TAG,"startLocation:" + e.getMessage());
        }
    }
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        try {
            if (mCurrentLocation == null) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            }
        }catch(SecurityException e){
            Log.e(TAG,"onConnected:" + e.getMessage());
        }
        listener.locationServiceConnected();
    }
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        listener.changeLocationData();
        mLastUpdateTime = DateFormat.getTimeInstance().format(mCurrentLocation.getTime());
        Log.i(TAG,"UPDATE:" + mLastUpdateTime + " ,LON:" + mCurrentLocation.getLongitude()
                + " ,LAT:" + mCurrentLocation.getLatitude());
        stopLocationUpdates();
    }
    @Override
    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    public void startUpdating() {
        if(mGoogleApiClient.isConnected()) {
            if (!mRequestingLocationUpdates) {
                mRequestingLocationUpdates = true;
                startLocationUpdates();
            }
        }else{
            mGoogleApiClient.connect();
        }
    }
    public void stopUpdating() {
        if(mGoogleApiClient.isConnected()) {
            if (mRequestingLocationUpdates) {
                mRequestingLocationUpdates = false;
                stopLocationUpdates();
            }
        }
    }
    public void updateSetting(String accuracy, int interval){
        stopUpdating();
        createLocationRequest(accuracy,interval);
    }
    public Location getLocationData(){
        return mCurrentLocation;
    }
    public boolean isRequesting(){
        return mRequestingLocationUpdates;
    }
    public void close() {
        mGoogleApiClient.disconnect();
    }

    public void setListener(AMS_LocationManagerListener listener){
        this.listener = listener;
    }
}
package jp.hmproject.ams_service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainService extends Service implements
        SharedPreferences.OnSharedPreferenceChangeListener,AMS_LocationManagerListener{
    final String TAG = "MainService";
    private final String[] status = {"FINISH", "INIT", "SUSPEND", "NORMAL"};
    final RemoteCallbackList<AMS_Callback> callback_list = new RemoteCallbackList<AMS_Callback>();
    private int start_id;
    private SharedPreferences sp;
    private AMS_DBManager dbm;
    private AMS_LocationManager alm;
    private Handler handler;
    private Timer timer;
    private int tracingTime;
    private int sendingTime;
    private int tCount;
    private int sCount;
    private Date requestDate;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        if (AMS_Remote.class.getName().equals(intent.getAction())) {
            return binder;
        }
        ;
        return null;
    }

    private AMS_Remote.Stub binder = new AMS_Remote.Stub() {
        @Override
        public void registerCallback(AMS_Callback cb) throws RemoteException {
            callback_list.register(cb);
        }

        @Override
        public void unregisterCallback(AMS_Callback cb) throws RemoteException {
            callback_list.unregister(cb);
        }

        @Override
        public void command(String msg) throws RemoteException {
            mainCommand(msg);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        start_id = startId;
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void finishTask() {
        setLastActiveDate();
        sp.unregisterOnSharedPreferenceChangeListener(this);
        timer.cancel();
        timer = null;
        dbm.close();
        dbm = null;
        stopLocationService();
        alm.close();
        alm = null;
        callback_list.kill();
        stopForeground(true);
        stopSelf(start_id);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbm = new AMS_DBManager(this);
        alm = new AMS_LocationManager(this);
        alm.setListener(this);
        handler = new Handler();
        timer = new Timer();
        timer.schedule(new myTimer(), 0, 1000);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor e = sp.edit();
        e.putString("Status", "");
        e.commit();
        sp.registerOnSharedPreferenceChangeListener(this);
    }
    private void broadcast(String msg) {
        int n = callback_list.beginBroadcast();
        for (int i = 0; i < n; i++) {
            try {
                callback_list.getBroadcastItem(i).basicTypes(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "broadcast:" + e.getMessage());
            }
        }
        callback_list.finishBroadcast();
    }
    private void transitState(String state) {
        if (Arrays.asList(status).contains(state)) {
            SharedPreferences.Editor e = sp.edit();
            e.putString("Status", state);
            e.commit();
        } else {
            Log.e(TAG, "transitState:" + state);
        }
    }
    public void mainCommand(String command) {
        String status = sp.getString("Status", "");
        if (command.equals("APP_FINISH")) {
            transitState("FINISH");
        } else if (command.equals("APP_INIT")) {
            transitState("INIT");
        } else if (command.equals("APP_SUSPEND")) {
            transitState("SUSPEND");
        } else if (command.equals("APP_NORMAL")) {
            transitState("NORMAL");
        } else if (command.equals("TRACE_DATA")){
            sendAllTracedData();
        }
    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals("Status")) {
            String v = sharedPreferences.getString(s, "");
            broadcast("Status:" + v);
            if (v.equals("FINISH")) {
                finishTask();
            } else if (v.equals("INIT")) {
                initSetting();
                startLocationService();
                transitState("NORMAL");
            } else if (v.equals("SUSPEND")) {
                stopLocationService();
            } else if (v.equals("NORMAL")) {
                doTracing();
                doSending();
            }
        }
    }
    @Override
    public void changeLocationData() {
        Location location = alm.getLocationData();
        if(location != null) {
            AMS_Data ams_data = new AMS_Data();
            ams_data.setTraceData(location.getAccuracy(),location.getLatitude(),
                    location.getLongitude(),new Date(location.getTime()),requestDate);
            dbm.setTraceData(ams_data);
            Log.d("TEST", "Location:" + location.getLatitude() + "," + location.getLongitude());
            requestDate = null;
        }else{
            Log.d("TEST", "Can't get the location data.");
        }
    }
    @Override
    public void locationServiceConnected() {
        changeLocationServiceSetting();
        doTracing();
    }


    //
    // The other thread for timer
    //
    private class myThread extends Thread {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    doTimerTask();
                }
            });
        }
    }
    private class myTimer extends TimerTask {
        @Override
        public void run() {
            myThread t = new myThread();
            t.start();
        }
    }
    private void doTimerTask() {
        String status = sp.getString("Status", "");
        tCount++;
        sCount++;
        if(status.equals("")){
            transitState("INIT");
        }else if (status.equals("NORMAL")) {
            if(!alm.isRequesting())startLocationService();
            int t = tracingTime - tCount > 0 ? tracingTime - tCount : 0;
            int s = sendingTime - sCount > 0 ? sendingTime - sCount : 0;
            if (t == 0) doTracing();
            if (s == 0) doSending();
        }
    }

    //
    // From the below, define normal operation.
    //
    private void initSetting() {
        String la = sp.getString("LastActive", "");
        if (la.equals("")) {
            SharedPreferences.Editor e = sp.edit();
            e.putString("Tracing", "60");//more than 59s
            e.putString("Sending", "3000"); //more than 5m
            e.putString("Accuracy","HIGH_ACCURACY"); //LOW_POWER/NO_POWER/BALANCED_POWER_ACCURACY
            e.putString("Updating","30");//a half of tracing interval
            e.putString("Server", "0.0.0.0/24");
            e.putString("Port", "60001");
            e.putString("Protocol", "HTTP");//HTTP/HTTPS
            e.commit();
        }
        tracingTime = Integer.parseInt(sp.getString("Tracing", "60"));
        sendingTime = Integer.parseInt(sp.getString("Sending", "300"));
        doTracing();
    }

    private void startLocationService(){
        alm.startUpdating();
    }
    private void stopLocationService(){
        alm.stopUpdating();
    }
    private void changeLocationServiceSetting(){
        String accuracy = sp.getString("Accuracy","HIGH_ACCURACY");
        int interval = Integer.parseInt(sp.getString("Updating","30"));
        alm.updateSetting(accuracy,interval);
    }

    private void doTracing() {
        if(requestDate == null){
            requestDate = new Date();
            alm.startUpdating();
        }else{
            alm.stopUpdating();
            while (alm.isRequesting()){};
            requestDate = new Date();
            alm.startUpdating();
        }
        tCount = 0;
    }


    private void sendAllTracedData(){
        String s = dbm.getAllTraceData();
        broadcast("TRACE_DATA:" + s);
    }

    private void doSending() {
        sCount = 0;
    }

    private void setLastActiveDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SharedPreferences.Editor e = sp.edit();
        e.putString("LastActive", sdf.format(new Date()));
        e.commit();
    }
}

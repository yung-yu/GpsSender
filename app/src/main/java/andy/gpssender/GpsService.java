package andy.gpssender;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by andyli on 2016/3/28.
 */
public class GpsService extends Service{
    private final String TAG = "GpsService";
    public static final String BUNDLE_KEY_CMD = "bundle_key_cmd";
    public static final String BUNDLE_KEY_MINTIME = "bundle_key_mintime";
    public static final String BUNDLE_KEY_MINDISTANCE = "bundle_key_minDistance";
    public static final String BUNDLE_KEY_ACTIVITYID = "bundle_key_activityId";
    public static final String GPS_START = "gps_start";
    public static final String GPS_STOP = "gps_stop";

    private HandlerThread handlerThread;
    private Handler worker;
    public static final int SEND_LOCATIONS_TO_SERVER = 1;
    GpsManager gpsManager;
    GpsLocationListener gpsLocationListener;
    PowerManager.WakeLock wakeLock = null;
    Long minTime = 5*1000L;
    Float minDistance = 1f;
    int activityId = 1;
    int notifyId = 7;
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        gpsManager = new GpsManager(this);
        handlerThread = new HandlerThread("network");
        handlerThread.start();
        worker = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case SEND_LOCATIONS_TO_SERVER:
                        try {
                            Location location = (Location) msg.obj;
                            String reponese = callApi(location, activityId);
                            Log.d(TAG, reponese);
                        }catch (Exception e){
                            Log.e(TAG,e.toString());
                        }
                        break;
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null)
            return Service.START_STICKY;
        Bundle bd = intent.getExtras();
        if(bd != null){
            String cmd = bd.getString(BUNDLE_KEY_CMD);
            Log.d(TAG," onStartCommand : "+bd.toString());
            if(cmd.equals(GPS_START)){
                minTime = bd.getLong(BUNDLE_KEY_MINTIME);
                minDistance = bd.getFloat(BUNDLE_KEY_MINDISTANCE);
                activityId = bd.getInt(BUNDLE_KEY_ACTIVITYID);

                if(gpsLocationListener == null)
                    gpsLocationListener = new GpsLocationListener();
                gpsManager.requestLocationUpdates(this,minTime, minDistance, gpsLocationListener);
                acquireWakeLock();
                AndroidUtil.showNotifycation(this,notifyId,"GPS開始追蹤中...");
            }else if(cmd.equals(GPS_STOP)){
                 if(gpsLocationListener != null)
                   gpsManager.removeUpdates(this,gpsLocationListener);
                gpsLocationListener = null;
                releaseWakeLock();
                AndroidUtil.cancelNotifycation(this,notifyId);
            }
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        if(handlerThread != null){
            handlerThread.quit();
            handlerThread.interrupt();
        }
        if(worker != null){
            worker.removeCallbacksAndMessages(null);
        }
        releaseWakeLock();
    }

    private  class GpsLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            getLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }
    private void getLocation(final Location location) {	//將定位資訊顯示在畫面中
        if(location != null) {
            Log.d(TAG, location.toString());
            worker.obtainMessage(SEND_LOCATIONS_TO_SERVER, location).sendToTarget();
        }
        else {
            Log.e(TAG, "無法定位座標!!");
        }
    }

    private String callApi(Location data ,int activityID){
        try {
            String urlStr = String.format(SystemConstant.API_URL, activityID);
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.connect();
            StringBuilder dataStr = new StringBuilder();
            dataStr.append("p[0][id]=").append("-1").append("&")
                    .append("p[0][a]=").append(data.getLatitude()).append("&")
                    .append("p[0][n]=").append(data.getLongitude()).append("&")
                    .append("p[0][h]=").append(data.getAccuracy()).append("&")
                    .append("p[0][v]=").append("-1").append("&")
                    .append("p[0][l]=").append("-1").append("&")
                    .append("p[0][s]=").append(data.getSpeed()).append("&")
                    .append("p[0][t]=").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(data.getTime()))).append("&")
                    .append("p[0][i]=").append("0");
            Log.d("gpshttp", dataStr.toString());
            OutputStream out = connection.getOutputStream();
            out.write(dataStr.toString().getBytes());
            out.flush();
            out.close();
            if(connection.getResponseCode() == 200){
                InputStream is = connection.getInputStream();
                String msg = AndroidUtil.getStringFromInputStream(is);
                //通知UI更新畫面
                Intent it = new Intent();
                it.setAction(SystemConstant.BROADCAST_LOCATION_UPDATE);
                SystemConstant.list.add(0, data);
                LocalBroadcastManager.getInstance(this).sendBroadcast(it);
                return msg;
            }else{
                return connection.getResponseCode()+" : "+ connection.getResponseMessage();
            }
        } catch (Exception e) {
            return e.toString();
        }
    }

    //在螢幕關掉時,維持cpu運作
    private void acquireWakeLock(){
        if (wakeLock == null){
            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "GpsService");
            if (null != wakeLock){
                wakeLock.acquire();
            }
        }
    }
    //釋放cpu
    private void releaseWakeLock(){
        if (wakeLock != null){
            wakeLock.release();
            wakeLock = null;
        }
    }
}

package andy.gpssender;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "GPSsender";
    public static final int REQUEST_COARSE_LOCATION = 1;
    //non_UI_HANDLER
    public static final int SEND_LOCATIONS_TO_SERVER = 1;
    //UI_HANDLER
    public static final int REFRESH_LISTVIEW = 1;
    public static final int SHOW_API_ERRMSG = 2;
    private Handler uiHandler;

    private Switch mSwitch;
    private SeekBar seekBar;
    private TextView seekBarMsg;
    private EditText editText;
    private long minTime = 5 * 1000L;
    private long minDistance = 1L;

    private ListView listView;
    private LocationListAdapter locationListAdapter;
    private GpsManager gpsManager;
    private GpsBroadReciver gpsBroadReciver = new GpsBroadReciver();;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSwitch = (Switch) findViewById(R.id.switch1);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBarMsg = (TextView) findViewById(R.id.textView);
        editText = (EditText) findViewById(R.id.editText);
        listView = (ListView) findViewById(R.id.listView);

        locationListAdapter = new LocationListAdapter(this);
        listView.setAdapter(locationListAdapter);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarMsg.setText(Integer.toString(progress + 5));
                minTime = (progress + 5) * 1000L;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        uiHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case REFRESH_LISTVIEW:
                        locationListAdapter.setDatas(SystemConstant.list);
                        locationListAdapter.notifyDataSetChanged();
                        break;
                    case SHOW_API_ERRMSG:
                        Toast.makeText(MainActivity.this,(String)msg.obj,Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };

        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleSwitch();
            }
        });

        gpsManager = new GpsManager(this);

        if (!getLastKnownLocationIfAllowed())
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COARSE_LOCATION);
        else
            gpsManager.openGPS();

    }

    @Override
    public void onStart() {
        super.onStart();

        gpsBroadReciver = new GpsBroadReciver();
        IntentFilter intentFilter = new IntentFilter(SystemConstant.BROADCAST_LOCATION_UPDATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(gpsBroadReciver, intentFilter);
        uiHandler.sendEmptyMessage(REFRESH_LISTVIEW);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleUIState();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(gpsBroadReciver!=null)
            LocalBroadcastManager.getInstance(this).unregisterReceiver(gpsBroadReciver);

    }

    @Override
    protected void onDestroy() {
        if ( uiHandler != null) uiHandler.removeCallbacksAndMessages(null);
        super.onDestroy();

    }

    private void handleUIState(){
        if (mSwitch.isChecked()) {
            //開啟追蹤處理
            editText.setEnabled(false);
            seekBar.setEnabled(false);
        } else {
            //關閉追蹤處理
            editText.setEnabled(true);
            seekBar.setEnabled(true);
        }
    }

    private void handleSwitch() {
        handleUIState();
        if (mSwitch.isChecked()) {
            //開啟追蹤處理
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Bundle bd = new Bundle();
            bd.putInt(GpsService.BUNDLE_KEY_ACTIVITYID,Integer.valueOf(editText.getText().toString()));
            bd.putLong(GpsService.BUNDLE_KEY_MINTIME, minTime);
            bd.putFloat(GpsService.BUNDLE_KEY_MINDISTANCE, minDistance);
            AndroidUtil.sendToGPSSrvice(this, bd, GpsService.GPS_START);

        } else {
            //關閉追蹤處理
            AndroidUtil.sendToGPSSrvice(this, null , GpsService.GPS_STOP);

        }
    }

    private boolean getLastKnownLocationIfAllowed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //noinspection ResourceType
                gpsManager.openGPS();
            } else {
                //Permission denied
            }
            break;
        }
    }


  private class GpsBroadReciver extends BroadcastReceiver{
      @Override
      public void onReceive(Context context, Intent intent) {
          uiHandler.sendEmptyMessage(REFRESH_LISTVIEW);
      }
  }


    private class LocationListAdapter extends BaseAdapter{

        private LayoutInflater inflater;
        private List<Location> datas;
        public LocationListAdapter(Context context){
            inflater = LayoutInflater.from(context);
        }
        public void setDatas(List<Location> datas){
            this.datas = datas;
        }
        @Override
        public int getCount() {
            if(datas != null)
                return datas.size();
            return 0;
        }

        @Override
        public Location getItem(int position) {
            if(datas != null)
                return datas.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
             TextView textView;
             if(convertView == null){
                 convertView = inflater.inflate(android.R.layout.simple_list_item_1, null);
                 textView = (TextView) convertView.findViewById(android.R.id.text1);
                 convertView.setTag(textView);
             }else{
                 textView = (TextView) convertView.getTag();
             }
            Location location = getItem(position);
             if(location != null){
                 StringBuilder dataStr = new StringBuilder();
                 dataStr.append("p[0][id]=").append("-1").append("\n")
                         .append("p[0][a]=").append(location.getLatitude()).append("\n")
                         .append("p[0][n]=").append(location.getLongitude()).append("\n")
                         .append("p[0][h]=").append(location.getAccuracy()).append("\n")
                         .append("p[0][v]=").append("-1").append("\n")
                         .append("p[0][l]=").append("-1").append("\n")
                         .append("p[0][s]=").append(location.getSpeed()).append("\n")
                         .append("p[0][t]=").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(location.getTime()))).append("\n");
                 textView.setText(dataStr.toString());
             }
            return convertView;
        }
    }
}

package andy.gpssender;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by andyli on 2016/3/28.
 */
public class AndroidUtil {

    public static String getStringFromInputStream(InputStream is)
            throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len = -1;

        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        is.close();
        String state = os.toString();
        os.close();
        return state;
    }

    public static void sendToGPSSrvice(Context context,Bundle bd,String cmd){
        try{
            Intent it = new Intent(context,GpsService.class);
            if(bd!=null){
                bd.putString(GpsService.BUNDLE_KEY_CMD, cmd);
                it.putExtras(bd);
            }else {
                it.putExtra(GpsService.BUNDLE_KEY_CMD, cmd);
            }
            context.startService(it);
        }catch (Exception e){
           Log.e("AndroidUtil", "sendToGPSSrvice " + e.toString());
        }
    }

    public static void showNotifycation(Context context, int notifyID, String msg){
        NotificationManager notifManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent intent = new Intent(context,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification mNotifyBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("ＧpsSender")
                .setContentText(msg)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setContentIntent(pendingIntent)
                .build();
        mNotifyBuilder.flags |= Notification.FLAG_NO_CLEAR;
        notifManager.notify(notifyID,mNotifyBuilder);
    }

    public static void cancelNotifycation(Context context, int notifyID){
        NotificationManager notifManager =  (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancel(notifyID);
    }
}

package andy.gpssender;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
}

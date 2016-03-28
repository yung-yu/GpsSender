package andy.gpssender;

import android.location.Location;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andyli on 2016/3/27.
 */
public class SystemConstant {

    public static String API_URL = "http://mazu.ioa.tw/api/march/%d/paths";

    public static final String BROADCAST_LOCATION_UPDATE = "BROADCAST_LOCATION_UPDATE";

    public static List<Location> list = new ArrayList<Location>();

}

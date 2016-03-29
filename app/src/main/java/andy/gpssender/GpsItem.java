package andy.gpssender;

import android.location.Location;

import java.util.Locale;

/**
 * Created by AndyLi on 2016/3/29.
 */
public class GpsItem {
	Location location;
	int bettery = -1;

	public GpsItem(Location location, int bettery) {
		this.location = location;
		this.bettery = bettery;
	}
}

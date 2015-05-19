package org.musicpd.android;

import org.musicpd.android.tools.Log;

import com.caverock.androidsvg.SVGImageView;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

public class TipsActivity extends Activity {

	public static final long tips_time = new java.util.GregorianCalendar(2015, 5, 19).getTimeInMillis();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        SVGImageView svgImageView = new SVGImageView(this);
        svgImageView.setImageAsset("tips.svg");
        layout.addView(svgImageView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(layout);

        layout.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				TipsActivity.this.finish();
				return false;
			}
        });
	}

	static boolean shown = false;
    public static void consider(Activity activity, MPDApplication app) {
    	if (shown)
    		return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(app);
		if (prefs.getLong("tips", 0) < tips_time
			&& !app.isTabletUiEnabled()
			//&& !isTablet
			//&& !isDualPaneMode
			&& activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			prefs
				.edit()
				.putLong("tips", tips_time) // consider using System.currentTimeMillis()
				.apply();
			shown = true;
			activity.startActivity(new Intent(activity, TipsActivity.class));
		}
    }
}

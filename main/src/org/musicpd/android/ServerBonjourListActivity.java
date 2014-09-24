package org.musicpd.android;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import org.musicpd.android.MPDActivities.MPDListActivity;
import org.musicpd.android.helpers.MPDAsyncHelper;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.SettingsHelper;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;

public class ServerBonjourListActivity extends MPDListActivity {

	private BaseAdapter listAdapter = null;
	ServerDiscovery serverDiscovery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);

		try {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.permitAll()
				.build());
		} catch (Throwable t) {
			Log.w(t);
		}

		serverDiscovery = ((MPDApplication) getApplicationContext()).serverDiscovery;

		listAdapter = new ArrayAdapter<ServerInfo>(this, android.R.layout.simple_list_item_1, android.R.id.text1, serverDiscovery.servers);
		getListView().setAdapter(listAdapter);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		setTitle(R.string.servers);

		refresh();
    }

    void refresh() {
    	Log.i("Refreshing Players view");
    	listAdapter.notifyDataSetChanged();
    	getListView().postDelayed(new Runnable() {
    		public void run() {
    			refresh();
    		}
    	}, 1000);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.mpd_servermenu, menu);
		return true;
	}
	
	public static final int SETTINGS = 5;

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent i = null;

		// Handle item selection
		switch (item.getItemId()) {
			case R.id.GMM_Settings:
				i = new Intent(this, WifiConnectionSettings.class);
				startActivityForResult(i, SETTINGS);
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onListItemClick (ListView l, View v, int position, long id) {
		serverDiscovery.choose(position);
		finish();
	}

}

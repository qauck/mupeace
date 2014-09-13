package org.musicpd.android;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.a0z.mpd.MPD;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.musicpd.android.helpers.MPDAsyncHelper;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.SettingsHelper;
import org.xbill.DNS.Message;
import org.xbill.mDNS.Browse;
import org.xbill.mDNS.DNSSDListener;
import org.xbill.mDNS.MulticastDNSService;
import org.xbill.mDNS.ServiceInstance;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class ServerBonjourListActivity extends SherlockListActivity {
	
	private static final String SERVER_NAME = "server_name";
	private static final String SERVER_IP = "server_ip";
	private static final String SERVER_PORT = "server_port";
	
	//The multicast lock we'll have to release
	private WifiManager.MulticastLock multicastLock = null;
	MulticastDNSService mdns = null;
	Object serviceDiscovery;
	private List<Map<String,String>> servers = null;
	private SimpleAdapter listAdapter = null;
	SettingsHelper settings;
	MPDAsyncHelper oMPDAsyncHelper;
  
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

    	final MPDApplication app = (MPDApplication) getApplicationContext();
    	settings = new SettingsHelper(app, oMPDAsyncHelper = app.oMPDAsyncHelper);
    	
		servers = new ArrayList<Map<String,String>>() {
			public boolean add(Map<String,String> mt) {
		        int index = Collections.binarySearch(this, mt, new java.util.Comparator<Map<String,String>>() {
					public int compare(Map<String, String> lhs, Map<String, String> rhs) {
						return lhs.get(SERVER_NAME).compareTo(rhs.get(SERVER_NAME));
					}
		        });
		        if (index < 0) index = ~index;
		        super.add(index, mt);
		        return true;
		    }
		};

    	//By default, the android wifi stack will ignore broadcasts, fix that
    	WifiManager wm = (WifiManager)getSystemService(Context.WIFI_SERVICE);
    	multicastLock = wm.createMulticastLock("mupeace_bonjour");

    	try {
            mdns = new MulticastDNSService();
            serviceDiscovery = mdns.startServiceDiscovery(new Browse("_mpd._tcp.local."), new DNSSDListener() {
	    		public void serviceDiscovered(Object id, ServiceInstance info) {
	    			Log.i("Service Discovered: " + info);
					String address = chooseAddress(info.getAddresses());
					Log.i("Address:   " + address);
					if(address != null) {
						final Map<String, String> server = new HashMap<String, String>();
						server.put(SERVER_NAME, info.getName().getInstance());
						server.put(SERVER_IP, address);
						server.put(SERVER_PORT, Integer.toString(info.getPort()));
						servers.add(server);
					}
					runOnUiThread(new Runnable() {
						public void run() {
							listAdapter.notifyDataSetChanged();
						}
					});
	    		}
	
	    		public void serviceRemoved(Object id, ServiceInstance info) {
	    			Log.i("Service Removed: " + info);
	    			String name = info.getName().getInstance();
	    			Iterator<Map<String, String>> i = servers.iterator();
	    			while (i.hasNext())
	    				if (i.next().get(SERVER_NAME).equals(name))
	    					i.remove();
					runOnUiThread(new Runnable() {
						public void run() {
							listAdapter.notifyDataSetChanged();
						}
					});
	    		}
	
	    		public void handleException(Object id, Exception e) {
	    			Log.e(e);
	    		}
	
				public void receiveMessage(Object arg0, Message arg1) {
				}    		
	    	});
    	} catch(IOException e) {
    		Log.w(e);
    	}

		listAdapter = new SimpleAdapter(this, servers, android.R.layout.simple_list_item_1, new String[]{SERVER_NAME}, new int[]{android.R.id.text1});
		getListView().setAdapter(listAdapter);

		final ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		setTitle(R.string.servers);
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
    protected void onPause() {
		super.onPause();
    	if (multicastLock != null)
        	multicastLock.release();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	if (multicastLock != null)
    		//Ask android to allow us to get WiFi broadcasts
    		multicastLock.acquire();
    }
    
    @Override
    protected void onDestroy() {
    	try {
    		mdns.stopServiceDiscovery(serviceDiscovery);
    		mdns.close();
			mdns = null;
		} catch (Exception e) {
		}
		
		super.onDestroy();
    }
    
    @Override
    protected void onListItemClick (ListView l, View v, int position, long id) {
    	settings.setHostname(
			servers
			.get(position)
			.get(SERVER_IP)
		);
    	oMPDAsyncHelper.disconnect();
    	finish();
    }
    
    String chooseAddress(InetAddress[] addresses) {
		Log.i("Addresses: "+TextUtils.join(", ", addresses));
    	for (InetAddress address : addresses) {
    		if (Inet4Address.class.isInstance(address) && !address.isMulticastAddress())
    			return address.getHostAddress();
    	}
    	for (InetAddress address : addresses) {
			return address.getHostAddress();
    	}
    	return null;
    }
}

package org.musicpd.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.musicpd.android.tools.Log;
import org.xbill.DNS.Message;
import org.xbill.mDNS.Browse;
import org.xbill.mDNS.DNSSDListener;
import org.xbill.mDNS.MulticastDNSService;
import org.xbill.mDNS.ServiceInstance;

import android.content.Context;
import android.net.wifi.WifiManager;

public class ServerDiscovery {
	//The multicast lock we'll have to release
	private WifiManager.MulticastLock multicastLock;
	MulticastDNSService mdns;
	Object serviceDiscovery;
	public final List<ServerInfo> servers = new java.util.concurrent.CopyOnWriteArrayList<ServerInfo>() {
		public boolean add(ServerInfo info) {
			int index = Collections.binarySearch(this, info, ServerInfo.byName);
			if (index < 0) index = ~index;
			super.add(index, info);
			return true;
		}
	};

	public ServerDiscovery(Context context) {
		try {
			//By default, the android wifi stack will ignore broadcasts, fix that
			WifiManager wm = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			multicastLock = wm.createMulticastLock("mupeace_bonjour");
		} catch(Exception e) {
			Log.w(e);
		}

		try {
			mdns = new MulticastDNSService();
			serviceDiscovery = mdns.startServiceDiscovery(new Browse("_mpd._tcp.local."), new DNSSDListener() {
				public void serviceDiscovered(Object id, ServiceInstance info) {
					Log.i("Service Discovered: " + info);
					ServerInfo server = new ServerInfo(info);
					if (server.address != null)
						servers.add(server);
				}

				public void serviceRemoved(Object id, ServiceInstance info) {
					Log.i("Service Removed: " + info);
					String name = info.getName().getInstance();
					Iterator<ServerInfo> i = servers.iterator();
					while (i.hasNext())
						if (i.next().name.equals(name))
							i.remove();
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
	}

	public void onPause() {
		if (multicastLock != null)
			multicastLock.release();
	}

	public void onResume() {
		if (multicastLock != null)
			// Ask android to allow us to get WiFi broadcasts
			multicastLock.acquire();
	}

	public void onDestroy() {
		try {
			mdns.stopServiceDiscovery(serviceDiscovery);
			mdns.close();
			mdns = null;
		} catch (Exception e) {
			Log.w(e);
		}
	}
}

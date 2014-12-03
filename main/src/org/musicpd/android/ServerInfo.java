package org.musicpd.android;

import java.net.Inet4Address;
import java.net.InetAddress;

import org.musicpd.android.tools.Log;
import org.xbill.mDNS.ServiceInstance;

import android.text.TextUtils;

public class ServerInfo {
	public final String name;
	public final String address;
	public final int port;

	public ServerInfo(ServiceInstance info) {
		this.name = info.getName().getInstance()
			// TODO: replace common chars hack with full unescape
			.replace("\\32", " ")
			.replaceAll("\\\\([^\\\\])", "$1")
			.intern();
		this.address = chooseAddress(info.getAddresses());
		this.port = info.getPort();
	}

	public static String chooseAddress(InetAddress[] addresses) {
		Log.i("Addresses: "+TextUtils.join(", ", addresses));
		for (InetAddress address : addresses) {
			if (Inet4Address.class.isInstance(address) && !address.isMulticastAddress())
				return address.getHostAddress();
		}
		return null;
	}

	public static final java.util.Comparator<ServerInfo> byName = new java.util.Comparator<ServerInfo>() {
		public int compare(ServerInfo lhs, ServerInfo rhs) {
			return lhs.name.compareTo(rhs.name);
		}
	};

	@Override
	public String toString() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ServerInfo && ((ServerInfo) obj).name == name;
	}

	@Override
	public int hashCode() {
		return name == null? 0 : name.hashCode();
	}
}

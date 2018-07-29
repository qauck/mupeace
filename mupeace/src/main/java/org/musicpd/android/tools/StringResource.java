package org.musicpd.android.tools;

import android.content.Context;

public final class StringResource {
	String string;
	final int resource;

	public StringResource(String string) {
		this.string = string;
		this.resource = 0;
	}

	public StringResource(int resource) {
		this.resource = resource;
	}

	public String getString(Context context) {
		if (string == null)
			string = context.getString(resource);
		return string;
	}

	@Override
	public String toString() {
		return string == null ? "resid:" + resource : string;
	}
}

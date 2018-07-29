package org.musicpd.android.tools;

import org.a0z.mpd.MPD;
import org.a0z.mpd.Music;

public class RelatedSongs {

	protected static final String dir = "^(.*)/(?:\\\\/|[^/]*)$";

	public static java.util.List<Music> items(MPD mpd, java.util.List<Music> xs) {

		return xs;
	}
}

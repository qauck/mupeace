package org.musicpd.android.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.musicpd.android.R;
import org.musicpd.android.fragments.AlbumsFragment;
import org.musicpd.android.fragments.AlbumsGridFragment;
import org.musicpd.android.fragments.ArtistsFragment;
import org.musicpd.android.fragments.FSFragment;
import org.musicpd.android.fragments.GenresFragment;
import org.musicpd.android.fragments.PlaylistsFragment;
import org.musicpd.android.fragments.StreamsFragment;

public class LibraryTabsUtil {

	public static final String TAB_ARTISTS = "artists";
	public static final String TAB_ALBUMS = "albums";
	public static final String TAB_PLAYLISTS = "playlists";
	public static final String TAB_STREAMS = "streams";
	public static final String TAB_FILES = "files";
	public static final String TAB_GENRES = "genres";

	private static final String LIBRARY_TABS_SETTINGS_KEY = "currentLibraryTabs";

	private static final String LIBRARY_TABS_DELIMITER = "|";

	private static String DEFAULT_LIBRARY_TABS = TAB_ARTISTS
			+ LIBRARY_TABS_DELIMITER + TAB_FILES
			+ LIBRARY_TABS_DELIMITER + TAB_PLAYLISTS
			+ LIBRARY_TABS_DELIMITER + TAB_STREAMS
			+ LIBRARY_TABS_DELIMITER + TAB_GENRES
			+ LIBRARY_TABS_DELIMITER + TAB_ALBUMS;

	public static ArrayList<String> getAllLibraryTabs() {
		String CurrentSettings = DEFAULT_LIBRARY_TABS;
		return new ArrayList<String>(Arrays.asList(CurrentSettings.split("\\"
				+ LIBRARY_TABS_DELIMITER)));
	}

	public static ArrayList<String> getCurrentLibraryTabs(Context context) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String currentSettings = settings.getString(LIBRARY_TABS_SETTINGS_KEY, "");
		if (currentSettings == "") {
			currentSettings = DEFAULT_LIBRARY_TABS;
			settings.edit().putString(LIBRARY_TABS_SETTINGS_KEY, currentSettings)
					.commit();
		}
		return new ArrayList<String>(Arrays.asList(currentSettings.split("\\"
				+ LIBRARY_TABS_DELIMITER)));
	}

	public static void saveCurrentLibraryTabs(Context context,
			ArrayList<String> tabs) {
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String currentSettings = getTabsStringFromList(tabs);
		settings.edit().putString(LIBRARY_TABS_SETTINGS_KEY, currentSettings).commit();
	}

	public static ArrayList<String> getTabsListFromString(String tabs) {
		return new ArrayList<String>(Arrays.asList(tabs.split("\\"
				+ LIBRARY_TABS_DELIMITER)));
	}

	public static String getTabsStringFromList(ArrayList<String> tabs) {
		if (tabs == null || tabs.size() <= 0) {
			return "";
		} else {
			String s = tabs.get(0);
			for (int i = 1; i < tabs.size(); i++) {
				s += LIBRARY_TABS_DELIMITER + tabs.get(i);
			}
			return s;
		}
	}

	public static int getTabTitleResId(String tab) {
		switch(tab.intern()) {
			case LibraryTabsUtil.TAB_ARTISTS:
				return R.string.artists;
			case LibraryTabsUtil.TAB_ALBUMS:
				return R.string.albums;
			case LibraryTabsUtil.TAB_PLAYLISTS:
				return R.string.playlists;
			case LibraryTabsUtil.TAB_STREAMS:
				return R.string.streams;
			case LibraryTabsUtil.TAB_FILES:
				return R.string.files;
			case LibraryTabsUtil.TAB_GENRES:
				return R.string.genres;
			default:
				return R.string.artists;
		}
	}

	public static final String PREFERENCE_ALBUM_LIBRARY = "enableAlbumArtLibrary";

	public static Class<? extends Object> getClass(Context context, String tab) {
		switch(tab.intern()) {
			case LibraryTabsUtil.TAB_ARTISTS:
				return ArtistsFragment.class;
			case LibraryTabsUtil.TAB_ALBUMS:
				final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				return settings.getBoolean(PREFERENCE_ALBUM_LIBRARY, false)
					? AlbumsGridFragment.class
					: AlbumsFragment.class
				;
			case LibraryTabsUtil.TAB_PLAYLISTS:
				return PlaylistsFragment.class;
			case LibraryTabsUtil.TAB_STREAMS:
				return StreamsFragment.class;
			case LibraryTabsUtil.TAB_FILES:
				return FSFragment.class;
			case LibraryTabsUtil.TAB_GENRES:
				return GenresFragment.class;
			default:
				return ArtistsFragment.class;
		}
	}
}
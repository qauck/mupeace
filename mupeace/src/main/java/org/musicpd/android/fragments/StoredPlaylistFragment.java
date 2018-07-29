package org.musicpd.android.fragments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.Music;
import org.a0z.mpd.exception.MPDServerException;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import org.musicpd.android.R;
import org.musicpd.android.library.LibraryTabActivity;
import org.musicpd.android.library.PlaylistEditActivity;
import org.musicpd.android.tools.StringResource;

public class StoredPlaylistFragment extends BrowseFragment {
	private static final String EXTRA_PLAYLIST_NAME = "playlist";

	private ArrayList<HashMap<String, Object>> songlist = new ArrayList<HashMap<String, Object>>();

	private String playlistName;

	public StoredPlaylistFragment() {
		super(R.string.addSong, R.string.songAdded, MPDCommand.MPD_SEARCH_TITLE);
		setHasOptionsMenu(true);
	}
	
	public StringResource getTitle() {
		if (playlistName == null)
			return new StringResource(R.string.playlist);
		else
			return new StringResource(playlistName);
	}

	public StoredPlaylistFragment init(String name) {
		playlistName = name;
		return this;
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null)
			init(icicle.getString(EXTRA_PLAYLIST_NAME));
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString(EXTRA_PLAYLIST_NAME, playlistName);
		super.onSaveInstanceState(outState);
	}

	@Override
	public String toString() {
		if (playlistName != null) {
			return playlistName;
		} else {
			return getString(R.string.playlist);
		}
	}

	@Override
	protected void asyncUpdate() {
		try {
			List<Music> musics = app.oMPDAsyncHelper.oMPD.getPlaylistSongs(playlistName);
			songlist.clear();
			for (Music m : musics) {
				if (m == null) {
					continue;
				}
				HashMap<String, Object> item = new HashMap<String, Object>();
				item.put("songid", m.getSongId());
				item.put("artist", m.getArtist());
				item.put("title", m.getTitle());
				item.put("play", 0);
				songlist.add(item);
			}
			items = musics;
		} catch (MPDServerException e) {
		}
	}

	@Override
	protected ListAdapter getCustomListAdapter() {
		return new SimpleAdapter(getActivity(), songlist, R.layout.playlist_list_item,
				new String[] { "play", "title", "artist" }, new int[] { R.id.picture, android.R.id.text1, android.R.id.text2 });
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		android.view.MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.mpd_browsermenu, menu);

		//AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		// arrayListId = info.position;
		menu.setHeaderTitle(playlistName);
	}

	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		return false;
	}

	/*
	 * Create Menu for Playlist View
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.mpd_storedplaylistmenu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Menu actions...
		Intent i;
		switch (item.getItemId()) {
		case R.id.PLM_EditPL:
			i = new Intent(getActivity(), PlaylistEditActivity.class);
			i.putExtra("playlist", playlistName);
			startActivity(i);
			return true;
		case R.id.GMM_LibTab:
			i = new Intent(getActivity(), LibraryTabActivity.class);
			startActivity(i);
		default:
			return false;
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
	}

	@Override
	protected void add(Item item, String playlist) {
	}
}
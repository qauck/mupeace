package org.musicpd.android.fragments;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;
import org.a0z.mpd.MPDCommand;
import org.a0z.mpd.exception.MPDServerException;

import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView;

import org.musicpd.android.R;
import org.musicpd.android.adapters.ArrayIndexerAdapter;
import org.musicpd.android.library.ILibraryFragmentActivity;
import org.musicpd.android.tools.AlbumGroup;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.StringResource;
import org.musicpd.android.tools.Tools;
import org.musicpd.android.views.AlbumDataBinder;

public class AlbumsFragment extends BrowseFragment {
	private static final String EXTRA_GENRE = "genre";
	private static final String EXTRA_ARTIST = "artist";
	protected Genre genre = null;
	protected Artist artist = null;

	public AlbumsFragment() {
		super(R.string.addAlbum, R.string.albumAdded, MPDCommand.MPD_SEARCH_ALBUM);
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (icicle != null)
			init((Genre) icicle.getParcelable(EXTRA_GENRE), (Artist) icicle.getParcelable(EXTRA_ARTIST));
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingAlbums;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putParcelable(EXTRA_GENRE, genre);
		outState.putParcelable(EXTRA_ARTIST, artist);
		super.onSaveInstanceState(outState);
	}

	public AlbumsFragment init(Genre g, Artist a) {
		genre = g;
		artist = a;
		return this;
	}

	@Override
	public StringResource getTitle() {
		if (artist != null) {
			return new StringResource(artist.getName());
		} else {
			return new StringResource(R.string.albums);
		}
	}

	@Override
	public void onItemClick(AdapterView adapterView, View v, int position, long id) {
		Album album = (Album)lookup(position);
		if (album instanceof AlbumGroup)
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new AlbumGroupFragment().init(genre, artist, (AlbumGroup)album),
				"albumgroup");
		else
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(new SongsFragment().init(genre, artist, album),
				"songs");
	}
	
	@Override
	protected ListAdapter getCustomListAdapter() {
		if(items != null) {
			return new ArrayIndexerAdapter(getActivity(),
					new AlbumDataBinder(app, artist == null ? null : artist.getName(), app.isLightThemeSelected()), items);
		}
		return super.getCustomListAdapter();
	}

	protected static final java.util.HashMap<Artist, java.util.List<Album>> albumCache = new java.util.HashMap<Artist, java.util.List<Album>>();
	protected static final Artist none = new Artist("", 0);
	
	@Override
	protected void asyncUpdate() {
		if (artist == null)
			Log.w("Listing all albums");
		try {
			if ((items = albumCache.get(artist == null? none : artist)) == null) {
				java.util.List<Album> albums = app.oMPDAsyncHelper.oMPD.getAlbums(genre, artist);
				if (artist != null && albums != null)
					albums = AlbumGroup.items(albums, albums.size() > 200);
				albumCache.put(artist, albums);
				items = albums;
			}
		} catch (MPDServerException e) {
			Log.w(e);
		} catch (Exception e) {
			Log.e(e);
		}
	}

	@Override
	protected String[] info(Item item) {
		return artist == null
			? new String[] { "album", item.getName() }
			: genre == null
			? new String[] { "artist", artist.getName(), "album", item.getName() }
			: new String[] { "genre", genre.getName(), "artist", artist.getName(), "album", item.getName() }
		;
	}

	@Override
	protected void add(Item item, boolean replace, boolean play) {
		try {
			app.oMPDAsyncHelper.oMPD.add(genre, artist, (Album) item, replace, play);
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (Exception e) {
			Log.w(e);
		}
	}

	@Override
	protected void add(Item item, String playlist) {
		try {
			app.oMPDAsyncHelper.oMPD.addToPlaylist(playlist, genre, artist, ((Album) item));
			Tools.notifyUser(String.format(getResources().getString(irAdded), item), getActivity());
		} catch (Exception e) {
			Log.w(e);
		}
	}
}

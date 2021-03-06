package org.musicpd.android.fragments;

import org.a0z.mpd.Artist;
import org.a0z.mpd.Genre;
import org.a0z.mpd.Item;

import org.musicpd.android.R;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.AlbumGroup;

import android.os.Bundle;

public class AlbumGroupFragment extends AlbumsFragment {
	private static final String EXTRA_ALBUMGROUP = "albumgroup";
	protected AlbumGroup group = null;

	public AlbumGroupFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {
		try {
			super.onCreate(icicle);
			if (icicle != null)
				init((AlbumGroup) icicle.getSerializable(EXTRA_ALBUMGROUP));
		} catch(Exception e) {
			Log.e(e);
			getActivity().getSupportFragmentManager().popBackStack();
		}
	}

	@Override
	public int getLoadingText() {
		return R.string.loadingAlbums;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putSerializable(EXTRA_ALBUMGROUP, group);
		super.onSaveInstanceState(outState);
	}

	public AlbumGroupFragment init(Genre G, Artist a, AlbumGroup g) {
		super.init(G, a);
		return init(g);
	}

	protected AlbumGroupFragment init(AlbumGroup g) {
		group = g;
		return this;
	}

	@Override
	protected Item lookup(int position)
	{
		return group == null ? null : group.get(position);
	}
	
	@Override
	protected void asyncUpdate() {
		if (group != null)
			items = group.albums();
	}
}

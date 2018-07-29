package org.musicpd.android.fragments;

import org.musicpd.android.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainPageFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_page_tablet, container, false);
        
        nowPlaying = (NowPlayingFragment)this.getChildFragmentManager().findFragmentById(R.id.nowplaying_fragment);
        playlist = (PlaylistFragment)this.getChildFragmentManager().findFragmentById(R.id.playlist_fragment);
        //this.getActivity().getSupportFragmentManager().findFragmentById(R.id.mainImagesList)

        return view;
    }

    public NowPlayingFragment nowPlaying;
    public PlaylistFragment playlist;
}
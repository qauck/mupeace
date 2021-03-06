package org.musicpd.android.fragments;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.a0z.mpd.Album;
import org.a0z.mpd.Artist;
import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.ClientActionListener;
import org.a0z.mpd.event.StatusChangeListener;
import org.a0z.mpd.event.TrackPositionListener;
import org.a0z.mpd.exception.MPDServerException;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.ListPopupWindow;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.musicpd.android.InformationActivity;
import org.musicpd.android.MPDApplication;
import org.musicpd.android.R;
import org.musicpd.android.StreamingService;
import org.musicpd.android.TipsActivity;
import org.musicpd.android.adapters.PopupMenuAdapter;
import org.musicpd.android.adapters.PopupMenuItem;
import org.musicpd.android.cover.CoverBitmapDrawable;
import org.musicpd.android.helpers.CoverAsyncHelper;
import org.musicpd.android.helpers.AlbumCoverDownloadListener;
import org.musicpd.android.helpers.MPDConnectionHandler;
import org.musicpd.android.library.ILibraryFragmentActivity;
import org.musicpd.android.library.SimpleLibraryActivity;
import org.musicpd.android.tools.Job;
import org.musicpd.android.tools.Log;

public class NowPlayingFragment extends Fragment implements ClientActionListener, StatusChangeListener, TrackPositionListener,
		OnSharedPreferenceChangeListener, OnItemClickListener {
	
	public static final String PREFS_NAME = "mupeace.properties";
	
	private static final int POPUP_ARTIST = 0;
	private static final int POPUP_ALBUM = 1;
	private static final int POPUP_FOLDER = 2;
	private static final int POPUP_STREAM = 3;
	private static final int POPUP_SHARE = 4;
	private static final int POPUP_INFO = 5;

	private TextView artistNameText;
	private TextView songNameText;
	private TextView albumNameText;
	private ImageButton shuffleButton=null;
	private ImageButton repeatButton=null;
	private ImageButton stopButton = null;
	private boolean shuffleCurrent=false;
	private boolean repeatCurrent=false;

	public static final int ALBUMS = 4;

	public static final int FILES = 3;

	private SeekBar progressBarVolume = null;
	private SeekBar progressBarTrack = null;

	private TextView trackTime = null;
	private TextView trackTotalTime = null;
	private TextView trackRemainingTime = null;

	private CoverAsyncHelper oCoverAsyncHelper = null;
	long lastSongTime = 0;
	long lastElapsedTime = 0;

	private AlbumCoverDownloadListener coverArtListener;
	private ImageView coverArt;
	private ProgressBar coverArtProgress;

	private ListPopupWindow popupMenu;

	public static final int VOLUME_STEP = 5;

	private static final int ANIMATION_DURATION_MSEC = 1000;

	private ButtonEventHandler buttonEventHandler;

	@SuppressWarnings("unused")
	private boolean streamingMode;
	private boolean connected;

	private Music currentSong = null;

	private Timer volTimer = new Timer();
	private TimerTask volTimerTask = null;
	private Handler handler;

	private Timer posTimer = null;
	private TimerTask posTimerTask = null;

	private String noSongInfo = "";

	View volume;
	View progress;
	long volume_show_time;
	boolean tablet;

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		default:
			break;
		}

	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		handler = new Handler();
		setHasOptionsMenu(false);
		getActivity().setTitle(getResources().getString(R.string.nowPlaying));
		getActivity().registerReceiver(MPDConnectionHandler.getInstance(), new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
	}

	@Override
	public void onStart() {
		super.onStart();
		noSongInfo = getResources().getString(R.string.noSongInfo);
		MPDApplication app = (MPDApplication) getActivity().getApplication();
		app.oMPDAsyncHelper.oMPD.addClientActionListener(this);
		app.oMPDAsyncHelper.addStatusChangeListener(this);
		app.oMPDAsyncHelper.addTrackPositionListener(this);
		app.setActivity(this);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public void onDestroyView() {
		if (coverArt != null)
			coverArt.setImageResource(R.drawable.no_cover_art);
		if (coverArtListener != null)
			coverArtListener.freeCoverDrawable();
		super.onDestroyView();
	}

	@Override
	public void onResume() {
		super.onResume();
		// Annoyingly this seems to be run when the app starts the first time to.
		// Just to make sure that we do actually get an update.
		try {
			updateTrackInfo();
		} catch (Exception e) {
			Log.w(e);
		}
		updateStatus(null);

		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		new Job() {
			@Override
			public void run() {
				try {
					final int volume = app.oMPDAsyncHelper.oMPD.getStatus().getVolume();
					handler.post(new Runnable() {
						@Override
						public void run() {
							progressBarVolume.setProgress(volume);
						}
					});
				} catch (MPDServerException e) {
					Log.w(e);
				}
			}
		};
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		View view = inflater.inflate((tablet = app.isTabletUiEnabled()) ? R.layout.main_fragment_tablet : R.layout.main_fragment, container, false);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.registerOnSharedPreferenceChangeListener(this);

		volume = view.findViewById(R.id.volume_layout);
		progress = view.findViewById(R.id.progress_layout);
		streamingMode = app.getApplicationState().streamingMode;
		connected = app.oMPDAsyncHelper.oMPD.isConnected();
		artistNameText = (TextView) view.findViewById(R.id.artistName);
		albumNameText = (TextView) view.findViewById(R.id.albumName);
		songNameText = (TextView) view.findViewById(R.id.songName);
		artistNameText.setSelected(true);
		albumNameText.setSelected(true);
		songNameText.setSelected(true);

		shuffleButton = (ImageButton) view.findViewById(R.id.shuffle);
		repeatButton = (ImageButton) view.findViewById(R.id.repeat);

		progressBarVolume = (SeekBar) view.findViewById(R.id.progress_volume);
		progressBarTrack = (SeekBar) view.findViewById(R.id.progress_track);

		trackTime = (TextView) view.findViewById(R.id.trackTime);
		trackTotalTime = (TextView) view.findViewById(R.id.trackTotalTime);
		trackRemainingTime = (TextView) view.findViewById(R.id.trackRemainingTime);

		Animation fadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
		fadeIn.setDuration(ANIMATION_DURATION_MSEC);
		Animation fadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		fadeOut.setDuration(ANIMATION_DURATION_MSEC);

		coverArt = (ImageView) view.findViewById(R.id.albumCover);
		coverArtProgress = (ProgressBar) view.findViewById(R.id.albumCoverProgress);

		oCoverAsyncHelper = new CoverAsyncHelper(app, settings);
		oCoverAsyncHelper.setCoverRetrieversFromPreferences();
		// Scale cover images down to screen width
		oCoverAsyncHelper.setCoverMaxSizeFromScreen(getActivity());
		oCoverAsyncHelper.setCachedCoverMaxSize(coverArt.getWidth());

		coverArtListener = new AlbumCoverDownloadListener(getActivity(), coverArt, coverArtProgress);
		oCoverAsyncHelper.addCoverDownloadListener(coverArtListener);

		buttonEventHandler = new ButtonEventHandler();
		ImageButton button = (ImageButton) view.findViewById(R.id.next);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.prev);
		button.setOnClickListener(buttonEventHandler);

		button = (ImageButton) view.findViewById(R.id.playpause);
		button.setOnClickListener(buttonEventHandler);
		button.setOnLongClickListener(buttonEventHandler);
		
		stopButton = (ImageButton) view.findViewById(R.id.stop);
		stopButton.setOnClickListener(buttonEventHandler);
		stopButton.setOnLongClickListener(buttonEventHandler);
		applyStopButtonVisibility(settings);

		if (null!=shuffleButton) {
			shuffleButton.setOnClickListener(buttonEventHandler);
		}
		if (null!=repeatButton) {
			repeatButton.setOnClickListener(buttonEventHandler);
		}

		final View songInfo = view.findViewById(R.id.songInfo);
		if(songInfo != null) {
			songInfo.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (currentSong == null)
						return;
					popupMenu = new ListPopupWindow(getActivity());
					popupMenu.setModal(true);
					popupMenu.setOnItemClickListener(NowPlayingFragment.this);
					PopupMenuItem items[];
					if (currentSong.isStream()) {
						items = new PopupMenuItem[2];
						items[0] = new PopupMenuItem(POPUP_STREAM, R.string.goToStream);
						items[1] = new PopupMenuItem(POPUP_SHARE, R.string.share);
					} else {
						items = new PopupMenuItem[5];
						items[0] = new PopupMenuItem(POPUP_ARTIST, R.string.goToAlbum);
						items[1] = new PopupMenuItem(POPUP_ALBUM, R.string.goToArtist);
						items[2] = new PopupMenuItem(POPUP_FOLDER, R.string.goToFolder);
						items[3] = new PopupMenuItem(POPUP_SHARE, R.string.share);
						items[4] = new PopupMenuItem(POPUP_INFO, R.string.information);
					}
					popupMenu.setAdapter(new PopupMenuAdapter(getActivity(),
							Build.VERSION.SDK_INT >= 14 ? android.R.layout.simple_spinner_dropdown_item
									: android.R.layout.simple_spinner_dropdown_item, items));
					popupMenu.setContentWidth((int) (v.getWidth() - v.getWidth() * 0.05));
					popupMenu.setAnchorView(v);
					popupMenu.show();
				}
			});
		}

		progressBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				volTimerTask = new TimerTask() {
					public void run() {
						final MPDApplication app = (MPDApplication) getActivity().getApplication();
						if (lastSentVol != progress.getProgress()) {
							lastSentVol = progress.getProgress();
							new Job() {
								@Override
								public void run() {
									try {
										app.oMPDAsyncHelper.oMPD.setVolume(lastSentVol);
									} catch (MPDServerException e) {
										Log.w(e);
									}
								}
							};
						}
					}

					int lastSentVol = -1;
					SeekBar progress;

					public TimerTask setProgress(SeekBar prg) {
						progress = prg;
						return this;
					}
				}.setProgress(seekBar);

				volTimer.scheduleAtFixedRate(volTimerTask, 0, 100);
			}

			public void onStopTrackingTouch(SeekBar seekBar) {
				volTimerTask.cancel();
				volTimerTask.run();
			}
		});

		progressBarTrack.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub

			}

			public void onStopTrackingTouch(final SeekBar seekBar) {

				final MPDApplication app = (MPDApplication) getActivity().getApplication();
				new Job() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.seek(seekBar.getProgress());
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				};
			}
		});

		songNameText.setText(getResources().getString(R.string.notConnected));
		Log.i("Initialization succeeded");

		return view;
	}

	private class ButtonEventHandler implements Button.OnClickListener, Button.OnLongClickListener {

		public void onClick(View v) {
			final MPDApplication app = (MPDApplication) getActivity().getApplication();
			final MPD mpd = app.oMPDAsyncHelper.oMPD;
			Intent i = null;

			switch (v.getId()) {
			case R.id.stop:
				new Job() {
					@Override
					public void run() {
						try {
							mpd.stop();
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				};
				if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
					i = new Intent(app, StreamingService.class);
					i.setAction("org.musicpd.android.DIE");
					getActivity().startService(i);
					((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode = false;
				}
				break;
			case R.id.next:
				new Job() {
					@Override
					public void run() {
						try {
							mpd.next();
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				};
				if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
					i = new Intent(app, StreamingService.class);
					i.setAction("org.musicpd.android.RESET_STREAMING");
					getActivity().startService(i);
				}
				break;
			case R.id.prev:
				new Job() {
					@Override
					public void run() {
						try {
							mpd.previous();
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				};
						
				if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
					i = new Intent(app, StreamingService.class);
					i.setAction("org.musicpd.android.RESET_STREAMING");
					getActivity().startService(i);
				}
				break;
			case R.id.playpause:
				/**
				 * If playing or paused, just toggle state, otherwise start playing.
				 * 
				 * @author slubman
				 */
				new Job() {
					@Override
					public void run() {
						String state;
						try {
							state = mpd.getStatus().getState();
							if (state.equals(MPDStatus.MPD_STATE_PLAYING) || state.equals(MPDStatus.MPD_STATE_PAUSED)) {
								mpd.pause();
							} else {
								mpd.play();
							}
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				};
				break;
            case R.id.shuffle:
                try {
					mpd.setRandom(!mpd.getStatus().isRandom());
				} catch (MPDServerException e) {
				}
                break;
            case R.id.repeat:
                try {
					mpd.setRepeat(!mpd.getStatus().isRepeat());
				} catch (MPDServerException e) {
				}
                break;

			}
		}

		public boolean onLongClick(View v) {
			MPDApplication app = (MPDApplication) getActivity().getApplication();
			MPD mpd = app.oMPDAsyncHelper.oMPD;
			try {
				switch (v.getId()) {
				case R.id.playpause:
					// Implements the ability to stop playing (may be useful for streams)
					mpd.stop();
					Intent i;
					if (((MPDApplication) getActivity().getApplication()).getApplicationState().streamingMode) {
						i = new Intent(app, StreamingService.class);
						i.setAction("org.musicpd.android.STOP_STREAMING");
						getActivity().startService(i);
					}
					break;
				default:
					return false;
				}
				return true;
			} catch (MPDServerException e) {

			}
			return true;
		}

	}

	private class PosTimerTask extends TimerTask {
		Date date = new Date();
		long start=0;
		long ellapsed=0;
		public PosTimerTask(long start) {
			this.start=start;
		}
		@Override
		public void run() {
			Date now=new Date();
			ellapsed=start+((now.getTime()-date.getTime())/1000);
			progressBarTrack.setProgress((int)ellapsed);
			handler.post(new Runnable() {
				@Override
				public void run() {
					showTrackTimes(ellapsed, lastSongTime);
			    }
			});
			lastElapsedTime = ellapsed;
		}
	}

	void showTrackTimes(long ellapsed, long lastSongTime) {
		trackTime.setText(timeToString(ellapsed));
		trackTotalTime.setText(timeToString(lastSongTime));
		trackRemainingTime.setText("-" + timeToString(lastSongTime - ellapsed));
	}

	private void startPosTimer(long start) {
		stopPosTimer();
		posTimer = new Timer();
		posTimerTask = new PosTimerTask(start);
		posTimer.scheduleAtFixedRate(posTimerTask, 0, 1000);
	}

	private void stopPosTimer() {
		if (null!=posTimer) {
			posTimer.cancel();
			posTimer=null;
		}
	}

	private String lastArtist = "";
	private String lastAlbum = "";

	public void updateTrackInfo() {
		updateTrackInfo(null);
	}

	public void updateTrackInfo(MPDStatus status) {
		new updateTrackInfoAsync(((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD).execute(status);
	}

	public class updateTrackInfoAsync extends AsyncTask<MPDStatus, Void, Boolean> {
		Music actSong = null;
		MPDStatus status = null;
		final MPD oMPD;
		public updateTrackInfoAsync(MPD oMPD) { this.oMPD = oMPD; }

		@Override
		protected Boolean doInBackground(MPDStatus... params) {
			String state = null;
			try {
				if (params == null || params[0] == null)
					state = (status = oMPD.getStatus()).getState();
				else
					state = (status = params[0]).getState();
				if (state != null) {
					int songPos = status.getSongPos();
					if (songPos >= 0) {
						actSong = oMPD.getPlaylist().getByIndex(songPos);
						return true;
					}
				}
			} catch (Exception e) {
				Log.w(e);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result != null && result) {
				String artist = null;
				String title = null;
				String album = null;
				String path = null;
				String filename = null;
				int songMax = 0;
				boolean noSong=actSong == null || status.getPlaylistLength() == 0;
				if (noSong) {
					currentSong = null;
					title = noSongInfo;
				} else if (actSong.isStream()){
					currentSong = actSong;
					Log.d("Playing a stream");
					if (actSong.haveTitle()) {
						title = actSong.getTitle();
					}
					artist = actSong.getArtist();
					album = actSong.getName();
					path = actSong.getPath();
					filename = actSong.getFilename();
					songMax = (int) actSong.getTime();
				} else {
					currentSong = actSong;
					Log.d("We did find an artist");
					artist = actSong.getArtist();
					title = actSong.getTitle();
					album = actSong.getAlbum();
					path = actSong.getPath();
					filename = actSong.getFilename();
					songMax = (int) actSong.getTime();
				}

				artist = artist == null ? "" : artist;
				title = title == null ? "" : title;
				album = album == null ? "" : album;

				artistNameText.setText(artist);
				songNameText.setText(title);
				albumNameText.setText(album);
				progressBarTrack.setMax(songMax);
				updateStatus(status);
				if (noSong || actSong.isStream()) {
					lastArtist = artist;
					lastAlbum = album;
					showTrackTimes(0, 0);
					coverArtListener.onCoverNotFound();
				} else if (!lastAlbum.equals(album) || !lastArtist.equals(artist)) {
					// coverSwitcher.setVisibility(ImageSwitcher.INVISIBLE);
					coverArtProgress.setVisibility(ProgressBar.VISIBLE);
					oCoverAsyncHelper.downloadCover(artist, album, path, filename);
					lastArtist = artist;
					lastAlbum = album;
				}
			} else {
				artistNameText.setText("");
				songNameText.setText(R.string.noSongInfo);
				albumNameText.setText("");
				progressBarTrack.setMax(0);
			}
		}
	}

	public void checkConnected() {
		connected = ((MPDApplication) getActivity().getApplication()).oMPDAsyncHelper.oMPD.isConnected();
		if (connected) {
			songNameText.setText(noSongInfo);
		} else {
			songNameText.setText(getResources().getString(R.string.notConnected));
		}
		return;
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getActivity().getApplicationContext();
		app.oMPDAsyncHelper.removeStatusChangeListener(this);
		app.oMPDAsyncHelper.removeTrackPositionListener(this);
		stopPosTimer();
		app.unsetActivity(this);
	}

	@Override
	public void onDestroy() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		settings.unregisterOnSharedPreferenceChangeListener(this);
		getActivity().unregisterReceiver(MPDConnectionHandler.getInstance());
		super.onDestroy();
	}

	public SeekBar getProgressBarTrack() {
		return progressBarTrack;
	}

	private static String timeToString(long seconds) {
		if (seconds<0) {
			seconds=0;
		}

		long hours = seconds / 3600;
		seconds -= 3600 * hours;
		long minutes = seconds / 60;
		seconds -= minutes * 60;
		if (hours == 0) {
			return String.format("%d:%02d", minutes, seconds);
		} else {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(CoverAsyncHelper.PREFERENCE_CACHE) || key.equals(CoverAsyncHelper.PREFERENCE_LASTFM)
				|| key.equals(CoverAsyncHelper.PREFERENCE_LOCALSERVER)) {
			oCoverAsyncHelper.setCoverRetrieversFromPreferences();
		} else if(key.equals("enableStopButton")) {
			applyStopButtonVisibility(sharedPreferences);
		}
	}

	private void applyStopButtonVisibility(SharedPreferences sharedPreferences) {
		if (stopButton == null) {
			return;
		}
		stopButton.setVisibility(sharedPreferences.getBoolean("enableStopButton", false) ? View.VISIBLE : View.GONE);
	}

	@Override
	public void trackPositionChanged(MPDStatus status) {
		startPosTimer(status.getElapsedTime());
	}

	@Override
	public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
		progressBarVolume.setProgress(mpdStatus.getVolume());
	}

	@Override
	public void volumeAdjusted(int newVolume, int oldVolume) {
		showVolume();
	}

	boolean isLandscape() {
		try {
			return tablet || this.getActivity().getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT;
		} catch(Exception e) {
			return true;
		}
	}

	final static long show_time = 1000;
	public void showVolume() {
		if (isLandscape())
			return;
		volume_show_time = new Date().getTime();
		volume.post(new Runnable() {
			@Override
			public void run() {
				progress.setVisibility(View.GONE);
				volume.setVisibility(View.VISIBLE);
				volume.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (volume_show_time + show_time <= new Date().getTime()) {
							if (isLandscape())
								return;
							volume.setVisibility(View.GONE);
							progress.setVisibility(View.VISIBLE);
						}
					}
				}, show_time);
			}
		});
	}

	@Override
	public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
		// If the playlist changed but not the song position in the playlist
		// We end up being desynced. Update the current song.
		try {
			updateTrackInfo();
		} catch (Exception e) {
			Log.w(e);
		}
	}

	private void updateStatus(MPDStatus status) {
		Activity activity = getActivity();
		if (activity == null)
			return;
		final MPDApplication app = (MPDApplication) getActivity().getApplication();
		if (status == null) {
			status = app.getApplicationState().currentMpdStatus;
			if (status == null)
				return;
		} else {
			app.getApplicationState().currentMpdStatus = status;
		}
		lastElapsedTime = status.getElapsedTime();
		lastSongTime = status.getTotalTime();
		showTrackTimes(lastElapsedTime, lastSongTime);
		progressBarTrack.setProgress((int) status.getElapsedTime());
		if (status.getState() != null) {

			this.getActivity().supportInvalidateOptionsMenu();

			if (status.getState().equals(MPDStatus.MPD_STATE_PLAYING)) {
				startPosTimer(status.getElapsedTime());
				ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
				button.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_pause));
			} else {
				stopPosTimer();
				ImageButton button = (ImageButton) getView().findViewById(R.id.playpause);
				button.setImageDrawable(getResources().getDrawable(R.drawable.ic_media_play));
			}
		}
        setShuffleButton(status.isRandom());
        setRepeatButton(status.isRepeat());
        TipsActivity.consider(activity, app);
	}

	@Override
	public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
		updateTrackInfo(mpdStatus);
	}

	@Override
	public void stateChanged(MPDStatus mpdStatus, String oldState) {
		updateStatus(mpdStatus);
	}

	@Override
	public void repeatChanged(boolean repeating) {
		setRepeatButton(repeating);
	}

	@Override
	public void randomChanged(boolean random) {
		setShuffleButton(random);
	}

	@Override
	public void connectionStateChanged(boolean connected, boolean connectionLost) {
		checkConnected();
	}

	@Override
	public void libraryStateChanged(boolean updating) {
		// TODO Auto-generated method stub
		
	}

	private void setShuffleButton(boolean on) {
		if (null!=shuffleButton && shuffleCurrent!=on) {
			int[] attrs = new int[] { on ? R.attr.shuffleEnabled : R.attr.shuffleDisabled };
			final TypedArray ta = getActivity().obtainStyledAttributes(attrs);
			final Drawable drawableFromTheme = ta.getDrawable(0);
			shuffleButton.setImageDrawable(drawableFromTheme);
			shuffleButton.invalidate();
			shuffleCurrent=on;
		}
	}

	private void setRepeatButton(boolean on) {
		if (null!=repeatButton && repeatCurrent!=on) {
			int[] attrs = new int[] { on ? R.attr.repeatEnabled : R.attr.repeatDisabled };
			final TypedArray ta = getActivity().obtainStyledAttributes(attrs);
			final Drawable drawableFromTheme = ta.getDrawable(0);
			repeatButton.setImageDrawable(drawableFromTheme);
			repeatButton.invalidate();
			repeatCurrent=on;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> adpaterView, View view, int position, long id) {
		popupMenu.dismiss();
		if(currentSong == null)
			return;
		BrowseFragment fragment = null;
		final int action = ((PopupMenuItem) adpaterView.getAdapter().getItem(position)).actionId;
		switch(action) {
			case POPUP_ALBUM:
				fragment = new AlbumsFragment().init(null, new Artist(currentSong.getArtist(), 2));
				break;
			case POPUP_ARTIST:
				fragment = new SongsFragment().init(null, new Artist(currentSong.getArtist(), 2), new Album(currentSong.getAlbum()));
				break;
			case POPUP_FOLDER:
				fragment = new FSFragment().init(currentSong.getParent());
				break;
			case POPUP_STREAM:
				fragment = new StreamsFragment();
				break;
			case POPUP_SHARE:
				String shareString = getString(R.string.sharePrefix);
				shareString += " " + currentSong.getTitle();
				if (!currentSong.isStream()) {
					shareString += " - " + currentSong.getArtist();
				}
				final Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, shareString);
				sendIntent.setType("text/plain");
				startActivity(sendIntent);
				break;
			case POPUP_INFO:
				InformationActivity.start(getActivity(), new String[] { "file", currentSong.getFullpath() });
				break;
		}
		if (fragment != null)
			((ILibraryFragmentActivity) getActivity()).pushLibraryFragment(fragment, "playing");
	}
}

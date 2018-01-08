package org.musicpd.android;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.Music;
import org.a0z.mpd.event.StatusChangeListener;
import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender.*;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.WindowManager.BadTokenException;
import android.widget.RemoteViews;

import org.musicpd.android.helpers.MPDAsyncHelper;
import org.musicpd.android.helpers.MPDAsyncHelper.ConnectionListener;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.NetworkHelper;
import org.musicpd.android.tools.SettingsHelper;
import org.musicpd.android.widgets.WidgetHelperService;

@ReportsCrashes(
	mode = ReportingInteractionMode.TOAST,
	resToastText = R.string.crashReport, 
	httpMethod = Method.PUT,
	reportType = Type.JSON,
	formUri = "http://mupeace.cloudant.com/acra-mupeace/_design/acra-storage/_update/report",
	formUriBasicAuthLogin = "waskervaptembrivoutstone",
	formUriBasicAuthPassword = "yH27tfmOtgx8DkwgoJtAEJDN",
	formKey = ""
)
public class MPDApplication extends Application implements ConnectionListener, StatusChangeListener {

	public static final String TAG = "Mupeace";
	
	private static final long DISCONNECT_TIMER = 15000; 
	
	public MPDAsyncHelper oMPDAsyncHelper = null;
	private SettingsHelper settingsHelper = null;
	private ApplicationState state = new ApplicationState();
	public final ServerDiscovery serverDiscovery = new ServerDiscovery(this);
	
	private Collection<Object> connectionLocks = new LinkedList<Object>();
	private AlertDialog ad;
	private Activity currentActivity;
	private Timer disconnectSheduler;

	public class ApplicationState {
		public boolean streamingMode = false;
		public boolean settingsShown = false;
		public boolean warningShown = false;
		public MPDStatus currentMpdStatus = null;
	}
	
	class DialogClickListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case AlertDialog.BUTTON_NEUTRAL:
				// Show Settings
				currentActivity.startActivityForResult(new Intent(currentActivity, WifiConnectionSettings.class), SETTINGS);
				break;
			case AlertDialog.BUTTON_NEGATIVE:
				currentActivity.startActivityForResult(new Intent(currentActivity, ServerBonjourListActivity.class), SETTINGS);
				break;
			case AlertDialog.BUTTON_POSITIVE:
				connectMPD();
				break;

			}
		}
	}

	public static final int SETTINGS = 5;

	@TargetApi(9)
	@Override
	public void onCreate() {
		super.onCreate();
		Log.tag = this.getString(R.string.app_name);
		Log.i("onCreate Application");
		ACRA.init(this);
		
		MPD.setApplicationContext(getApplicationContext());

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.VmPolicy vmpolicy = new StrictMode.VmPolicy.Builder().penaltyLog().build();
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
			StrictMode.setVmPolicy(vmpolicy);
		}

		useThreadPool();

		oMPDAsyncHelper = new MPDAsyncHelper();
		oMPDAsyncHelper.addConnectionListener((MPDApplication) getApplicationContext());
		oMPDAsyncHelper.addStatusChangeListener((MPDApplication) getApplicationContext());
		
		settingsHelper = new SettingsHelper(this, oMPDAsyncHelper);
		
		disconnectSheduler = new Timer();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		if(!settings.contains("albumTrackSort"))
			settings.edit().putBoolean("albumTrackSort", true).commit();
	}

	@SuppressLint({"InlinedApi", "NewApi"})
	void useThreadPool() {
		try {
			android.os.AsyncTask.class
			.getMethod("setDefaultExecutor", java.util.concurrent.Executor.class)
			.invoke(null, android.os.AsyncTask.THREAD_POOL_EXECUTOR);
		} catch(Exception e) {
			Log.w(e);
		}
	}

	public void onPause() {
		serverDiscovery.onPause();
	}

	public void onResume() {
		serverDiscovery.onResume();
	}

	public void setActivity(Object activity) {
		if (activity instanceof Activity)
			currentActivity = (Activity) activity;
		
		connectionLocks.add(activity);
		checkMonitorNeeded();
		checkConnectionNeeded();
		cancelDisconnectSheduler();
	}

	public void unsetActivity(Object activity) {
		connectionLocks.remove(activity);
		checkMonitorNeeded();
		checkConnectionNeeded();
		
		if (currentActivity == activity)
			currentActivity = null;
	}

	private void checkMonitorNeeded() {
		if (connectionLocks.size() > 0) {
			if (!oMPDAsyncHelper.isMonitorAlive())
				oMPDAsyncHelper.startMonitor();
		} else {
			oMPDAsyncHelper.stopMonitor();
		}
	}

	private void checkConnectionNeeded() {
		if (connectionLocks.size() > 0) {
			if (!oMPDAsyncHelper.oMPD.isConnected() && (currentActivity == null || !currentActivity.getClass().equals(WifiConnectionSettings.class)))
				connect();
		} else {
			disconnect();
		}
	}

	public void connect() {
		if(!settingsHelper.updateSettings()) {
			// Absolutely no settings defined! Open Settings!
			if (currentActivity != null && !state.settingsShown) {
				currentActivity.startActivityForResult(new Intent(currentActivity, ServerBonjourListActivity.class), SETTINGS);
				state.settingsShown = true;
			}
		}
		
		if (currentActivity != null && !settingsHelper.warningShown() && !state.warningShown) {
			currentActivity.startActivity(new Intent(currentActivity, WarningActivity.class));
			state.warningShown = true;
		}
		connectMPD();
	}

	public void reconnect() {
		try {
			expected_disconnects++;
			oMPDAsyncHelper.disconnect();
		} finally {
			//dismissAlertDialog();
			//cancelDisconnectSheduler();
			//oMPDAsyncHelper.connect();
		}
	}

	public void terminateApplication() {
		serverDiscovery.onDestroy();
		this.currentActivity.finish();
	}
	
	public void disconnect() {
		cancelDisconnectSheduler();
		startDisconnectSheduler();
	}
	
	private void startDisconnectSheduler() {
		disconnectSheduler.schedule(new TimerTask() {
			@Override
			public void run() {
				Log.w("Disconnecting (" + DISCONNECT_TIMER + " ms timeout)");
				oMPDAsyncHelper.disconnect();
			}
		}, DISCONNECT_TIMER);
		
	}
	
	private void cancelDisconnectSheduler() {
		disconnectSheduler.cancel();
		disconnectSheduler.purge();
		disconnectSheduler = new Timer();
	}

	private void connectMPD() {
		// dismiss possible dialog
		dismissAlertDialog();
		
		// check for network
		if (!NetworkHelper.isNetworkConnected(this.getApplicationContext())) {
			connectionFailed("No network.");
			return;
		}
		
		// show connecting to server dialog
		if (currentActivity != null) {
			ad = new ProgressDialog(currentActivity);
			ad.setTitle(getResources().getString(R.string.connecting));
			ad.setMessage(getResources().getString(R.string.connectingToServer));
			ad.setCancelable(false);
			ad.setOnKeyListener(new OnKeyListener() {
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					// Handle all keys!
					return true;
				}
			});
			try {
				ad.show();
			} catch (BadTokenException e) {
				// Can't display it. Don't care.
			}
		}
		
		cancelDisconnectSheduler();
		
		// really connect
		oMPDAsyncHelper.connect();
	}

	int expected_disconnects = 0;
	public void connectionFailed(String message) {
		if (expected_disconnects > 0) {
			expected_disconnects--;
			connectMPD();
			return;
		}

		// dismiss possible dialog
		dismissAlertDialog();
		
		if (currentActivity == null)
			return;
		
		if (currentActivity != null && connectionLocks.size() > 0) {
			// are we in the settings activity?
			if (currentActivity.getClass() == SettingsActivity.class) {
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				builder.setMessage(String.format(getResources().getString(R.string.connectionFailedMessageSetting), message));
				builder.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface arg0, int arg1) {
					}
				});
				ad = builder.show();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				builder.setTitle(getResources().getString(R.string.connectionFailed));
				builder.setMessage(String.format(getResources().getString(R.string.connectionFailedMessage), message));
				
				DialogClickListener oDialogClickListener = new DialogClickListener();
				builder.setNegativeButton(getResources().getString(R.string.search), oDialogClickListener);
				builder.setNeutralButton(getResources().getString(R.string.settings), oDialogClickListener);
				builder.setPositiveButton(getResources().getString(R.string.retry), oDialogClickListener);
				try {
					ad = builder.show();
				} catch (BadTokenException e) {
					// Can't display it. Don't care.
				}
			}
		}

	}

	public void connectionSucceeded(String message) {
		dismissAlertDialog();
		// checkMonitorNeeded();
	}

	public ApplicationState getApplicationState() {
		return state;
	}
	
	private void dismissAlertDialog() {
		if (ad != null) {
			if (ad.isShowing()) {
				try {
					ad.dismiss();
				} catch (IllegalArgumentException e) {
					// We don't care, it has already been destroyed
				}
			}
		}
	}

	public boolean isTabletUiEnabled() {
		return getResources().getBoolean(R.bool.isTablet)
				&& PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletUI", true);
	}

	public boolean isLightThemeSelected() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			return false;
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lightTheme", false);
	}

	public boolean isLightNowPlayingThemeSelected() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
			return false;
		return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("lightNowPlayingTheme", false);
	}

  @Override
  public void volumeChanged(MPDStatus mpdStatus, int oldVolume) {
    // ignore
  }

  @Override
  public void playlistChanged(MPDStatus mpdStatus, int oldPlaylistVersion) {
    updateNotification(mpdStatus);
  }

  @Override
  public void trackChanged(MPDStatus mpdStatus, int oldTrack) {
    updateNotification(mpdStatus);
  }

  @Override
  public void stateChanged(MPDStatus mpdStatus, String oldState) {
    if (mpdStatus.getState().equals(MPDStatus.MPD_STATE_PLAYING) || mpdStatus.getState().equals(MPDStatus.MPD_STATE_PAUSED)){
      updateNotification(mpdStatus);
    }else{
      dismissNotification();
    }
  }

  @Override
  public void repeatChanged(boolean repeating) {
    // ignore
  }

  @Override
  public void randomChanged(boolean random) {
    // ignore
  }

  @Override
  public void connectionStateChanged(boolean connected, boolean connectionLost) {
    if (connectionLost){
      dismissNotification();
    }
  }

  @Override
  public void libraryStateChanged(boolean updating) {
    // ignore
  }

  @SuppressLint("NewApi")
  public Notification createNotification(MPDStatus status, Music song) {
    RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_panel);
    rv.setTextViewText(R.id.title, song.getTitle());
    rv.setTextViewText(R.id.artist, song.getArtist());
    rv.setTextViewText(R.id.album, song.getAlbum());
    
    boolean isPlaying = MPDStatus.MPD_STATE_PLAYING.equals(status.getState());
    
    rv.setImageViewResource(R.id.playpause, isPlaying ? R.drawable.ic_media_pause
        : R.drawable.ic_media_play);
    rv.setOnClickPendingIntent(R.id.album_art,
        PendingIntent.getActivity(this, 0, new Intent(this, MainMenuActivity.class), 0));
    
    Intent intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_STOP);
    rv.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_PLAYPAUSE);
    rv.setOnClickPendingIntent(R.id.playpause, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_NEXT);
    rv.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_STOP);
    rv.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, intent, 0));
    
    RemoteViews rvBig = new RemoteViews(this.getPackageName(), R.layout.notification_panel_big);
    rvBig.setTextViewText(R.id.title, song.getTitle());
    rvBig.setTextViewText(R.id.artist, song.getArtist());
    rvBig.setTextViewText(R.id.album, song.getAlbum());
    
    rvBig.setImageViewResource(R.id.playpause, isPlaying ? R.drawable.ic_media_pause
        : R.drawable.ic_media_play);
    rvBig.setOnClickPendingIntent(R.id.album_art,
        PendingIntent.getActivity(this, 0, new Intent(this, MainMenuActivity.class), 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_STOP);
    rvBig.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_PREV);
    rvBig.setOnClickPendingIntent(R.id.prev, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_PLAYPAUSE);
    rvBig.setOnClickPendingIntent(R.id.playpause, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_NEXT);
    rvBig.setOnClickPendingIntent(R.id.next, PendingIntent.getService(this, 0, intent, 0));
    
    intent = new Intent(this, WidgetHelperService.class);
    intent.setAction(WidgetHelperService.CMD_STOP);
    rv.setOnClickPendingIntent(R.id.close, PendingIntent.getService(this, 0, intent, 0));
    
    Notification nf = new Notification();
    nf.icon = R.drawable.ic_media_play;
    nf.contentView = rv;
    nf.bigContentView = rvBig;
    
    return nf;
    
//    NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//    builder
//        .setOngoing(true)
//        .setPriority(Notification.PRIORITY_HIGH)
//       // .setContent(rv)
//        .setStyle(
//            new NotificationCompat.BigPictureStyle(builder).setBigContentTitle(song.getTitle())
//                .setSummaryText(song.getArtist() + " | " + song.getAlbum()))
//        .setSmallIcon(R.drawable.stat_notify_musicplayer).setContentTitle(song.getTitle())
//        .setContentText(song.getArtist() + " | " + song.getAlbum());
//    
////    builder.addAction(R.drawable.ic_appwidget_music_prev, null, null);
////
////    if (status.getState().equals(MPDStatus.MPD_STATE_PLAYING)) {
////      builder.addAction(R.drawable.ic_appwidget_music_pause, null, null);
////    } else {
////      builder.addAction(R.drawable.ic_appwidget_music_play, null, null);
////    }
////    builder.addAction(R.drawable.ic_appwidget_music_next, null, null);
////    builder.addAction(R.drawable.ic_media_stop, null, null);
//
//    return builder.build();
  }
  
  public void updateNotificationSync(MPDStatus status) {
    String state = null;
    try {
      state = status.getState();
      if (state != null) {
        int songPos = status.getSongPos();
        if (songPos >= 0) {
          Music actSong = oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
          if (actSong != null) {
            Notification notification = createNotification(status, actSong);
            if (notification == null) {
              dismissNotification();
              return;
            }

            final NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(0, notification);
          }
        }
      }
    } catch (Exception e) {
      Log.w(e);
    }
  }
  
  public void updateNotification(MPDStatus status) {
    
    new AsyncTask<MPDStatus, Void, Boolean>() {
      
      Music actSong = null;
      MPDStatus status = null;

      @Override
      protected Boolean doInBackground(MPDStatus... params) {
        String state = null;
        try {
          status = params[0];
          state = status.getState();
          if (state != null) {
            int songPos = status.getSongPos();
            if (songPos >= 0) {
              actSong = oMPDAsyncHelper.oMPD.getPlaylist().getByIndex(songPos);
              return true;
            }
          }
        } catch (Exception e) {
          Log.w(e);
        }
        return false;
      }

      protected void onPostExecute(Boolean result) {
        
        if (result == null || !result || actSong == null) {
          dismissNotification();
          return;
        }
        
        Notification notification = createNotification(status, actSong);

        if (notification == null) {
          dismissNotification();
          return;
        }

        final NotificationManager nm =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(0, notification);
      };

    }.execute(status);
  }
  
  public void dismissNotification() {
    final NotificationManager nm =
        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    nm.cancel(0);
  }
}

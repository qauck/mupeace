package org.musicpd.android;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.a0z.mpd.MPD;
import org.a0z.mpd.MPDOutput;
import org.a0z.mpd.MPDStatus;
import org.a0z.mpd.exception.MPDServerException;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import org.musicpd.android.MPDActivities.MPDFragmentActivity;
import org.musicpd.android.fragments.BrowseFragment;
import org.musicpd.android.fragments.NowPlayingFragment;
import org.musicpd.android.fragments.PlaylistFragment;
import org.musicpd.android.fragments.PlaylistFragmentCompat;
import org.musicpd.android.helpers.MPDAsyncHelper.ConnectionListener;
import org.musicpd.android.library.ILibraryFragmentActivity;
import org.musicpd.android.library.LibraryTabActivity;
import org.musicpd.android.tools.LibraryTabsUtil;
import org.musicpd.android.tools.Log;
import org.musicpd.android.tools.Tools;

public class MainMenuActivity extends MPDFragmentActivity implements OnNavigationListener, ILibraryFragmentActivity {

	public static final int PLAYLIST = 1;

	public static final int ARTISTS = 2;

	public static final int SETTINGS = 5;

	public static final int STREAM = 6;

	public static final int LIBRARY = 7;

	public static final int CONNECT = 8;
	
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the
     * sections. We use a {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will
     * keep every loaded fragment in memory. If this becomes too memory intensive, it may be best
     * to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;	
    private int backPressExitCount;
    private Handler exitCounterReset;
	private boolean isDualPaneMode;
	ActionBar actionBar;
	ArrayAdapter<CharSequence> actionBarAdapter;
	List<String> tabs;
	ConnectionListener persistentConnectionListener;
	ActionBarDrawerToggle drawerToggle;

	@SuppressLint("NewApi")
	@TargetApi(11)
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		final MPDApplication app = (MPDApplication) getApplication();
		setContentView(app.isTabletUiEnabled() ? R.layout.main_activity_tablet : R.layout.main_activity);
        
		isDualPaneMode = (findViewById(R.id.playlist_fragment) != null);

        exitCounterReset = new Handler();
        
		if (android.os.Build.VERSION.SDK_INT >= 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Create the adapter that will return a fragment for each of the three primary sections
		// of the app.
		tabs = LibraryTabsUtil.getCurrentLibraryTabs(getApplicationContext());
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the action bar.
		actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowHomeEnabled(true);
		setTitle(R.string.nowPlaying);

		actionBarAdapter = new ArrayAdapter<CharSequence>(getSupportActionBar().getThemedContext(),
				R.layout.sherlock_spinner_item);
		actionBarAdapter.add(getString(LibraryTabsUtil.getTabTitleResId(tabs.get(0))));
		actionBarAdapter.add(getString(R.string.nowPlaying));
		actionBarAdapter.add(getString(R.string.playQueue));

        if(Build.VERSION.SDK_INT >= 14) {
        	//Bug on ICS with sherlock's layout
        	actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        } else {
        	actionBarAdapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
        }
        actionBar.setListNavigationCallbacks(actionBarAdapter, this);
        if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_STANDARD)
        	actionBar.setTitle(actionBarAdapter.getItem(1));
        else
        	actionBar.setSelectedNavigationItem(1);

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setCurrentItem(1, false);
		if (android.os.Build.VERSION.SDK_INT >= 9)
			mViewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // When swiping between different sections, select the corresponding tab.
        // We can also use ActionBar.Tab#select() to do this if we have a reference to the
        // Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	supportInvalidateOptionsMenu();
                if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_STANDARD)
                	actionBar.setTitle(actionBarAdapter.getItem(position));
                else
                	actionBar.setSelectedNavigationItem(position);
            }
        });

        final DrawerLayout drawer_layout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer_layout.setDrawerListener(drawerToggle = new ActionBarDrawerToggle(
            this,                  /* host Activity */
            drawer_layout,         /* DrawerLayout object */
            R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
            R.string.drawer_open,  /* "open drawer" description */
            R.string.drawer_close  /* "close drawer" description */
        ));

        final List<String> tabNames = new ArrayList<String>(tabs.size());
        for (String tab : tabs)
        	tabNames.add(getString(LibraryTabsUtil.getTabTitleResId(tab)));

        final ListView left_library = (ListView) findViewById(R.id.left_library);
        left_library.setAdapter(new ArrayAdapter<String>(this, R.layout.simple_list_item_1, tabNames));
        left_library.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainMenuActivity.this.replaceLibraryFragment(tabs.get(position), tabNames.get(position));
                drawer_layout.closeDrawers();
                mViewPager.setCurrentItem(0, true);
            }
        });
        setListViewHeightBasedOnChildren(left_library);

		final ListView left_outputs = (ListView) findViewById(R.id.left_outputs);
		final List<MPDOutput> outputs = new ArrayList<MPDOutput>();
		final ArrayAdapter<MPDOutput> outputs_adapter = new ArrayAdapter<MPDOutput>(this, android.R.layout.simple_list_item_multiple_choice, outputs);
		left_outputs.setAdapter(outputs_adapter);
		app.oMPDAsyncHelper.addConnectionListener(persistentConnectionListener = new ConnectionListener() {
			@Override
			public void connectionFailed(String message) {
			}

			@Override
			public void connectionSucceeded(String message) {
				try {
					final Collection<MPDOutput> o = app.oMPDAsyncHelper.oMPD.getOutputs();
					Iterator<MPDOutput> i = o.iterator();
					while (i.hasNext())
						if ("quiet".equalsIgnoreCase(i.next().getName()))
							i.remove();
					left_outputs.post(new Runnable() {
						@Override
						public void run() {
							outputs.clear();
							outputs.addAll(o);
							outputs_adapter.notifyDataSetChanged();
							setListViewHeightBasedOnChildren(left_outputs);
						}
					});
				} catch (MPDServerException e) {
					Log.w(e);
				}
			}
		});
		left_outputs.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				int i = 0;
				for (MPDOutput output : outputs)
					left_outputs.setItemChecked(i++, output.isEnabled());
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
			}
		});
		left_outputs.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				try {
					//if ("slave".equals(id[1]))
						//oMPD.setStickers("global", "slaves", id[0], (System.currentTimeMillis()/1000) + (on? "=on" : "=off"));
					if (left_outputs.isItemChecked(position))
						app.oMPDAsyncHelper.oMPD.enableOutput(outputs.get(position).getId());
					else
						app.oMPDAsyncHelper.oMPD.disableOutput(outputs.get(position).getId());
				} catch (MPDServerException e) {
					Log.e(e);
				}
			}
		});
		setListViewHeightBasedOnChildren(left_outputs);

		final ListView left_servers = (ListView) findViewById(R.id.left_servers);
		final ArrayAdapter<ServerInfo> servers_adapter = new ArrayAdapter<ServerInfo>(this, android.R.layout.simple_list_item_1, android.R.id.text1, app.serverDiscovery.servers);
		left_servers.setAdapter(servers_adapter);
		app.serverDiscovery.onChanged = new Runnable() {
			@Override
			public void run() {
				left_servers.post(new Runnable() {
					@Override
					public void run() {
						servers_adapter.notifyDataSetChanged();
						setListViewHeightBasedOnChildren(left_servers);
					}
				});
			}
		};
		left_servers.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				app.serverDiscovery.choose(position);
				drawer_layout.closeDrawers();
			}
		});
		setListViewHeightBasedOnChildren(left_servers);

	}

	public void setListViewHeightBasedOnChildren(ListView listView) {
		ArrayAdapter<?> listAdapter = (ArrayAdapter<?>) listView.getAdapter(); 
		if (listAdapter == null)
			return;

		int totalHeight = 0, N = listAdapter.getCount();
		for (int i = 0; i < N; i++) {
			final View listItem = listAdapter.getView(i, null, listView);
			listItem.measure(0, 0);
			totalHeight += Math.max(96, listItem.getMeasuredHeight());
		}

		ViewGroup.LayoutParams params = listView.getLayoutParams();
		params.height = totalHeight + (listView.getDividerHeight() * (N - 1));
		listView.setLayoutParams(params);
		listView.requestLayout();
   }

	static String getTitle(Fragment f) {
		if (f instanceof BrowseFragment) {
			return ((BrowseFragment) f).getTitle();
		} else {
			return f.toString();
		}
	}

	void replace(Fragment old, String title, String label) {
		actionBarAdapter.remove(getTitle(old));
		actionBarAdapter.insert(title, 0);
		if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_STANDARD)
			actionBar.setTitle(title);
	}

	public void replaceLibraryFragment(String tab, String label) {
		replace(mSectionsPagerAdapter.replace(tab), getString(LibraryTabsUtil.getTabTitleResId(tab)), label);
	}

	@Override
	public void pushLibraryFragment(Fragment fragment, String label) {
		replace(mSectionsPagerAdapter.push(fragment), getTitle(fragment), label);
	}

	@Override
	public void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	public void onStop() {
		super.onStop();
		
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		backPressExitCount = 0;
	}

	/**
	 * Called when Back button is pressed, displays message to user indicating the if back button is pressed again the application will exit. We keep a count of how many time back
	 * button is pressed within 5 seconds. If the count is greater than 1 then call system.exit(0)
	 * 
	 * Starts a post delay handler to reset the back press count to zero after 5 seconds
	 * 
	 * @return None
	 */
	@Override
	public void onBackPressed() {
		if (mViewPager.getCurrentItem() == 0) {
			Fragment old = mSectionsPagerAdapter.pop();
			if (old != null) {
				CharSequence title = mSectionsPagerAdapter.getPageTitle(0);
				actionBarAdapter.remove(getTitle(old));
				actionBarAdapter.insert(title, 0);
				if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_STANDARD)
					actionBar.setTitle(title);
				final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
				ft.remove(old);
				ft.commit();
				return;
			}
		}
		if (mViewPager.getCurrentItem() != 1) {
			mViewPager.setCurrentItem(1, true);
			return;
		}
		final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean exitConfirmationRequired = settings.getBoolean("enableExitConfirmation", false);
		if (exitConfirmationRequired && backPressExitCount < 1) {
			try {
				Tools.notifyUser(String.format(getResources().getString(R.string.backpressToQuit)), this);
			} catch (Exception e) {
				Log.w(e);
			}
			backPressExitCount += 1;
			exitCounterReset.postDelayed(new Runnable() {
				@Override
				public void run() {
					backPressExitCount = 0;
				}
			}, 5000);
		} else {
			/*
			 * Nasty force quit, should shutdown everything nicely but there just too many async tasks maybe I'll correctly implement app.terminateApplication();
			 */
			System.exit(0);
		}
		return;
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		mViewPager.setCurrentItem(itemPosition);
		return true;
	}
	
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            replace(tabs.get(0));
        }

		public Fragment replace(String tab) {
			Class<? extends Object> clazz = tab == null? null : LibraryTabsUtil.getClass(MainMenuActivity.this, tab);
			for (int i = 0, N = stack.size(); i < N; i++) {
				Fragment f = stack.get(i).getValue();
				if (tab == null || clazz.isInstance(f) && getTitle(f).equals(getString(LibraryTabsUtil.getTabTitleResId(tab))))
					try {
						return stack.peek().getValue();
					} finally {
						if (i + 1 < N) {
							stack.subList(i + 1, N).clear();
							notifyDataSetChanged();
						}
					}
			}
			try {
				return push((Fragment) Tools.instantiate(clazz));
			} finally {
				stack.subList(0, stack.size() - 1).clear();
			}
		}

        int next = 100;
        Stack<Map.Entry<Integer, Fragment>> stack = new Stack<Map.Entry<Integer, Fragment>>();
        public Fragment push(Fragment f) {
            try {
                Fragment g = stack.isEmpty()? null : stack.peek().getValue();
                stack.push(new AbstractMap.SimpleEntry<Integer, Fragment>(++next, f));
                return g;
            } finally {
                notifyDataSetChanged();
            }
        }

        public Fragment pop() {
            try {
                return stack.size() > 1? stack.pop().getValue() : null;
            } finally {
                notifyDataSetChanged();
            }
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = null;
            switch (i) {
				case 0:
					fragment = stack.peek().getValue();
					break;
				case 1:
					fragment = new NowPlayingFragment();
					break;
				case 2:
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
						fragment = new PlaylistFragment();
					} else {
						fragment = new PlaylistFragmentCompat();
					}
					break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
			return isDualPaneMode ? 2 : 3;
        }

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
				case 0: return getTitle(stack.peek().getValue());
				case 1: return getString(R.string.nowPlaying);
				case 2: return getString(R.string.playQueue);
			}
			return null;
		}

        @Override
        public int getItemPosition(Object object)
        {
        	if (object instanceof BrowseFragment && object != stack.peek().getValue())
        		return POSITION_NONE;
            return POSITION_UNCHANGED;
        }

        @Override
        public long getItemId(int position) {
            return position > 0? position : stack.peek().getKey();
        }
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getSupportMenuInflater().inflate(R.menu.mpd_mainmenu, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		//Reminder : never disable buttons that are shown as actionbar actions here
		super.onPrepareOptionsMenu(menu);

		int page = mViewPager.getCurrentItem();
		Log.i("onPrepareOptionsMenu " + page);
		//menu.findItem(R.id.menu_search).setShowAsAction(page == 0? MenuItem.SHOW_AS_ACTION_ALWAYS : MenuItem.SHOW_AS_ACTION_NEVER);
		menu.findItem(R.id.menu_up_to_root).setVisible(page == 0);
		menu.findItem(R.id.GMM_LibTab).setVisible(page == 1);
		menu.findItem(R.id.menu_back_left).setVisible(page == 2);
		menu.findItem(R.id.menu_back_right).setVisible(page == 0);
		menu.findItem(R.id.menu_playlist).setVisible(page == 1);
		menu.findItem(R.id.PLM_EditPL).setVisible(page == 2);

		final MPDApplication app = (MPDApplication) getApplication();
		MPDStatus status = app.getApplicationState().currentMpdStatus;
		menu.findItem(R.id.menu_play).setIcon(getResources().getDrawable(
			status != null && MPDStatus.MPD_STATE_PLAYING.equals(status.getState())
			? R.drawable.ic_media_pause
			: R.drawable.ic_media_play
		));

		MPD mpd = app.oMPDAsyncHelper.oMPD;
		if (!mpd.isConnected()) {
			if (menu.findItem(CONNECT) == null) {
				menu.add(0, CONNECT, 0, R.string.connect);
			}
		} else {
			if (menu.findItem(CONNECT) != null) {
				menu.removeItem(CONNECT);
			}
		}
		setMenuChecked(menu.findItem(R.id.GMM_Stream), app.getApplicationState().streamingMode);
		final MPDStatus mpdStatus = app.getApplicationState().currentMpdStatus;
		if (mpdStatus != null) {
			setMenuChecked(menu.findItem(R.id.GMM_Single), mpdStatus.isSingle());
			setMenuChecked(menu.findItem(R.id.GMM_Consume), mpdStatus.isConsume());
		}
		return true;
	}

	private void setMenuChecked(MenuItem item, boolean checked) {
		// Set the icon to a checkbox so 2.x users also get one
		item.setChecked(checked);
		item.setIcon(checked ? R.drawable.btn_check_buttonless_on : R.drawable.btn_check_buttonless_off);
	}

	private void openLibrary() {
		final Intent i = new Intent(this, LibraryTabActivity.class);
		startActivity(i);
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		drawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		drawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		if (drawerToggle.onOptionsItemSelected(getMenuItem(item)))
			return true;

		Intent i = null;
		final MPDApplication app = (MPDApplication) this.getApplication();
		final MPD mpd = app.oMPDAsyncHelper.oMPD;

		// Handle item selection
		switch (item.getItemId()) {
			case R.id.menu_search:
				this.onSearchRequested();
				return true;
			case R.id.menu_up_to_root:
				mSectionsPagerAdapter.replace(null);
				return true;
			case R.id.GMM_LibTab:
				mViewPager.setCurrentItem(0, true);
				return true;
			case R.id.menu_back_left:
			case R.id.menu_back_right:
				mViewPager.setCurrentItem(1, true);
				return true;
			case R.id.menu_playlist:
				mViewPager.setCurrentItem(2, true);
				return true;
			case R.id.GMM_Settings:
				i = new Intent(this, SettingsActivity.class);
				startActivityForResult(i, SETTINGS);
				return true;
			case R.id.GMM_Outputs:
				i = new Intent(this, SettingsActivity.class);
				i.putExtra(SettingsActivity.OPEN_OUTPUT, true);
				startActivityForResult(i, SETTINGS);
				return true;
			case CONNECT:
				((MPDApplication) this.getApplication()).connect();
				return true;
			case R.id.GMM_Stream:
				if (app.getApplicationState().streamingMode) {
					i = new Intent(this, StreamingService.class);
					i.setAction("org.musicpd.android.DIE");
					this.startService(i);
					((MPDApplication) this.getApplication()).getApplicationState().streamingMode = false;
					// Toast.makeText(this, "MPD Streaming Stopped", Toast.LENGTH_SHORT).show();
				} else {
					if (app.oMPDAsyncHelper.oMPD.isConnected()) {
						i = new Intent(this, StreamingService.class);
						i.setAction("org.musicpd.android.START_STREAMING");
						this.startService(i);
						((MPDApplication) this.getApplication()).getApplicationState().streamingMode = true;
						// Toast.makeText(this, "MPD Streaming Started", Toast.LENGTH_SHORT).show();
					}
				}
				return true;
			case R.id.GMM_bonjour:
				startActivity(new Intent(this, ServerBonjourListActivity.class));
				return true;
			case R.id.GMM_Consume:
				try {
					mpd.setConsume(!mpd.getStatus().isConsume());
				} catch (MPDServerException e) {
				}
				return true;
			case R.id.GMM_Single:
				try {
					mpd.setSingle(!mpd.getStatus().isSingle());
				} catch (MPDServerException e) {
				}
				return true;
			case R.id.menu_previous:
			case R.id.menu_play:
			case R.id.menu_next:
				new AsyncTask<Void, Void, Void>() {
					@Override
					protected Void doInBackground(Void... params) {
						try {
							switch (item.getItemId()) {
								case R.id.menu_previous:
									mpd.previous();
									break;
								case R.id.menu_play:
									String state = mpd.getStatus().getState();
									if (state == null || state.equals(MPDStatus.MPD_STATE_PLAYING) || state.equals(MPDStatus.MPD_STATE_PAUSED))
										mpd.pause();
									else
										mpd.play();
									break;
								case R.id.menu_next:
									mpd.next();
									break;
							}
						} catch (MPDServerException e) {
							Log.w(e);
						}
						return null;
					}
				}.execute();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}

	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						app.oMPDAsyncHelper.oMPD.next();
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			}).start();
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						app.oMPDAsyncHelper.oMPD.previous();
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			}).start();
			return true;
		}
		return super.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			// For onKeyLongPress to work
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, final KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			if (event.isTracking() && !event.isCanceled() && !app.getApplicationState().streamingMode) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.adjustVolume(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ? NowPlayingFragment.VOLUME_STEP
									: -NowPlayingFragment.VOLUME_STEP);
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				}).start();
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
    
}

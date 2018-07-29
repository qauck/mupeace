package org.musicpd.android.library;

import java.util.ArrayList;

import org.a0z.mpd.exception.MPDServerException;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.app.ActionBar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import org.musicpd.android.MPDApplication;
import org.musicpd.android.MPDActivities.MPDFragmentActivity;
import org.musicpd.android.R;
import org.musicpd.android.fragments.BrowseFragment;
import org.musicpd.android.fragments.LibraryFragment;
import org.musicpd.android.fragments.NowPlayingFragment;
import org.musicpd.android.tools.Job;
import org.musicpd.android.tools.LibraryTabsUtil;
import org.musicpd.android.tools.Log;


public class LibraryTabActivity extends MPDFragmentActivity implements ActionBar.OnNavigationListener,
		ILibraryFragmentActivity,
		ILibraryTabActivity {

	private static final String FRAGMENT_TAG_LIBRARY = "library";

	LibraryFragment libraryFragment;
    
	ActionBar actionBar;
	ArrayList<String> mTabList;


	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tabs);
        
		final FragmentManager fm = getSupportFragmentManager();

        // Get the list of the currently visible tabs
        mTabList=LibraryTabsUtil.getCurrentLibraryTabs(this.getApplicationContext());

        // Set up the action bar.
		actionBar = getActionBar();
		// Will set the action bar to it's List style.
		final int fmStackCount = fm.getBackStackEntryCount();
		if(fmStackCount > 0) {
			refreshActionBarNavigation(false, fm.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
		} else {
			refreshActionBarNavigation(true, null);
		}
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
		ArrayAdapter<CharSequence> actionBarAdapter = new ArrayAdapter<CharSequence>(getActionBar().getThemedContext(),
				android.R.layout.simple_spinner_item);
        for (int i=0;i<mTabList.size();i++){
            actionBarAdapter.add(LibraryTabsUtil.getTabTitle(this, mTabList.get(i)).getString(this));
        }

        if(Build.VERSION.SDK_INT >= 14) {
        	//Bug on ICS with sherlock's layout
        	actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        } else {
        	actionBarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
        actionBar.setListNavigationCallbacks(actionBarAdapter, this);

		libraryFragment = (LibraryFragment) fm.findFragmentByTag(FRAGMENT_TAG_LIBRARY);
		if (libraryFragment == null) {
			libraryFragment = new LibraryFragment();
			final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
			ft.replace(R.id.root_frame, libraryFragment, FRAGMENT_TAG_LIBRARY);
			ft.commit();
		}

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
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.mpd_browsermenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_search:
			this.onSearchRequested();
			return true;
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		libraryFragment.setCurrentItem(itemPosition, true);
		return true;
	}

	public void refreshActionBarNavigation(boolean enableTabs, CharSequence title) {
		if (enableTabs) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
			actionBar.setDisplayShowTitleEnabled(false);
		} else {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
			actionBar.setDisplayShowTitleEnabled(true);
			actionBar.setTitle(title);
		}
	}

	@Override
	public void pushLibraryFragment(Fragment fragment, String label) {
		String title = getTitle(fragment);
		refreshActionBarNavigation(false, title);
		final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
		ft.replace(R.id.root_frame, fragment);
		ft.addToBackStack(label);
		ft.setBreadCrumbTitle(title);
		ft.commit();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		final FragmentManager supportFM = getSupportFragmentManager();
		final int fmStackCount = supportFM.getBackStackEntryCount();
		if(fmStackCount > 0) {
			refreshActionBarNavigation(false, supportFM.getBackStackEntryAt(fmStackCount - 1).getBreadCrumbTitle());
		} else {
			refreshActionBarNavigation(true, null);
		}
	}

	@Override
	public ArrayList<String> getTabList() {
		return mTabList;
	}

	@Override
	public void pageChanged(int position) {
		if(actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_STANDARD)
			actionBar.setSelectedNavigationItem(position);
	}
	
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		final MPDApplication app = (MPDApplication) getApplicationContext();
		switch (event.getKeyCode()) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			new Job() {
				@Override
				public void run() {
					try {
						app.oMPDAsyncHelper.oMPD.next();
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			};
			return true;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			new Job() {
				@Override
				public void run() {
					try {
						app.oMPDAsyncHelper.oMPD.previous();
					} catch (MPDServerException e) {
						Log.w(e);
					}
				}
			};
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
				new Job() {
					@Override
					public void run() {
						try {
							app.oMPDAsyncHelper.oMPD.adjustVolume(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ? NowPlayingFragment.VOLUME_STEP
									: -NowPlayingFragment.VOLUME_STEP);
						} catch (MPDServerException e) {
							Log.w(e);
						}
					}
				};
			}
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
}

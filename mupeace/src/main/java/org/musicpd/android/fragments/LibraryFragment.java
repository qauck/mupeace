package org.musicpd.android.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.musicpd.android.R;
import org.musicpd.android.library.ILibraryTabActivity;
import org.musicpd.android.tools.LibraryTabsUtil;
import org.musicpd.android.tools.Tools;

public class LibraryFragment extends Fragment/*SherlockFragment*/ {
	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which will keep every loaded fragment in memory. If this becomes too
	 * memory intensive, it may be best to switch to a {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter sectionsPagerAdapter = null;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager viewPager = null;
	ILibraryTabActivity activity = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.library_tabs_fragment, container, false);
		viewPager = (ViewPager) view;
		if (sectionsPagerAdapter != null)
			viewPager.setAdapter(sectionsPagerAdapter);
		viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (activity != null)
					activity.pageChanged(position);
			}
		});
		return view;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (!(activity instanceof ILibraryTabActivity)) {
			throw new RuntimeException("Error : LibraryFragment can only be attached to an activity implementing ILibraryTabActivity");
		}
		this.activity = (ILibraryTabActivity) activity;
		sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager());
		if (viewPager != null)
			viewPager.setAdapter(sectionsPagerAdapter);
	}

	public void setCurrentItem(int item, boolean smoothScroll) {
		if (viewPager != null)
			viewPager.setCurrentItem(item, smoothScroll);
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary sections of the app.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {
		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			return (Fragment)Tools.instantiate(LibraryTabsUtil.getClass(getActivity(), activity.getTabList().get(i)));
		}

		@Override
		public int getCount() {
			return activity.getTabList().size();
		}

	}
}

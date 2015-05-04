package org.musicpd.android.library;

import java.util.ArrayList;
import java.util.List;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.text.Editable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.TextView;

import org.musicpd.android.MPDApplication;
import org.musicpd.android.R;
import org.musicpd.android.R.id;
import org.musicpd.android.R.layout;
import org.musicpd.android.R.string;
import org.musicpd.android.adapters.SeparatedListAdapter;
import org.musicpd.android.adapters.SeparatedListDataBinder;
import org.musicpd.android.views.TouchInterceptor;
import org.musicpd.android.tools.LibraryTabsUtil;

class TabItem {
	String text;

	TabItem(String text) {
		this.text = text;
	}
}

public class LibraryTabsSettings extends PreferenceActivity {

	private SeparatedListAdapter adapter;
	private ArrayList<Object> tabList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.library_tabs_settings);
		final Context context = this.getApplicationContext();

		// get a list of all tabs
		final ArrayList<String> allTabs = LibraryTabsUtil.getAllLibraryTabs();

		// get a list of all currently visible tabs
		ArrayList<String> currentTabs = LibraryTabsUtil
				.getCurrentLibraryTabs(context);

		// create a list of all currently hidden tabs
		ArrayList<String> hiddenTabs = new ArrayList<String>();
		for (String tab : allTabs) {
			// add all items not in currentTabs
			if (!currentTabs.contains(tab)) {
				hiddenTabs.add(tab);
			}
		}

		tabList = new ArrayList<Object>();
		// add a separator
		tabList.add(getString(R.string.visibleTabs));
		// add all visible tabs
		for (int i = 0; i < currentTabs.size(); i++) {
			tabList.add(new TabItem(currentTabs.get(i)));
		}
		// add a separator
		tabList.add(getString(R.string.hiddenTabs));
		// add all hidden tabs
		for (int i = 0; i < hiddenTabs.size(); i++) {
			tabList.add(new TabItem(hiddenTabs.get(i)));
		}
		adapter = new SeparatedListAdapter(this,
				R.layout.library_tabs_settings_item, new TabListDataBinder(),
				tabList);

		setListAdapter(adapter);
		ListView mList;
		mList = getListView();
		((TouchInterceptor) mList).setDropListener(mDropListener);
		mList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final TabItem item = (TabItem)adapter.getItem(position);
				if (allTabs.indexOf(item.text) >= 0) {
					final EditText input = new EditText(LibraryTabsSettings.this);
					input.setText(LibraryTabsUtil.getTabTitle(context, item.text).getString(context));
					new AlertDialog.Builder(LibraryTabsSettings.this)
						.setTitle("Rename " + item.text)
						.setMessage("")
						.setView(input)
						.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								Editable value = input.getText();
								LibraryTabsUtil.setTabName(context, item.text, value.toString());
							}
						}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
							}
						}).show();
				}

			}
		});
	}

	@Override
	protected void onStart() {
		super.onStart();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.setActivity(this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		MPDApplication app = (MPDApplication) getApplicationContext();
		app.unsetActivity(this);
	}

	private ArrayList<String> getVisibleTabs() {
		ArrayList<String> visibleTabs = new ArrayList<String>();
		// item 0 is a separator so we start with 1
		for (int i = 1; i < tabList.size(); i++) {
			// if the item is a separator break
			if (tabList.get(i) instanceof String) {
				break;
			}
			// if item is a TabItem add it to the list
			if (tabList.get(i) instanceof TabItem) {
				visibleTabs.add(((TabItem) tabList.get(i)).text);
			}
		}
		return visibleTabs;
	}

	private void saveSettings() {
		LibraryTabsUtil.saveCurrentLibraryTabs(this.getApplicationContext(),
				getVisibleTabs());
	}

	public TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {

		public void drop(int from, int to) {
			if (from == to) {
				return;
			}
			Object item = tabList.get(from);
			tabList.remove(from);
			tabList.add(to, item);
			if (getVisibleTabs().size() == 0) {
				// at least one tab should be visible so revert the changes
				tabList.remove(to);
				tabList.add(from, item);
			} else {
				saveSettings();
				adapter.notifyDataSetChanged();
			}
		}
	};

}

class TabListDataBinder implements SeparatedListDataBinder {

	public void onDataBind(Context context, View targetView,
			List<Object> items, Object item, int position) {
		final TextView text1 = (TextView) targetView.findViewById(R.id.text1);
		text1.setText(LibraryTabsUtil.getTabTitle(context, ((TabItem) item).text).getString(context));
	}

	public boolean isEnabled(int position, List<Object> items, Object item) {
		return true;
	}

}
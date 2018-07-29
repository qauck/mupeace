package org.musicpd.android;

import org.a0z.mpd.exception.MPDServerException;
import org.musicpd.android.MPDActivities.MPDListActivity;
import org.musicpd.android.R;
import org.musicpd.android.tools.Log;


import android.content.Context;
import android.content.Intent;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.Menu;
import android.view.MenuItem;

public class InformationActivity extends MPDListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.simple_list);

		try
		{
			Bundle extras = getIntent().getExtras();
			if (extras != null) {
				final MPDApplication app = (MPDApplication)getApplication();

				final String[] matrix  = { BaseColumns._ID, "key", "value" };
				final String[] columns = { "key", "value" };
				final int[] layouts = { R.id.text1, R.id.text2 };

				MatrixCursor cursor = null;

				for (int attempts = 0; attempts < 5; attempts++)
					try {
						if (cursor != null)
							cursor.close();

						cursor = new MatrixCursor(matrix);
						int i, j, id = 0;
						String[] args;
						for (String line :
							(args = extras.getStringArray("query")) == null
								? app.oMPDAsyncHelper.oMPD.getPlaylistSongInfo(extras.getInt("song"))
								: app.oMPDAsyncHelper.oMPD.findInfo(args))
							if (line != null && (i = line.indexOf(": ")) >= 0) {
								String key = line.substring(0, i);
								i += 2;
								if ("Last-Modified".equals(key) || "AlbumArtist".equals(key)) {
								} else if ("file".equals(key)) {
									String last = line.substring(j = Math.max(i, line.lastIndexOf('/') + 1));
									cursor.addRow(new Object[] { id++, line.substring(Math.max(i, line.lastIndexOf('/', j - 2) + 1), Math.max(i, j - 1)), last });
								} else
									cursor.addRow(new Object[] { id++, key, line.substring(i) });
							}

						this.setListAdapter(
								new SimpleCursorAdapter(
									this,
									R.layout.double_wrapped_list_item,
									cursor,
									columns,
									layouts,
									0)
							);

						return;
					} catch (MPDServerException e) {
						Log.w(e);

						if (e.getMessage().endsWith("incorrect arguments"))
							break;

						try {
							Thread.sleep(200);
						} catch (InterruptedException e1) { }
					}
			}
		} catch (Exception e) {
			Log.e(e);
		}

		finish();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.information, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.showRelated) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public static void start(Context context, String[] args) {
		Intent intent = new Intent(context, InformationActivity.class);
		intent.putExtra("query", args);
		context.startActivity(intent);
	}

	public static void start(Context context, int SongId) {
		Intent intent = new Intent(context, InformationActivity.class);
		intent.putExtra("song", SongId);
		context.startActivity(intent);
	}
}
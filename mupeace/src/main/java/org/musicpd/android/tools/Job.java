package org.musicpd.android.tools;

public abstract class Job implements Runnable {
	public Job() {
		new android.os.AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				run();
				return null;
			}
		}.execute();
	}
}

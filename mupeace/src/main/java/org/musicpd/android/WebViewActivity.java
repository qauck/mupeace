package org.musicpd.android;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;


public class WebViewActivity extends AppCompatActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle(R.string.donate);
		WebView webview = new WebView(this);
		webview.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		this.setContentView(webview);
		final String url = getIntent().getStringExtra("url");
		if (url != null) {
			webview.loadUrl(url);
		} else {
			// Defaut on the what's new page
			webview.loadUrl("http://nlss.fr/mpdroid/donate.html");
		}
	}
}

package org.musicpd.android.fragments;

import org.musicpd.android.tools.Log;

import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.actionbarsherlock.app.SherlockFragment;

public class GestureFragment extends SherlockFragment {
    private static final int SWIPE_DISTANCE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

	public void onSwipeLeft() {
		if (listener != null)
			listener.onSwipeLeft();
	}

	public void onSwipeRight() {
		if (listener != null)
			listener.onSwipeRight();
	}

	public interface Listener {
		public void onSwipeLeft();
		public void onSwipeRight();
	}

	Listener listener;
	public void setGestureFragmentListener(Listener listener) {
		this.listener = listener;
	}

	GestureDetectorCompat detector;
	public boolean onTouchEvent(View v, MotionEvent event) {
		if (v == null && event.getAction() != MotionEvent.ACTION_DOWN)
			return false;
		return detector.onTouchEvent(event);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		detector = new GestureDetectorCompat(getActivity(), new SimpleOnGestureListener() {
	        @Override
	        public boolean onDown(MotionEvent e) {
	            return true;
	        }

	        @Override
	        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	            float distanceX = e2.getX() - e1.getX();
	            float distanceY = e2.getY() - e1.getY();
	            if (Math.abs(distanceX) > Math.abs(distanceY)
            		&& Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD
            		&& Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
	                if (distanceX > 0)
	                    onSwipeRight();
	                else
	                    onSwipeLeft();
	                return true;
	            }
	            return false;
	        }
		});
		view.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				detector.onTouchEvent(event);
				return false;
			}
		});
    }
}

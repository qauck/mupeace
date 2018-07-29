package org.a0z.mpd.event;

public interface ClientActionListener {
	/**
	 * Called when the client adjusts the volume.
	 * 
	 * @param newVolume
	 *           target volume
	 * 
	 * @param oldVolume
	 *           volume before adjustment
	 */
	void volumeAdjusted(int newVolume, int oldVolume);
}

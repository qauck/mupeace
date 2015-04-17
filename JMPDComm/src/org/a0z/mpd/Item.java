package org.a0z.mpd;

import java.text.CollationKey;
import java.text.Collator;

public abstract class Item implements Comparable<Item> {
	public String mainText() {
		return getName();
	}
	public String subText() {
		return null;
	}
	public String sort() {
		return mainText();
	}
	abstract public String getName();

	public static final Collator collator = Collator.getInstance();
	static {
		collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
		collator.setStrength(Collator.SECONDARY);
	}
	private CollationKey _key;
	private CollationKey key() {
		if (_key == null)
			_key = collator.getCollationKey(sort());
		return _key;
	}

	@Override
	public int compareTo(Item o) {
		return key().compareTo(o.key());
	}

	@Override
	public	String toString() {
		return mainText();
	}
}

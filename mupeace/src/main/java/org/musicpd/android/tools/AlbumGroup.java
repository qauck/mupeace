package org.musicpd.android.tools;

import org.a0z.mpd.Album;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


public class AlbumGroup extends Album implements java.io.Serializable {

	protected Pattern catalogue = Pattern.compile("^(.*?)(?:,? +[(]([^()]+)[)])$");

	private final List<Album> as;

	public AlbumGroup(String display, List<Album> as) {
		super(display);
		this.as = as;
	}


	public List<Album> albums() {
		List<Album> list = new ArrayList<>();
		for (Album ab : as) {
			list.add(new Album(ab.getName().replaceAll("^(.*?)(?:,? +[(]([^()]+)[)])$", "$2, $1")));
		}
		return list;
	}

	public Album get(int i) {
		return as.get(i);
	}

	protected static final String recordings = "^(.+?)(?i: music| works?| pieces?)?(?:, +[IVXivx\\d/:-]*[IVXivx/:-][IVXivxa\\d/:-]*\\b|,? *\\b(?:[Vv]ol\\.?|No\\.?|[Oo]p(?:\\.|us)?|Book|BB|Sz\\.?) +[IVXivxa\\d/:-]+(.*?))*(?:,? +[(]([^()]+)[)])*$";
	protected static final String punct = "(?:\\W*[(][^()]+[)])*\\W+";
	protected static final String numbers = "\\b(?:(0|zero|zero)|(1|one|un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)|(12|twelve|douze)|(13|thirteen|treize)|(14|fourteen|quatorze)|(15|fifteen|quinze)|(16|sixteen|seize)|(17|seventeen|dix-sept)|(18|eighteen|dix-huit)|(19|nineteen|dix-neuf)|(?:(20|twenty|vingt)|(30|thirty|trente)|(40|fourty|quarante)|(50|fifty|cinquante)|(60|sixty|soixante)|(70|seventy|soixante-dix)|(80|eighty|quatre-vingts)|(90|ninety|quatre-vingt-dix))(?: (?:(0|zero|zero)|(1|one|(?:et )?un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)))?|(\\d+))\\b".replace(" ", "\\\\W+");
	protected static final String number_initial = "\\A(?:(0|zero|zero)|(1|one|un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)|(12|twelve|douze)|(13|thirteen|treize)|(14|fourteen|quatorze)|(15|fifteen|quinze)|(16|sixteen|seize)|(17|seventeen|dix-sept)|(18|eighteen|dix-huit)|(19|nineteen|dix-neuf)|(?:(20|twenty|vingt)|(30|thirty|trente)|(40|fourty|quarante)|(50|fifty|cinquante)|(60|sixty|soixante)|(70|seventy|soixante-dix)|(80|eighty|quatre-vingts)|(90|ninety|quatre-vingt-dix))(?: (?:(0|zero|zero)|(1|one|(?:et )?un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)))?|(\\d+))\\b".replace(" ", "\\\\W+");

	public static String number_chuck(String x) {
		if (x == null || x.isEmpty() || x.charAt(0) < '0' || x.charAt(0) > '9') {
			return x;
		} else {
			return "ZZ" + x;
		}
	}

	public static List<Album> items(Iterable<Album> xs, boolean isLong) {
		List<Album> list = new ArrayList<>();
		for (Album ab : xs) {
			list.add(ab);
		}
		return list;
	}

}

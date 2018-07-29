package org.musicpd.android.tools;

import org.a0z.mpd.exception.MPDServerException;

import java.util.ConcurrentModificationException;

public class Utils {

//	implicit def elvisOperator[T](alt: =>T) = new {
//		def ?:[A >: T](pred: A) = if (pred == null) alt else pred
//	}
//	def mapMaybe[A,B](list: List[A])(f: A => B): Traversable[B] = {
//		mapMaybe(list.view)(f)
//	}
//	def mapMaybe[A,B](xs: Traversable[A])(f: A => B) = {
//		xs.map(f(_)).filter(_ != null)
//	}
//
//	implicit def traversableToGroupByOrderedImplicit[A](t: Traversable[A]) = new {
//		def groupByOrdered[K](f: A => K)(implicit ord: Ordering[K]): collection.SortedMap[K, List[A]] = {
//		    var map = collection.SortedMap[K,List[A]]()
//		    for (i <- t) {
//		    	val key = f(i)
//    			map = map + ((key, i :: map.lift(key).getOrElse(List[A]())))
//		    }
//		    map
//		}
//
//		def groupByStable[K](f: A => K): LinkedHashMap[K, List[A]] = {
//		    val map = LinkedHashMap[K,List[A]]()
//		    for (i <- t) {
//		    	val key = f(i)
//    			map(key) = i :: map.lift(key).getOrElse(List[A]())
//		    }
//		    map
//		}
//	}

	public static void retry(Function.MPDAction0 f) {
		int retry = 0;
		while (retry <= 3) {
			try {
				f.apply();
				return;
			} catch (MPDServerException e) {
				Log.w(e);
				retry += 1;
				try {
					Thread.sleep(1000 ^ retry);
				} catch (Throwable t) {
				}
			} catch (ConcurrentModificationException e) {
				Log.w(e);
			} catch (Throwable e) {
				Log.e(e);
				return;
			}
		}
	}
}

//package org.musicpd.android.tools;
//
//object RelatedSongs {
//  protected val dir = new scala.util.matching.Regex("""^(.*)/(?:\\/|[^/]*)$""")
//
//  def items(mpd : MPD, xs : java.util.List[Music]): java.util.List[Music] = {
//    new java.util.ArrayList(items(mpd, xs.asScala).toList.asJava)
//  }
//  protected def items(mpd : MPD, xs : Seq[Music]) = {
//    var selected = xs.toSet
//    xs
//    //.sortBy(x => x.getDisc() * 1000 + x.getTrack())
//    .groupByStable(x => dir.replaceAllIn(x.getFullpath(), "$1"))
//    .flatMap(g => {Log.i("Finding related in "+g._1); mpd.getSongs(g._1).asScala})
//    .filterNot(x => x == null || x.hashCode() == 0)
//    .map(x => x.setSelected(selected.contains(x)))
//    /*.foldLeft(collection.mutable.LinkedHashSet[Music]())((s,g) =>
//      s ++ mpd.getSongs(g._1).asScala
//    )*/
//  }
//}

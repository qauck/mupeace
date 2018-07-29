//package org.musicpd.android.tools;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.net.URL;
//import java.util.Arrays;
//
//public class YouTube {
//
//	public static URL resolve(String idOrUrl)  {
//            Log.v("Resolving YouTube data " + idOrUrl);
//		String id = idOrUrl.replaceAll("[^?]+\\?|.*youtu\\.be/|.*[/&?]v=|&[^=]*(?<!&v)=[^&]*(&|$)", "");
//		InputStream is = null;
//		String[] info;
//		try {
//			is = new java.net.URL("http://www.youtube.com/get_video_info?video_id=" + id + "&asv=2").
//					openConnection().getInputStream();
//			val info = scala.io.Source.fromInputStream(
//
//			).mkString("").split("&")
//		}catch (IOException e){
//			e.printStackTrace();
//		}
//
//	    Log.v(Arrays.toString(info));
//
//	    for {
//	        title <-
//                    info.find(_.startsWith("title=")) orElse
//	            Some("title=" + """:.*$""".r.replaceAllIn(idOrUrl, ""))
//	        urlmap <- info.find(_.startsWith("url_encoded_fmt_stream_map="))
//	        streams = java.net.URLDecoder.decode(urlmap.substring(27), "UTF-8").
//	            split(",").map(_.split("&"))
//            stream <-
//            	streams.find(_.exists(_.equals("quality=hd720"))) orElse
//            	streams.find(_.exists(_.startsWith("quality=hd"))) orElse
//            	streams.find(_.exists(_.startsWith("quality="))) orElse
//            	streams.find(!_.isEmpty)
//	        url <- stream.find(_.startsWith("url="))
//	        sig <- stream.find(_.startsWith("sig=")) orElse Some(null)
//	    } yield (
//	        java.net.URLDecoder.decode(title.substring(6), "UTF-8"),
//	        java.net.URLDecoder.decode(url.substring(4), "UTF-8") + (if (sig == null) "" else "&signature=" + sig.substring(4))
//        )}.get
//}

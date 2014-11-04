# Mupeace

**Mupeace** is an Android Music Player Client (/ˈmjuːˌpiːs/, from MPC with pronounceability vowels) for MPD, the Music Player Daemon. It is a fork of MPDroid and, in turn, [PMix](http://code.google.com/p/pmix/). This fork retains Gingerbread compatibility, is active at squashing bugs, especially crashes, and adds features to aid navigation of large jazz or classical collections.

You can browse your library, control the current song and playlist, manage your outputs and stream music to your phone. Switch between different servers, say, in different rooms, easily with Zeroconf auto-discovery and share from the YouTube application to queue the audio for your stereo.


## Updates

Automatic, often unstable builds are available from [downloads](https://github.com/eisnerd/mupeace/commits/downloads), the latest apk always being available from [Mupeace-debug.apk](https://github.com/eisnerd/mupeace/raw/downloads/Mupeace-debug.apk).

You may want to join the [Mupeace Beta](https://plus.google.com/u/0/communities/115435517365221313224) group to receive news and automatic updates of experimental work. For the most stable releases, see [Google Play](https://play.google.com/store/apps/details?id=org.musicpd.android). You can check the version number there and find it in [downloads](https://github.com/eisnerd/mupeace/commits/downloads) if you prefer not to use the store

![Now Playing/library Screenshot](https://raw.github.com/eisnerd/dmix/master/Screenshots/readme.png)


## Compatibility

Mupeace works on all devices from 2.2 to 5.0 encountered so far.

Large tablets are supported with a specially adjusted layout.


## Libraries used

 - ActionBarSherlock. A wonderful library backporting most Holo elements to Android 2.x
 - JMPDComm. The core MPD protocol client library, heavily modified by https://github.com/abarisain/dmix
 - LastFM-java. Last.FM cover art support
 - [mdnsjava](https://code.google.com/p/mdnsjava/). A complete mDNS/DNS-SD/Zeroconf/avahi/Bonjour library for discovering MPD servers


## Roadmap

 - [ ] Improved server discovery reliability
 - [ ] Improved reconnection behaviour for temporary network issues
 - [x] Reduced use of modal UI through sliding panels and the navigation drawer
 - [ ] Further UI modernisation
 - [ ] Protocol library overhaul for performance
 - [ ] General design revision


## Credit

Nearly everything you can find in Mupeace was developed as part of MPDroid or its predecessors, so please take note of the information at [abarisain/dmix](https://github.com/abarisain/dmix#special-thanks) and [contributors](https://github.com/abarisain/dmix/graphs/contributors) to know who to thank.

My thanks go to them, [@abarisain](https://github.com/abarisain) in particular, and to those who have helped out with bug reports and investigations.


<img src="/../media/screenshot-sony-xperia-t3.png?raw=true"
     title="A screenshot of the app" width="30%" align="left" hspace="10">

SnapScore is an Android app that lets you take a photo of your game board after playing
[*Take it Easy!*][1] and will calculate the score for you.  There's a beta version
available on [Google Play][2].

## TODO

*   On a Moto G4 Plus (Android 7.0, API 24) the photos we take are all available from
    Google Photos.  That's not supposed to happen.  Try using [`CaptureRequest`][3]?
*   We crash on my Sony Xperia T3 D5103 (Android 4.4.4) after taking a photo when
    replacing `getExternalCacheDir()` with `getCacheDir()`.  Why?
*   Link to a webpage that shows which *Take it Easy!* versions are supported when telling
    people to check whether theirs is.

[1]: http://www.burleygames.com/board-games/take-it-easy/
[2]: https://play.google.com/store/apps/details?id=xyz.meribold.snapscore
[3]: https://developer.android.com/reference/android/hardware/camera2/CaptureRequest

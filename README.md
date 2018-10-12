# SnapScore

The SnapScore Android app.  When it will be right, I don't know.  What it will be like, I
don't know.

## TODO

*   On a Moto G4 Plus (Android 7.0, API 24) the photos we take are all available from
    Google Photos.  That's not supposed to happen.  Try using [`CaptureRequest`][1]?
*   We crash on my Sony Xperia T3 D5103 (Android 4.4.4) after taking a photo when
    replacing `getExternalCacheDir()` with `getCacheDir()`.  Why?

[1]: https://developer.android.com/reference/android/hardware/camera2/CaptureRequest

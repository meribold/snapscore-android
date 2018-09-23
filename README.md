# SnapScore

The SnapScore Android app.  I mean, that's what will be here once I wrote it.

## TODO

*  Why is my *Kotlin* code in a subdirectory called `java`?  Is that necessary?
*  [Load photos efficiently.][1]
*  On a Moto G4 Plus (Android 7.0, API 24) the photos we take are all available from
   Google Photos.  That's not supposed to happen.
*  The `ImageView` doesn't show any photo after returning from the camera app on a Samsung
   Galaxy S4 GT-I9500 with Android 5.0.1 (API 21).
   *  I think this error message points out the reason: `W/OpenGLRenderer: Bitmap too
      large to be uploaded into a texture (2322x4128, max=4096x4096)`.
*  We crash on my Sony Xperia T3 D5103 (Android 4.4.4) after taking a photo when replacing
   `getExternalCacheDir()` with `getCacheDir()`.  Why?

[1]: https://developer.android.com/topic/performance/graphics/load-bitmap

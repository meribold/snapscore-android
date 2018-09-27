package xyz.meribold.snapscore

import android.app.Activity
import android.os.AsyncTask
import java.io.File
import java.lang.ref.WeakReference
import java.io.IOException
import java.net.Socket
import java.net.UnknownHostException
import kotlinx.android.synthetic.main.activity_main.*

class NetworkTask(val actRef: WeakReference<Activity>) : AsyncTask<File, Unit, Int>() {
    // This runs in the UI thread.
    override fun onPreExecute() {}

    // This runs in the background thread.
    override fun doInBackground(vararg image: File): Int? {
        // Create a TCP socket and connect.
        try {
            val socket = Socket("192.168.168.75", 50007)
        } catch (e: UnknownHostException) {
            return -1
        } catch (e: IOException) {
            return -2
        } catch (e: SecurityException) {
            return -3
        } catch (e: IllegalArgumentException) {
            return -4
        }
        return 42
    }

    // This runs in the UI thread and gets the result of the background task.
    override fun onPostExecute(score: Int?) {
        actRef.get()?.scoreTV?.text = "$score"
    }
}

// [1]: https://developer.android.com/training/basics/network-ops/connecting
// [2]: https://developer.android.com/guide/components/processes-and-threads
// [3]: https://developer.android.com/topic/performance/threads
// [4]: https://developer.android.com/reference/android/os/AsyncTask
// [5]: https://developer.android.com/reference/kotlin/android/os/AsyncTask
// [6]: https://codereview.stackexchange.com/q/175340
//      "Updating the UI without leaking the context after an Android AsyncTask finishes"
// [7]: https://developer.android.com/reference/java/lang/ref/WeakReference

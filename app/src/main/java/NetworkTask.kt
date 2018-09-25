package xyz.meribold.snapscore

import android.content.Context
import android.os.AsyncTask
import android.widget.Toast
import java.io.File
import java.lang.ref.WeakReference

class NetworkTask(val contextRef: WeakReference<Context>) : AsyncTask<File, Unit, Int>() {
    // This runs in the UI thread.
    override fun onPreExecute() {}

    // This runs in the background thread.
    override fun doInBackground(vararg image: File): Int? {
        Thread.sleep(5000)
        return 42
    }

    // This runs in the UI thread and gets the result of the background task.
    override fun onPostExecute(score: Int?) {
        contextRef.get()?.let {
            val text = if (score != null) {
                "Your score is $score."
            } else {
                "Something went wrong."
            }
            Toast.makeText(it.applicationContext, text, Toast.LENGTH_LONG).show()
        }
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

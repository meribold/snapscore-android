package xyz.meribold.snapscore

import android.app.Activity
import android.os.AsyncTask
import android.widget.Toast
import java.io.*
import java.lang.ref.WeakReference
import java.net.Socket
import java.net.UnknownHostException
import kotlinx.android.synthetic.main.activity_main.*

class NetworkTask(val actRef: WeakReference<Activity>) : AsyncTask<File, Unit, Int>() {
    // This runs in the UI thread.
    override fun onPreExecute() {}

    // This runs in the background thread.
    override fun doInBackground(vararg params: File): Int? {
        if (params.size != 1) {
            return -1
        }
        val image: File = params[0]
        // Create a TCP socket and connect.
        val socket = try {
            Socket("snapscore.meribold.xyz", 50007)
        } catch (e: UnknownHostException) {
            return -2
        } catch (e: IOException) {
            return -3
        } catch (e: SecurityException) {
            return -4
        } catch (e: IllegalArgumentException) {
            return -5
        }

        val oStream = try {
            DataOutputStream(socket.getOutputStream())
        } catch (e: IOException) {
            return -6
        }

        val iStream = try {
            DataInputStream(socket.getInputStream())
        } catch (e: IOException) {
            return -7
        }

        oStream.writeInt(image.length().toInt())
        val imageStream = FileInputStream(image)
        val buffer = ByteArray(4096)
        var numBytesRead: Int
        while (true) {
            numBytesRead = imageStream.read(buffer)
            if (numBytesRead == -1) {
                break
            }
            oStream.write(buffer, 0, numBytesRead)
        }
        oStream.flush()

        val score: Int = try {
            iStream.readInt()
        } catch (e: EOFException) {
            return -8
        } catch (e: IOException) {
            return -9
        }

        // "Closing the [...] OutputStream will close the associated socket." [8].
        oStream.close()
        iStream.close()
        imageStream.close()

        return score
    }

    // This runs in the UI thread and gets the result of the background task.
    override fun onPostExecute(score: Int?) {
        actRef.get()?.let {
            if (score == null || score < 0) {
                Toast.makeText(it.applicationContext, "Something went wrong.",
                               Toast.LENGTH_LONG).show()
                if (score != null) {
                    it.scoreTV?.text = "?".repeat(-score)
                } else {
                    it.scoreTV?.text = "?!"
                }
            } else {
                it.scoreTV?.text = "$score"
            }
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
// [8]: https://developer.android.com/reference/kotlin/java/net/Socket#getOutputStream%28%29

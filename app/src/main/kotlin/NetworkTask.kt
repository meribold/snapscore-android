package xyz.meribold.snapscore

import android.app.Activity
import android.os.AsyncTask
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import java.io.*
import java.lang.ref.WeakReference
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.roundToInt
import kotlinx.android.synthetic.main.activity_main.*

class NetworkTask(val actRef: WeakReference<Activity>) : AsyncTask<File, Int, Int>() {
    // This runs in the UI thread.
    override fun onPreExecute() {
        actRef.get()?.progressBar?.visibility = VISIBLE
    }

    // This runs in the background thread.
    override fun doInBackground(vararg params: File): Int? {
        if (params.size != 1) {
            return -1
        }
        val image: File = params[0]

        // Create a TCP socket that uses SSL/TLS and connect to "snapscore.meribold.xyz".
        // Based on [10].
        val socket: SSLSocket = try {
            SSLSocketFactory.getDefault().run {
                // This is the `createSocket` member function inherited from
                // `SocketFactory` [11].
                createSocket("snapscore.meribold.xyz", 50007) as SSLSocket
            }
        } catch (e: UnknownHostException) {
            return -2
        } catch (e: IOException) {
            return -3
        } catch (e: SecurityException) {
            return -4
        } catch (e: IllegalArgumentException) {
            return -5
        }
        // Verify that the certificate we got is actually for "snapscore.meribold.xyz", I
        // think.
        HttpsURLConnection.getDefaultHostnameVerifier().run {
            if (!verify("snapscore.meribold.xyz", socket.session)) {
                return -10
            }
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

        val numBytes: Int = image.length().toInt()
        try {
            oStream.writeInt(numBytes)
        } catch (e: IOException) {
            return -11
        }

        val imageStream = FileInputStream(image)
        val buffer = ByteArray(4096)
        var totalBytesRead: Int = 0
        var progress: Int = 0
        publishProgress(0)
        while (true) {
            val numBytesRead: Int = imageStream.read(buffer)
            if (numBytesRead == -1) {
                break
            }
            try {
                oStream.write(buffer, 0, numBytesRead)
            } catch (e: IOException) {
                return -12
            }
            totalBytesRead += numBytesRead
            val new_progress = (100.0f * totalBytesRead / numBytes).roundToInt()
            if (new_progress != progress) {
                progress = new_progress
                // "This method may take several seconds to complete [...]." [9]  What?!
                publishProgress(progress)
            }
        }
        oStream.flush()
        publishProgress(-1)

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

    override fun onProgressUpdate(vararg values: Int?) {
        val progress = values[0] ?: return
        actRef.get()?.progressBar?.apply {
            when (progress) {
                0 -> setIndeterminate(false)
                -1 -> setIndeterminate(true)
            }
            setProgress(progress)
        }
    }

    override fun onCancelled(score: Int?) {
        actRef.get()?.progressBar?.visibility = GONE
    }

    // This runs in the UI thread and gets the result of the background task.
    override fun onPostExecute(score: Int?) {
        actRef.get()?.let {
            it.progressBar?.visibility = GONE
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
// [9]: https://developer.android.com/reference/android/os/AsyncTask.html#publishProgress(Progress...)
// [10]: https://developer.android.com/training/articles/security-ssl#WarningsSslSocket
// [11]: https://developer.android.com/reference/kotlin/javax/net/SocketFactory#createsocket_1

package xyz.meribold.snapscore

import android.os.AsyncTask
import java.io.*
import java.lang.ref.WeakReference
import java.net.UnknownHostException
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.math.roundToInt

class NetworkTask(
    private val viewModelRef: WeakReference<MainViewModel>
) : AsyncTask<File, Int, Int>() {

    // This runs in the UI thread.
    override fun onPreExecute() {
        viewModelRef.get()?.scoringPhase?.value = ScoringPhase.CONNECTING
    }

    // This runs in the background thread.  TODO: it would be good to check the value of
    // `AsyncTask.isCancelled()` periodically.
    override fun doInBackground(vararg params: File): Int? {
        if (params.size != 1) {
            return -1
        }
        val image: File = params[0]

        // Create a TCP socket that uses SSL/TLS and connect to "snapscore.meribold.xyz".
        // Based on [9].
        val socket: SSLSocket = try {
            SSLSocketFactory.getDefault().run {
                // This is the `createSocket` member function inherited from
                // `SocketFactory` [10].
                createSocket("snapscore.meribold.xyz", 50007) as SSLSocket
            }
        } catch (e: UnknownHostException) {
            return -2
        } catch (e: IOException) {
            return -100
        } catch (e: SecurityException) {
            return -3
        } catch (e: IllegalArgumentException) {
            return -4
        }

        // The socket will be closed when this scope is exited.
        socket.use {
            // Verify that the certificate we got is actually for
            // "snapscore.meribold.xyz", I think.
            HttpsURLConnection.getDefaultHostnameVerifier().run {
                if (!verify("snapscore.meribold.xyz", socket.session)) {
                    return -10
                }
            }

            // "Closing [the] socket will also close the socket's `InputStream` and
            // `OutputStream`." [8]
            val oStream = try {
                DataOutputStream(socket.outputStream)
            } catch (e: IOException) {
                return -101
            }
            val iStream = try {
                DataInputStream(socket.inputStream)
            } catch (e: IOException) {
                return -102
            }

            val numBytes: Int = image.length().toInt()
            try {
                oStream.writeInt(numBytes)
            } catch (e: IOException) {
                return -103
            }

            FileInputStream(image).use { imageStream ->
                val buffer = ByteArray(4096)
                var totalBytesRead = 0
                var progress = 0
                viewModelRef.get()?.let {
                    it.scoringPhase.postValue(ScoringPhase.UPLOADING)
                    it.progress.postValue(progress)
                }
                while (true) {
                    val numBytesRead: Int = imageStream.read(buffer)
                    if (numBytesRead == -1) {
                        break
                    }
                    try {
                        oStream.write(buffer, 0, numBytesRead)
                    } catch (e: IOException) {
                        return -104
                    }
                    totalBytesRead += numBytesRead
                    val newProgress = (100.0f * totalBytesRead / numBytes).roundToInt()
                    if (newProgress != progress) {
                        progress = newProgress
                        viewModelRef.get()?.progress?.postValue(progress)
                    }
                }
            }
            oStream.flush()
            viewModelRef.get()?.scoringPhase?.postValue(ScoringPhase.AWAITING)

            return try {
                iStream.readInt()
            } catch (e: EOFException) {
                -5
            } catch (e: IOException) {
                -105
            }
        }
    }

    // This runs in the UI thread.
    override fun onCancelled(score: Int?) {
        viewModelRef.get()?.let { model ->
            model.score.value = null
            model.scoringPhase.value = ScoringPhase.INACTIVE
        }
    }

    // This runs in the UI thread and gets the result of the background task.
    override fun onPostExecute(score: Int?) {
        viewModelRef.get()?.let { model ->
            model.score.value = score ?: -100
            model.scoringPhase.value = ScoringPhase.INACTIVE
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
// [8]: https://developer.android.com/reference/kotlin/java/net/Socket#close()
// [9]: https://developer.android.com/training/articles/security-ssl#WarningsSslSocket
// [10]: https://developer.android.com/reference/kotlin/javax/net/SocketFactory#createsocket_1

package xyz.meribold.snapscore

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.os.AsyncTask
import java.io.File
import java.lang.ref.WeakReference

enum class ScoringPhase {
    INACTIVE, CONNECTING, UPLOADING, AWAITING
}

// This class is all about starting an `AsyncTask` for scoring and updating the
// `ProgressBar` and `TextView` of the `MainActivity`.  It needs to exist because Android
// "may decide to destroy or re-create a UI controller in response to certain user actions
// or device events that are completely out of [our] control." [2]  If that were not the
// case, the `AsyncTask` task could just hold a reference to the activity and update it
// directly.  Woe is me.
class MainViewModel() : ViewModel() {
    val scoringPhase: MutableLiveData<ScoringPhase> = MutableLiveData()
    val progress: MutableLiveData<Int> = MutableLiveData()
    val score: MutableLiveData<Int?> = MutableLiveData()
    val networkTask: MutableLiveData<NetworkTask> = MutableLiveData()

    fun kickOffScoring(photoFile: File) {
        networkTask.value?.cancel(true)
        networkTask.value = NetworkTask(WeakReference(this)).apply {
            execute(photoFile)
        }
    }
}

// [1]: https://developer.android.com/topic/performance/threads#persisting
// [2]: https://developer.android.com/topic/libraries/architecture/viewmodel
// [3]: https://developer.android.com/topic/libraries/architecture/livedata
// [4]: https://developer.android.com/reference/android/arch/lifecycle/ViewModel
// [5]: https://developer.android.com/reference/android/arch/lifecycle/LiveData
// [6]: https://developer.android.com/topic/libraries/architecture/saving-states
// [7]: https://proandroiddev.com/customizing-the-new-viewmodel-cf28b8a7c5fc

package xyz.meribold.snapscore

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog

class AuthorizeUploadDialogFragment : DialogFragment() {
    init {
        // See <https://stackoverflow.com/q/8906269>.
        setCancelable(false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        (activity!! as MainActivity).let { activity ->
            with(AlertDialog.Builder(activity)) {
                setTitle(R.string.authorize_upload_dialog_title)
                setMessage(R.string.inform_about_upload)
                setPositiveButton(R.string.authorize_upload) { _, _ ->
                    activity.getPreferences(Context.MODE_PRIVATE).edit()
                        .putBoolean("uploading_authorized", true)
                        .apply()
                    activity.model.kickOffScoring(activity.photoFile)
                }
                setNegativeButton(R.string.disallow_upload) { _, _ ->
                    activity.finish()
                }
                setCancelable(false)
                create()
            }
        }
}

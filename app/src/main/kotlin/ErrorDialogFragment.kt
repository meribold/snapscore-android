package xyz.meribold.snapscore

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog

class ErrorDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        with(AlertDialog.Builder(activity!!)) {
            setTitle(arguments!!.getInt("title"))
            setMessage(arguments!!.getInt("message"))
            // setPositiveButton("Dismiss") { _, _ -> }
            create()
        }
}

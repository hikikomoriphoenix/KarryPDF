package marabillas.loremar.andpdfapp

import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class UserInputDialog(
    context: Context,
    message: String,
    val inputDoneAction: (input: String) -> Unit,
    val validation: ((input: String) -> Boolean?)? = null
) {
    private var dialog: AlertDialog = {
        dialog = AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("OK", DialogInterface.OnClickListener(this::onOK))
            .setNegativeButton("Cancel", null)
            .create()
        dialog
    }()

    private val inputBox: EditText = {
        val inputBox = EditText(context)
        val params = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        inputBox.layoutParams = params
        inputBox.inputType = InputType.TYPE_CLASS_NUMBER
        inputBox
    }()

    init {
        dialog.setView(inputBox)
        dialog.show()
    }

    private fun onOK(dialog: DialogInterface, which: Int) {
        val input = inputBox.text
        if (input.isNotBlank()) {
            val valid = validation?.invoke(input.toString()) ?: true

            if (valid) inputDoneAction(input.toString())
        }
    }
}
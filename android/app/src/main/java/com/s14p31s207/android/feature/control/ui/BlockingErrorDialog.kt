package com.s14p31s207.android.feature.control.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.s14p31s207.android.data.comm.tcp.ServerMessage

@Composable
fun BlockingErrorDialog(
    error: ServerMessage.Error,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = error.errorName.ifBlank { "Error" })
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ErrorField(label = "Error Code", value = error.errorCode.toString())
                ErrorField(label = "Category", value = error.category)
                ErrorField(label = "Severity", value = error.severity)
                ErrorField(label = "Context", value = error.context)
                ErrorField(label = "Solution", value = error.solution)
                if (!error.requestId.isNullOrBlank()) {
                    ErrorField(label = "Request ID", value = error.requestId.orEmpty())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ErrorField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label)
        Text(text = value.ifBlank { "-" })
    }
}

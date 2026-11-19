package com.transvibe.app.presentation.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun RecoveryDialog(
    message: String,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "تنبيه استرداد" else "Recovery Notice") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "حسناً" else "OK")
            }
        }
    )
}

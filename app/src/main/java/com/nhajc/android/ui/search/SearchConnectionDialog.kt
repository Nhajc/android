package com.nhajc.android.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nhajc.android.data.model.SearchError
import com.nhajc.android.data.model.SearchViewModel

@Composable
internal fun SearchConnectionDialog(
    model: SearchViewModel,
    onDismiss: () -> Unit,
    onUseSample: () -> Unit,
    onConnect: (String, String, String) -> Unit
) {
    var address by rememberSaveable { mutableStateOf(model.connection.serverUrl) }
    var username by rememberSaveable { mutableStateOf(model.connection.username.orEmpty()) }
    // Credentials are intentionally not saved in the activity saved state.
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nguồn tìm kiếm") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Dùng dữ liệu mẫu để trải nghiệm ngay, hoặc đăng nhập tài khoản backend của bạn.")
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Địa chỉ API") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !model.isConnecting,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Text(
                    "Máy ảo Android dùng 10.0.2.2 để kết nối server trên máy tính. Điện thoại thật dùng IP LAN của máy tính.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Tên đăng nhập hoặc email") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !model.isConnecting
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Mật khẩu") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), enabled = !model.isConnecting,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                model.connectionError?.let { error ->
                    Text(
                        text = when (error) {
                            SearchError.UNAUTHORIZED -> "Tên đăng nhập hoặc mật khẩu chưa đúng."
                            SearchError.INVALID_QUERY -> "Kiểm tra lại thông tin đăng nhập."
                            else -> error.message()
                        },
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (model.isConnecting) CircularProgressIndicator()
                TextButton(onClick = onUseSample) { Text("Dùng dữ liệu mẫu") }
            }
        },
        confirmButton = {
            Button(
                enabled = !model.isConnecting && address.isNotBlank() &&
                    username.isNotBlank() && password.isNotEmpty(),
                onClick = { onConnect(address, username, password) }
            ) { Text(if (model.isConnecting) "Đang kết nối…" else "Đăng nhập & kết nối") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

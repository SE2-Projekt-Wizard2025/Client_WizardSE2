package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.klu.client_wizardse2.ui.presentation.MainViewModel

/**
 * The main UI screen displaying action buttons and a list of incoming messages.
 *
 * @param viewModel The [MainViewModel] containing message state and STOMP interaction logic.
 * @param modifier Modifier to apply to the outer column layout.
 */
@Composable
fun MainScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val messages = viewModel.messages

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ActionButtons(
            onTextSend = { viewModel.sendHello() },
            onJsonSend = { viewModel.sendJson() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Empfangene Nachrichten:")
        MessageList(messages = messages)
    }
}

/**
 * A row of two buttons for sending STOMP messages to the server.
 *
 * @param onTextSend Called when the "Send Text" button is clicked.
 * @param onJsonSend Called when the "Send JSON" button is clicked.
 */
@Composable
fun ActionButtons(onTextSend: () -> Unit, onJsonSend: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Button(onClick = onTextSend) {
            Text("Text senden", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
        Button(onClick = onJsonSend) {
            Text("JSON senden", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
        }
    }
}

/**
 * A scrollable list of received messages displayed as text.
 *
 * @param messages A list of message strings to display.
 */
@Composable
fun MessageList(messages: List<String>) {
    LazyColumn {
        items(messages) { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

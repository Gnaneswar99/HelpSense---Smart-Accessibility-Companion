package com.accessai.android.ui.screens.communication

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.accessai.android.service.PermissionManager
import com.accessai.feature.communication.PhraseCategory
import com.accessai.feature.communication.QuickPhrase

/**
 * Communication screen — the core accessibility communication tool.
 *
 * Layout:
 * ┌─────────────────────────────────┐
 * │  Status / Live Transcription    │  ← Shows what others say (STT)
 * ├─────────────────────────────────┤
 * │  Quick Phrases OR Conversation  │  ← Toggle between views
 * │  [Greetings] [Needs] [Emergency]│
 * │  ┌──────┐ ┌──────┐ ┌──────┐   │
 * │  │Hello │ │Help  │ │Thanks│   │
 * │  └──────┘ └──────┘ └──────┘   │
 * ├─────────────────────────────────┤
 * │  [Type message here...] [Send] │  ← Text input → TTS
 * │  [🎤 Listen]  [🔊 Speak]       │  ← Action buttons
 * └─────────────────────────────────┘
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunicationScreen(
    onBack: () -> Unit,
    viewModel: CommunicationViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasMicPermission by remember {
        mutableStateOf(PermissionManager.isPermissionGranted(context, Manifest.permission.RECORD_AUDIO))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) viewModel.startListening()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Communication") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go back")
                    }
                },
                actions = {
                    // Toggle view button
                    IconButton(
                        onClick = { viewModel.toggleView() },
                        modifier = Modifier.semantics {
                            contentDescription = if (uiState.showQuickPhrases)
                                "Switch to conversation history" else "Switch to quick phrases"
                        }
                    ) {
                        Icon(
                            if (uiState.showQuickPhrases) Icons.Default.Chat else Icons.Default.GridView,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status banner / live transcription
            StatusBanner(
                statusMessage = uiState.statusMessage,
                isListening = uiState.isListening,
                partialText = uiState.partialSpeechText,
                isSpeaking = uiState.isSpeaking
            )

            // Main content area
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.showQuickPhrases) {
                    QuickPhrasesView(
                        categories = uiState.allCategories,
                        selectedCategory = uiState.selectedCategory,
                        phrases = uiState.phrases,
                        onCategorySelected = { viewModel.selectCategory(it) },
                        onPhraseSelected = { viewModel.speakPhrase(it) }
                    )
                } else {
                    ConversationLogView(
                        messages = uiState.conversation,
                        onClear = { viewModel.clearConversation() }
                    )
                }
            }

            // Input area at bottom
            InputArea(
                typedText = uiState.typedText,
                isListening = uiState.isListening,
                isSpeaking = uiState.isSpeaking,
                onTextChanged = { viewModel.updateText(it) },
                onSend = { viewModel.speakTypedText() },
                onStartListening = {
                    if (hasMicPermission) viewModel.startListening()
                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStopListening = { viewModel.stopListening() },
                onStopSpeaking = { viewModel.stopSpeaking() }
            )
        }
    }
}

/**
 * Status banner showing current state and live transcription.
 */
@Composable
fun StatusBanner(
    statusMessage: String,
    isListening: Boolean,
    partialText: String,
    isSpeaking: Boolean
) {
    val bgColor = when {
        isListening -> Color(0xFFE3F2FD)  // Blue when listening
        isSpeaking -> Color(0xFFE8F5E9)   // Green when speaking
        else -> Color(0xFFF5F5F5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = if (isListening && partialText.isNotEmpty())
                    "They are saying: $partialText"
                else statusMessage
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isListening) {
                    Icon(
                        Icons.Default.Hearing,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                if (isSpeaking) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242)
                )
            }

            // Show live partial transcription
            if (isListening && partialText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\u201C$partialText...\u201D",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1565C0)
                )
            }
        }
    }
}

/**
 * Quick phrases grid with category tabs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickPhrasesView(
    categories: List<PhraseCategory>,
    selectedCategory: PhraseCategory,
    phrases: List<QuickPhrase>,
    onCategorySelected: (PhraseCategory) -> Unit,
    onPhraseSelected: (QuickPhrase) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Category tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            getCategoryLabel(category),
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "${getCategoryLabel(category)} phrases"
                    }
                )
            }
        }

        // Phrases grid
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(phrases) { phrase ->
                PhraseCard(
                    phrase = phrase,
                    onClick = { onPhraseSelected(phrase) }
                )
            }
        }
    }
}

/**
 * Single tappable phrase card.
 */
@Composable
fun PhraseCard(phrase: QuickPhrase, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Tap to say: ${phrase.text}"
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (phrase.emoji.isNotEmpty()) {
                Text(
                    text = phrase.emoji,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(14.dp))
            }
            Text(
                text = phrase.text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Conversation history log.
 */
@Composable
fun ConversationLogView(
    messages: List<com.accessai.feature.communication.ConversationMessage>,
    onClear: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() }
            )
            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.semantics {
                        contentDescription = "Clear conversation history"
                    }
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\uD83D\uDCAC", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No messages yet.\nTap phrases or type to start communicating.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

/**
 * Chat-style message bubble.
 */
@Composable
fun MessageBubble(message: com.accessai.feature.communication.ConversationMessage) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bgColor = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5)
    val textColor = if (isUser) Color.White else Color(0xFF212121)
    val label = if (isUser) "You said" else "They said"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bgColor),
            modifier = Modifier.semantics {
                contentDescription = "$label: ${message.text}"
            }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (message.wasSpoken) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Was spoken aloud",
                        tint = if (isUser) Color.White.copy(alpha = 0.7f) else Color(0xFF9E9E9E),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom input area with text field and action buttons.
 */
@Composable
fun InputArea(
    typedText: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    onTextChanged: (String) -> Unit,
    onSend: () -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Text input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = typedText,
                onValueChange = onTextChanged,
                placeholder = { Text("Type a message to say aloud...") },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Type a message. It will be spoken aloud when you tap send."
                    },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Speak this message aloud" },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Listen button (STT)
            FloatingActionButton(
                onClick = {
                    if (isListening) onStopListening()
                    else onStartListening()
                },
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .padding(end = 6.dp)
                    .semantics {
                        contentDescription = if (isListening)
                            "Stop listening to the other person"
                        else "Listen to the other person speak"
                    },
                containerColor = if (isListening) MaterialTheme.colorScheme.error
                else Color(0xFF1565C0),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isListening) "Stop" else "Listen",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Stop speaking button (only when TTS is active)
            AnimatedVisibility(visible = isSpeaking) {
                FloatingActionButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f)
                        .padding(start = 6.dp)
                        .semantics { contentDescription = "Stop speaking" },
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeOff, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

private fun getCategoryLabel(category: PhraseCategory): String = when (category) {
    PhraseCategory.GREETINGS -> "\uD83D\uDC4B Greetings"
    PhraseCategory.NEEDS -> "\uD83C\uDD98 Needs"
    PhraseCategory.EMERGENCY -> "\uD83D\uDEA8 Emergency"
    PhraseCategory.DIRECTIONS -> "\uD83E\uDDED Directions"
    PhraseCategory.SHOPPING -> "\uD83D\uDED2 Shopping"
    PhraseCategory.MEDICAL -> "\uD83C\uDFE5 Medical"
    PhraseCategory.CUSTOM -> "\u2B50 Custom"
}

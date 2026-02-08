package com.accessai.android.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel

/**
 * HomeViewModel - manages home screen state.
 * Will be expanded as features are added.
 */
class HomeViewModel : ViewModel()

/**
 * Home screen displaying the four accessibility modules as cards.
 * Designed with accessibility-first approach:
 * - Large touch targets (minimum 48dp)
 * - Clear content descriptions for screen readers
 * - High contrast text
 * - Semantic headings
 */
@Composable
fun HomeScreen(
    onNavigateToVision: () -> Unit,
    onNavigateToAudio: () -> Unit,
    onNavigateToNavigation: () -> Unit,
    onNavigateToCommunication: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "HelpSense",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Smart Accessibility Companion",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Vision Module Card
        item {
            ModuleCard(
                title = "Vision",
                description = "Image captioning, scene description, and text recognition powered by on-device AI",
                emoji = "\uD83D\uDC41\uFE0F",  // 👁️
                onClick = onNavigateToVision,
                contentDesc = "Vision module. Image captioning, scene description, and text recognition. Tap to open."
            )
        }

        // Audio Module Card
        item {
            ModuleCard(
                title = "Audio",
                description = "Sound recognition and smart alerts for doorbells, alarms, speech, and more",
                emoji = "\uD83D\uDD0A",  // 🔊
                onClick = onNavigateToAudio,
                contentDesc = "Audio module. Sound recognition and smart alerts. Tap to open."
            )
        }

        // Navigation Module Card
        item {
            ModuleCard(
                title = "Navigation",
                description = "Accessible routes with obstacle detection and voice-guided wayfinding",
                emoji = "\uD83E\uDDED",  // 🧭
                onClick = onNavigateToNavigation,
                contentDesc = "Navigation module. Accessible routes with obstacle detection. Tap to open."
            )
        }

        // Communication Module Card
        item {
            ModuleCard(
                title = "Communication",
                description = "Speech-to-text, sign language detection, and smart communication tools",
                emoji = "\uD83D\uDCAC",  // 💬
                onClick = onNavigateToCommunication,
                contentDesc = "Communication module. Speech to text and sign language detection. Tap to open."
            )
        }

        // Bottom spacer
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

/**
 * Reusable module card component.
 * Accessible: large touch target, descriptive semantics, high contrast.
 */
@Composable
fun ModuleCard(
    title: String,
    description: String,
    emoji: String,
    onClick: () -> Unit,
    contentDesc: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = contentDesc },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Text(
                text = emoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

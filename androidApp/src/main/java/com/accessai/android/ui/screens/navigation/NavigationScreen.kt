package com.accessai.android.ui.screens.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

/**
 * Navigation screen with three modes:
 * 1. Search mode — enter destination and plan route
 * 2. Route preview — view steps before starting
 * 3. Active navigation — step-by-step with obstacle detection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    onBack: () -> Unit,
    viewModel: NavigationViewModel = org.koin.androidx.compose.koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Permission handling for camera (obstacle detection) and location
    var hasPermissions by remember {
        mutableStateOf(
            PermissionManager.areAllGranted(context, PermissionManager.navigationPermissions)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNavigating) "Navigating" else "Navigation") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isNavigating) viewModel.stopNavigation()
                        else onBack()
                    }) {
                        Icon(
                            if (uiState.isNavigating) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (uiState.isNavigating) "Stop navigation" else "Go back"
                        )
                    }
                },
                actions = {
                    if (uiState.isNavigating) {
                        // Repeat instruction button
                        IconButton(
                            onClick = { viewModel.repeatInstruction() },
                            modifier = Modifier.semantics {
                                contentDescription = "Repeat current direction"
                            }
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null)
                        }
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
            if (uiState.isNavigating) {
                // Active navigation mode
                ActiveNavigationView(
                    uiState = uiState,
                    onNextStep = { viewModel.nextStep() },
                    onPreviousStep = { viewModel.previousStep() },
                    onRepeat = { viewModel.repeatInstruction() },
                    onToggleObstacles = { viewModel.toggleObstacleDetection() }
                )
            } else {
                // Search & route planning mode
                RoutePlanningView(
                    uiState = uiState,
                    onDestinationChanged = { viewModel.updateDestination(it) },
                    onSearch = { viewModel.planRoute() },
                    onStartNavigation = {
                        if (!hasPermissions) {
                            permissionLauncher.launch(
                                PermissionManager.navigationPermissions.toTypedArray()
                            )
                        } else {
                            viewModel.startNavigation()
                        }
                    }
                )
            }
        }
    }
}

/**
 * Route planning view with search bar and step list.
 */
@Composable
fun RoutePlanningView(
    uiState: NavigationUiState,
    onDestinationChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onStartNavigation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        OutlinedTextField(
            value = uiState.destinationQuery,
            onValueChange = onDestinationChanged,
            label = { Text("Where do you want to go?") },
            placeholder = { Text("Enter address or place name") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (uiState.isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Destination search field. Enter an address or place name."
                },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() })
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search button
        Button(
            onClick = onSearch,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = uiState.destinationQuery.isNotBlank() && !uiState.isSearching
        ) {
            Icon(Icons.Default.Navigation, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Find Accessible Route", style = MaterialTheme.typography.titleSmall)
        }

        // Error message
        if (uiState.errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Route steps list
        if (uiState.hasRoute) {
            // Route summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${uiState.steps.size} steps \u2022 ${formatDistance(uiState.totalDistanceMeters)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accessible route with sidewalks and crosswalks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Steps list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.steps) { index, step ->
                    StepCard(
                        stepNumber = index + 1,
                        instruction = step.instruction,
                        distance = step.distanceMeters,
                        landmark = step.landmark,
                        isFirst = index == 0,
                        isLast = index == uiState.steps.size - 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Start navigation button
            Button(
                onClick = onStartNavigation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics {
                        contentDescription = "Start navigation with voice guidance and obstacle detection"
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00897B)
                )
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Navigation", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else if (!uiState.isSearching) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "\uD83E\uDDED", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter a destination to get\naccessible walking directions",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Active navigation view with current step, progress, and obstacle warnings.
 */
@Composable
fun ActiveNavigationView(
    uiState: NavigationUiState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onRepeat: () -> Unit,
    onToggleObstacles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { uiState.progressPercent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .padding(vertical = 2.dp),
            color = Color(0xFF00897B),
            trackColor = Color(0xFFE0E0E0),
        )

        Text(
            text = "Step ${uiState.currentStepIndex + 1} of ${uiState.steps.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Obstacle warning banner
        AnimatedVisibility(
            visible = uiState.obstacleWarning.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        liveRegion = LiveRegionMode.Assertive
                        contentDescription = "Warning: ${uiState.obstacleWarning}"
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = uiState.obstacleWarning,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFB71C1C)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current instruction card (large, prominent)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = "Current direction: ${uiState.currentInstruction}"
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.currentInstruction,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // Landmark info
                val currentStep = uiState.steps.getOrNull(uiState.currentStepIndex)
                currentStep?.landmark?.let { landmark ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\uD83D\uDCCD $landmark",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Next instruction preview
        if (uiState.nextInstruction.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Then:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.nextInstruction,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Obstacle detection status
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (uiState.isObstacleDetectionActive)
                    "\uD83D\uDFE2 Obstacle detection active"
                else "\u26AA Obstacle detection off",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onToggleObstacles,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isObstacleDetectionActive)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    else MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    if (uiState.isObstacleDetectionActive) "Stop Detection" else "Start Detection",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        // Detected obstacles list
        if (uiState.obstacles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            uiState.obstacles.take(3).forEach { obstacle ->
                val color = when (obstacle.distanceEstimate) {
                    "near" -> Color(0xFFE53935)
                    "medium" -> Color(0xFFFFA000)
                    else -> Color(0xFF43A047)
                }
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${obstacle.label} — ${obstacle.distanceEstimate} ${obstacle.direction}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Navigation controls at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous step
            FloatingActionButton(
                onClick = onPreviousStep,
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = "Go to previous step" },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
            }

            // Repeat instruction
            FloatingActionButton(
                onClick = onRepeat,
                modifier = Modifier
                    .size(64.dp)
                    .semantics { contentDescription = "Repeat current direction" },
                containerColor = MaterialTheme.colorScheme.secondary,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Next step
            FloatingActionButton(
                onClick = onNextStep,
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = "Go to next step" },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }
    }
}

/**
 * Individual route step card.
 */
@Composable
fun StepCard(
    stepNumber: Int,
    instruction: String,
    distance: Double,
    landmark: String?,
    isFirst: Boolean,
    isLast: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isFirst -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                isLast -> Color(0xFFE8F5E9)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number circle
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isLast) Color(0xFF43A047)
                        else MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLast) {
                    Text("\u2713", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        "$stepNumber",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = instruction,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (distance > 0 || landmark != null) {
                    Row {
                        if (distance > 0) {
                            Text(
                                text = formatDistance(distance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (distance > 0 && landmark != null) {
                            Text(
                                " \u2022 ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        landmark?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDistance(meters: Double): String = when {
    meters <= 0 -> ""
    meters < 100 -> "${meters.toInt()}m"
    meters < 1000 -> "${(meters / 10).toInt() * 10}m"
    else -> "${"%.1f".format(meters / 1000)}km"
}

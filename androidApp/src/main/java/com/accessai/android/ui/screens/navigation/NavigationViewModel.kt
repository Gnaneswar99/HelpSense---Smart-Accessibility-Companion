package com.accessai.android.ui.screens.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.accessai.android.ml.TFLiteObjectDetectionEngine
import com.accessai.android.service.AndroidCameraService
import com.accessai.core.model.DetectedObject
import com.accessai.core.model.InferenceResult
import com.accessai.core.model.NavigationRepository
import com.accessai.core.model.NavigationStep
import com.accessai.core.util.HapticService
import com.accessai.core.util.TextToSpeechService
import com.accessai.feature.navigation.MonitorObstaclesUseCase
import com.accessai.feature.navigation.NavigationSession
import com.accessai.feature.navigation.Obstacle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Navigation screen.
 */
data class NavigationUiState(
    // Destination input
    val destinationQuery: String = "",
    val isSearching: Boolean = false,

    // Route state
    val hasRoute: Boolean = false,
    val steps: List<NavigationStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val totalDistanceMeters: Double = 0.0,

    // Active navigation
    val isNavigating: Boolean = false,
    val currentInstruction: String = "Enter a destination to start",
    val nextInstruction: String = "",
    val progressPercent: Float = 0f,

    // Obstacle detection
    val isObstacleDetectionActive: Boolean = false,
    val obstacles: List<Obstacle> = emptyList(),
    val obstacleWarning: String = "",

    // Mode
    val showCamera: Boolean = false,
    val errorMessage: String = ""
)

/**
 * NavigationViewModel — manages route planning, step-by-step guidance,
 * obstacle detection, and voice announcements.
 */
class NavigationViewModel(
    private val navigationRepository: NavigationRepository,
    private val cameraService: AndroidCameraService,
    private val ttsService: TextToSpeechService,
    private val hapticService: HapticService,
    private val monitorObstaclesUseCase: MonitorObstaclesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var session: NavigationSession? = null
    private var obstacleMonitorJob: Job? = null

    // Demo coordinates (can be replaced with real GPS)
    private val demoOriginLat = 35.6812
    private val demoOriginLng = 139.7671

    fun updateDestination(query: String) {
        _uiState.value = _uiState.value.copy(destinationQuery = query)
    }

    /**
     * Plan a route to the entered destination.
     * Uses demo coordinates for now — real GPS integration in production.
     */
    fun planRoute() {
        val destination = _uiState.value.destinationQuery.trim()
        if (destination.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Please enter a destination"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = "")

            // Demo destination offset (in production, geocode the address)
            val destLat = demoOriginLat + 0.005  // ~500m north
            val destLng = demoOriginLng + 0.003  // ~300m east

            val result = navigationRepository.getAccessibleRoute(
                demoOriginLat, demoOriginLng,
                destLat, destLng
            )

            when (result) {
                is InferenceResult.Success -> {
                    val steps = result.data
                    val totalDistance = steps.sumOf { it.distanceMeters }

                    session = NavigationSession(
                        originLat = demoOriginLat,
                        originLng = demoOriginLng,
                        destinationLat = destLat,
                        destinationLng = destLng,
                        destinationName = destination,
                        steps = steps,
                        isActive = false
                    )

                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        hasRoute = true,
                        steps = steps,
                        totalDistanceMeters = totalDistance,
                        currentInstruction = "Route found: ${steps.size} steps, " +
                                "${formatDistance(totalDistance)}. Tap Start to begin.",
                        nextInstruction = steps.firstOrNull()?.instruction ?: ""
                    )

                    ttsService.speak(
                        "Route found. ${steps.size} steps, ${formatDistance(totalDistance)}. " +
                                "Tap start to begin navigation."
                    )
                }
                is InferenceResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        errorMessage = result.message
                    )
                }
                is InferenceResult.Loading -> {}
            }
        }
    }

    /**
     * Start turn-by-turn navigation with voice guidance.
     */
    fun startNavigation() {
        val currentSession = session ?: return

        session = currentSession.copy(isActive = true, currentStepIndex = 0)

        _uiState.value = _uiState.value.copy(
            isNavigating = true,
            currentStepIndex = 0,
            showCamera = true,
            currentInstruction = currentSession.steps.firstOrNull()?.instruction ?: "",
            nextInstruction = currentSession.steps.getOrNull(1)?.instruction ?: "Then arrive at destination",
            progressPercent = 0f
        )

        // Announce first step
        val firstStep = currentSession.steps.firstOrNull()
        if (firstStep != null) {
            viewModelScope.launch {
                ttsService.speak(buildStepAnnouncement(firstStep, 0, currentSession.steps.size))
                hapticService.lightTap()
            }
        }

        // Start obstacle monitoring
        startObstacleDetection()
    }

    /**
     * Advance to the next navigation step.
     * In production, this would be triggered by GPS proximity.
     */
    fun nextStep() {
        val currentSession = session ?: return
        val newIndex = (currentSession.currentStepIndex + 1)

        if (newIndex >= currentSession.steps.size) {
            // Arrived
            arriveAtDestination()
            return
        }

        session = currentSession.copy(currentStepIndex = newIndex)

        val step = currentSession.steps[newIndex]
        val nextStep = currentSession.steps.getOrNull(newIndex + 1)
        val progress = (newIndex.toFloat() / currentSession.steps.size) * 100f

        _uiState.value = _uiState.value.copy(
            currentStepIndex = newIndex,
            currentInstruction = step.instruction,
            nextInstruction = nextStep?.instruction ?: "Then arrive at destination",
            progressPercent = progress
        )

        // Voice announcement
        viewModelScope.launch {
            ttsService.speak(buildStepAnnouncement(step, newIndex, currentSession.steps.size))
            hapticService.mediumImpact()
        }
    }

    /**
     * Go back to previous step.
     */
    fun previousStep() {
        val currentSession = session ?: return
        val newIndex = (currentSession.currentStepIndex - 1).coerceAtLeast(0)

        session = currentSession.copy(currentStepIndex = newIndex)

        val step = currentSession.steps[newIndex]
        val progress = (newIndex.toFloat() / currentSession.steps.size) * 100f

        _uiState.value = _uiState.value.copy(
            currentStepIndex = newIndex,
            currentInstruction = step.instruction,
            progressPercent = progress
        )

        viewModelScope.launch {
            ttsService.speak("Going back. ${step.instruction}")
        }
    }

    /**
     * Repeat current instruction via TTS.
     */
    fun repeatInstruction() {
        viewModelScope.launch {
            ttsService.speak(_uiState.value.currentInstruction)
        }
    }

    private fun arriveAtDestination() {
        _uiState.value = _uiState.value.copy(
            isNavigating = false,
            currentInstruction = "You have arrived at ${session?.destinationName ?: "your destination"}!",
            progressPercent = 100f,
            showCamera = false
        )

        stopObstacleDetection()

        viewModelScope.launch {
            hapticService.success()
            ttsService.speak("You have arrived at your destination!")
        }
    }

    /**
     * Stop navigation and return to search.
     */
    fun stopNavigation() {
        session = session?.copy(isActive = false)
        stopObstacleDetection()

        _uiState.value = _uiState.value.copy(
            isNavigating = false,
            showCamera = false,
            obstacles = emptyList(),
            obstacleWarning = ""
        )

        viewModelScope.launch {
            ttsService.speak("Navigation stopped.")
        }
    }

    /**
     * Toggle obstacle detection camera.
     */
    fun toggleObstacleDetection() {
        if (_uiState.value.isObstacleDetectionActive) {
            stopObstacleDetection()
        } else {
            startObstacleDetection()
        }
    }

    private fun startObstacleDetection() {
        obstacleMonitorJob?.cancel()

        _uiState.value = _uiState.value.copy(isObstacleDetectionActive = true)

        obstacleMonitorJob = viewModelScope.launch {
            monitorObstaclesUseCase.monitor(
                frameProvider = { cameraService.captureImage() },
                intervalMs = 800  // Check every 800ms for battery efficiency
            ).collect { obstacles ->
                val warnings = obstacles.filter { it.distanceEstimate == "near" }

                _uiState.value = _uiState.value.copy(
                    obstacles = obstacles,
                    obstacleWarning = if (warnings.isNotEmpty()) {
                        buildObstacleWarning(warnings)
                    } else ""
                )

                // Alert for nearby obstacles
                if (warnings.isNotEmpty()) {
                    val warning = buildObstacleWarning(warnings)
                    hapticService.warning()
                    ttsService.speak("Warning. $warning", queueMode = false)
                }
            }
        }
    }

    private fun stopObstacleDetection() {
        obstacleMonitorJob?.cancel()
        obstacleMonitorJob = null

        _uiState.value = _uiState.value.copy(
            isObstacleDetectionActive = false,
            obstacles = emptyList(),
            obstacleWarning = ""
        )
    }

    private fun buildObstacleWarning(obstacles: List<Obstacle>): String {
        return obstacles.joinToString(". ") { obstacle ->
            "${obstacle.label} ${obstacle.distanceEstimate} ${obstacle.direction}"
        }
    }

    private fun buildStepAnnouncement(
        step: NavigationStep, index: Int, totalSteps: Int
    ): String {
        val sb = StringBuilder()
        sb.append("Step ${index + 1} of $totalSteps. ")
        sb.append(step.instruction)

        if (step.distanceMeters > 0) {
            sb.append(" for ${formatDistance(step.distanceMeters)}")
        }

        step.landmark?.let { landmark ->
            sb.append(". Look for: $landmark")
        }

        return sb.toString()
    }

    private fun formatDistance(meters: Double): String = when {
        meters < 100 -> "${meters.toInt()} meters"
        meters < 1000 -> "${(meters / 10).toInt() * 10} meters"
        else -> "${"%.1f".format(meters / 1000)} kilometers"
    }

    override fun onCleared() {
        super.onCleared()
        stopObstacleDetection()
    }
}

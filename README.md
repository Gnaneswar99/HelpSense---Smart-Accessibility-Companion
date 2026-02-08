# <img width="70" height="71" alt="image" src="https://github.com/user-attachments/assets/9b0e690b-4a57-4d1e-be8c-30960c0bd902" /> HelpSense — Smart Accessibility Companion

<img width="500" height="500" alt="image" src="https://github.com/user-attachments/assets/e08322ef-8569-48a7-8fa8-8013df3be077" />
<img width="200" height="200" alt="image" src="https://github.com/user-attachments/assets/bc28d5fc-b02b-4b9b-aed9-4a4589cce5b7" />


An on-device AI accessibility app built with **Kotlin Multiplatform (KMM)** — helping people with visual, hearing, and mobility impairments navigate the world through real-time image captioning, sound classification, accessible navigation, and communication tools.

> **All AI runs on-device** — no internet required, no data leaves your phone.

## 📱 Features

### 👁️ Vision
- Real-time **object detection** (MobileNet SSD) with spatial descriptions
- **Image captioning** — natural language scene descriptions ("I see a park with a person on the left and a dog nearby")
- **CameraX** pipeline optimized for ML inference
- Text-to-Speech output of all captions

### 🔊 Audio Monitor
- **YAMNet** sound classification (521 classes → 15 accessibility-relevant categories)
- **Priority-based alerts**: fire alarm → heavy haptics + interrupt TTS, doorbell → medium haptics
- 16kHz audio capture pipeline optimized for real-time ML

### 🧭 Navigation
- **Accessible route planning** with step-by-step voice guidance
- **Obstacle detection** during navigation using live camera feed
- Distance estimation and directional warnings ("Car nearby to your left")
- Progress tracking with previous/next/repeat controls

### 💬 Communication
- **35+ Quick Phrases** across 6 categories (Greetings, Needs, Emergency, Directions, Shopping, Medical)
- **Text-to-Speech** — type any message and speak it aloud
- **Speech-to-Text** — capture what others say and display on screen
- **Conversation log** — two-way communication for deaf/hard-of-hearing users

## 🎯 What Makes This Special

- **Cross-platform** — Kotlin Multiplatform shared business logic for Android (iOS ready)
- **On-device ML** — TensorFlow Lite inference, zero cloud dependency
- **Accessibility-first** — TalkBack support, live regions, semantic headings, 48dp+ touch targets
- **Clean Architecture** — MVVM + Use Cases + Repository pattern + Koin DI
- **Production patterns** — Coroutines, Flow, proper lifecycle management

## 🏗️ Architecture

```
┌──────────────────────────────────────────┐
│           Presentation Layer             │
│  Jetpack Compose + Material 3            │
│  ViewModels + StateFlow                  │
├──────────────────────────────────────────┤
│            Domain Layer (KMM)            │
│  Use Cases + Repository Interfaces       │
│  Models + Business Logic                 │
├──────────────────────────────────────────┤
│              Data Layer                  │
│  TFLite ML Engines + Platform Services   │
│  Camera, Audio, TTS, Speech Recognition  │
└──────────────────────────────────────────┘
```

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9.22 (Multiplatform) |
| Android UI | Jetpack Compose + Material 3 |
| DI | Koin 3.5.3 |
| ML | TensorFlow Lite 2.14.0 |
| Camera | CameraX 1.3.1 |
| Audio | AudioRecord (16kHz PCM) |
| Speech | Android SpeechRecognizer + TTS |
| Async | Kotlin Coroutines + Flow |
| Navigation | Compose Navigation |

## 📂 Project Structure

```
HelpSense/
├── shared/                       # KMM shared module
│   └── src/
│       ├── commonMain/           # Cross-platform code
│       │   ├── core/model/       # Domain models
│       │   ├── core/ml/          # ML engine interfaces
│       │   ├── core/util/        # Service interfaces (TTS, Camera, Audio)
│       │   ├── core/di/          # Koin modules
│       │   └── feature/          # Use cases per module
│       │       ├── vision/
│       │       ├── audio/
│       │       ├── navigation/
│       │       └── communication/
│       ├── androidMain/          # Android actual implementations
│       └── commonTest/           # Unit tests
├── androidApp/                   # Android application
│   └── src/main/
│       ├── java/.../android/
│       │   ├── ml/               # TFLite engines & repositories
│       │   ├── service/          # Platform services
│       │   ├── ui/screens/       # Compose screens per module
│       │   └── di/               # Android Koin module
│       └── assets/models/        # TFLite model files
└── buildSrc/                     # Centralized dependency versions
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Setup
```bash
git clone https://github.com/YOUR_USERNAME/HelpSense.git
```
1. Open `HelpSense/` in Android Studio
2. Wait for Gradle sync to complete
3. Run on a physical device or emulator (API 26+)

### ML Models (Optional)
The app works **without model files** — ML features show informative messages while all UI, navigation, and communication features work fully.

To enable Vision and Audio ML features, download models to `androidApp/src/main/assets/models/`:

| Model | File | Size | Source |
|-------|------|------|--------|
| Object Detection | `detect.tflite` + `labelmap.txt` | ~4 MB | [TFLite Model Zoo](https://www.tensorflow.org/lite/examples/object_detection/overview) |
| Image Classification | `label.tflite` | ~3 MB | [MobileNet v2](https://storage.googleapis.com/download.tensorflow.org/models/tflite_11_05_08/mobilenet_v2_1.0_224_quant.tgz) |
| Sound Classification | `yamnet.tflite` + `yamnet_labels.txt` | ~3 MB | [YAMNet on Kaggle](https://www.kaggle.com/models/google/yamnet/tfLite/classification-tflite) |

See [`MODEL_SETUP.md`](androidApp/src/main/assets/models/MODEL_SETUP.md) for detailed download instructions.

## 🧪 Testing

```bash
# Shared unit tests
./gradlew :shared:testDebugUnitTest

# Android tests
./gradlew :androidApp:testDebugUnitTest
```

Test coverage: Domain models, Use Cases, Navigation Session, Communication Phrases, ML Engine interfaces

## 🗺️ Roadmap

- [x] KMM Project Structure & Gradle Setup
- [x] Core Accessibility Framework (TTS, Haptics, Camera, Audio)
- [x] ML Model Integration (Object Detection, Captioning, Sound Classification)
- [x] Navigation Module (Route Planning, Obstacle Detection, Voice Guidance)
- [x] Communication Module (Quick Phrases, STT, TTS, Conversation Log)
- [ ] iOS Implementation (SwiftUI screens, CoreML engines)
- [ ] Accessibility Audit (TalkBack testing, WCAG 2.1 AA compliance)

## 📄 License

MIT License — Built as a portfolio project demonstrating cross-platform mobile development with on-device AI.

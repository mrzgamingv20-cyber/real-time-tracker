# Real Time Tracker 🎥🤖

AI-powered real-time motion and object tracking application for Android. Tracks moving objects, calculates distance and speed, and analyzes lighting conditions.

## Features

- 📹 **Real-time Video Capture** — Live camera feed with preview
- 🤖 **AI Object Detection** — YOLO-based object detection and tracking
- 📊 **Motion Analytics**:
  - Distance tracking (pixel-based)
  - Speed calculation (pixels/second)
  - Acceleration measurement
  - Light source detection
- 💾 **Data Export**:
  - JSON format for tracking data
  - CSV export for analytics
  - Annotated video output
- 🎨 **Real-time Overlay** — Live metrics display on camera feed

## Tech Stack

- **Language**: Kotlin
- **Build**: Gradle
- **Camera**: AndroidX Camera API
- **ML**: TensorFlow Lite + YOLO v5
- **Storage**: Gson + SharedPreferences
- **Logging**: Timber

## Project Structure

```
.
├── src/main/
│   ├── kotlin/com/realtimetracker/app/
│   │   ├── ui/                    # UI components
│   │   ├── camera/                # Camera management
│   │   ├── tracking/              # Object tracking logic
│   │   ├── ml/                    # Machine Learning integration
│   │   └── data/                  # Data persistence
│   ├── res/                       # Resources
│   └── AndroidManifest.xml
├── build.gradle.kts               # Dependencies
└── README.md
```

## Getting Started

### Prerequisites

- Android SDK 24+
- Android Studio 2022.1+
- Kotlin 1.9+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/mrzgamingv20-cyber/real-time-tracker.git
cd real-time-tracker
```

2. Open in Android Studio

3. Build and run:
```bash
./gradlew build
./gradlew installDebug
```

## Permissions Required

- `CAMERA` — For video capture
- `RECORD_AUDIO` — For video sound
- `WRITE_EXTERNAL_STORAGE` — For saving videos and data
- `READ_EXTERNAL_STORAGE` — For accessing saved files

## Current Phase

**Phase 1** ✅ — Project Setup
- Android project structure
- Camera module integration
- Basic UI layout
- Data persistence layer

**Phase 2** 🔄 — ML Integration (In Progress)
- YOLO model integration
- Real-time object detection
- Object tracking engine
- Metrics calculation

**Phase 3** (Coming)
- Video recording module
- Export functionality
- Advanced UI with graphs
- Performance optimization

## Development Workflow

1. Create feature branches from `main`
2. Commit with descriptive messages
3. Push and create pull requests
4. Code review and merge

## Contributing

This is a learning project. Feel free to fork, experiment, and submit improvements!

## License

MIT License — See LICENSE file

## Author

**mrzgamingv20-cyber** — Learning Android Development with AI

---

**Last Updated**: 2026-06-20  
**Status**: 🚀 Active Development

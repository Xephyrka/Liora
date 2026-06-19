<p align="center">
  <img src="./assets/banner.svg" alt="Liora Banner" width="100%">
</p>

# Liora App
Liora is a task management application specifically designed for me by me  : )

## Features
- **Quick Add** - Instantly add notes or tasks via constant notification of the Liora app. You can also add a reminder time for the task by adding a semicolon with space in the end of the task title. **Example**:
  - Add a task ->
    - "Don't forget to do the dishes after eating; 8am"
    - "Read a book, damn; 8:30 pm"
    - "Take your kid home from school; 16:00"
- **Notes** - Save thoughts that aren’t tasks but shouldn’t be forgotten
- **Tasks** - Write down what needs to be done, add a reminder time. If the task is complex - break it down into subtasks
- **Repetition** - You can make tasks repeat and be reminded every n day/weeks/months
- **Focus** - Full-screen reminders and blocker mode to reduce distraction
- **Privacy First** - All data is stored locally using Room database


## Screenshots

<p align="center">
  <img src="./assets/Liora-home.png" width="30%">
  <img src="./assets/Liora-notification.png" width="30%">
  <img src="./assets/Liora-settings.png" width="30%">
</p>


## Installation

1. Download the latest APK from the [Releases page](https://github.com/Xephyrka/Liora/releases)
2. Install it on your phone
3. Enable WRITE_SECURE_SETTINGS permission by ADB to the app. THIS PERMISSION IS USED TO GRAYSCALE THE DISTRACTING APPS AND THAT ONLY
```bash
adb shell pm grant com.xephyrka.liora android.permission.WRITE_SECURE_SETTINGS
```

### Prerequisites
- Android SDK 29+


## Built With
- **Kotlin** - 100%
- **Jetpack Compose** - Modern declarative UI with animations
- **Room Database** - Persistent local storage with automated migrations
- **DataStore** - User preferences and theme settings
- **Navigation Compose** - Type-safe routing between screens


## Feedback

If you have any feedback, please reach out to me [here](https://github.com/Xephyrka/Liora/issues)


## License

MIT - see the [LICENSE](LICENSE) file for details

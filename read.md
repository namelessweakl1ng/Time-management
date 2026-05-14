# Time Management

An Android-based Time Management application designed to help users manage tasks, schedules, reminders, and daily productivity efficiently.

---

## Features

- Add and manage tasks
- Schedule daily activities
- Set reminders and deadlines
- Simple and responsive UI
- Productivity tracking
- Easy navigation

---

## Tech Stack

- Android Studio
- Java / Kotlin
- Gradle
- XML
- Android SDK

---

## Prerequisites

Make sure the following are installed before running the project:

- Android Studio
- JDK 17 or above
- Android SDK
- Git

---

## Clone the Repository

```bash
git clone https://github.com/namelessweakl1ng/Time-management.git
cd Time-management
```

---

## Open Project in Android Studio

1. Open Android Studio
2. Click **Open**
3. Select the `Time-management` folder
4. Wait for Gradle Sync to finish

---

## Run the Application

### Using Android Studio

1. Start an Android Emulator or connect a physical Android device
2. Click the **Run ▶** button

---

### Using Command Line

For Linux/macOS:

```bash
./gradlew build
```

For Windows:

```bash
gradlew.bat build
```

Install debug APK:

```bash
./gradlew installDebug
```

---

## Build APK

Generate APK using:

```bash
./gradlew assembleDebug
```

Generated APK location:

```text
app/build/outputs/apk/debug/
```

---

## Project Structure

```text
Time-management/
│
├── app/
│   ├── src/
│   ├── build.gradle
│
├── gradle/
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```

---

## Common Issues

### Gradle Sync Failed

Run:

```bash
./gradlew clean
./gradlew build
```

---

### SDK Location Not Found

Configure SDK path in Android Studio:

```text
File → Settings → Android SDK
```

---

### Emulator Not Starting

- Enable virtualization in BIOS
- Install Android Emulator Hypervisor

---

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a new branch
3. Commit your changes
4. Push the branch
5. Open a Pull Request

---

## License

This project is licensed under the MIT License.

---

## Repository Link

https://github.com/namelessweakl1ng/Time-management
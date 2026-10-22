Eisenhower Widget (Android + Desktop)
A full-stack productivity application that combines the Eisenhower Matrix with a dynamic monthly calendar. Built with Kotlin, Jetpack Compose (for widgets), and Firebase, with a cross-platform desktop companion built in Tauri.

Features
Interactive Matrix Widget: A 2x2 grid widget that allows you to check off tasks directly from the home screen.

Dynamic Calendar Widget: A full monthly calendar view that automatically adapts to the widget's size and allows scrolling.

Advanced Event Logic: Tasks can be configured as "Events" that dynamically change quadrants as the due date approaches (Phase 1, Phase 2, Phase 3).

Recurring Tasks: Support for daily, weekly, and monthly recurrence.

Rollover System: Incomplete tasks automatically move to the next day, ensuring nothing is forgotten.

Productivity Tracking: If a task is completed late, the calendar highlights the original due date in gray and shows the actual completion date with a "COMPLETED X DAYS LATER" prefix.

Cloud Sync & Calendar Integration: Real-time synchronization via Firebase Firestore and automatic insertion into Google Calendar.

Tech Stack
Mobile: Kotlin, Android SDK (API 26+), RemoteViews, SharedPreferences, WorkManager.

Desktop: Tauri (Rust + TypeScript).

Backend: Firebase Firestore (NoSQL), Firebase Auth (Google Sign-In).

CI/CD: GitHub Actions (automated APK and Windows installer builds).

Architecture
The app follows a modular architecture where the UI interacts with a central TaskRepository, which handles local storage and synchronizes with Firebase.

text
[ Android App ] <---> [ TaskRepository ] <---> [ Firebase Firestore ] <---> [ Desktop App ]
                             |                          |
                    [ Local JSON Storage ]      [ Auth (Google) ]
                             |
                    [ Widgets (RemoteViews) ]
How to Build
Clone the repository.

Ensure you have Android Studio (Hedgehog or newer) installed.

Set up a Firebase project and add your google-services.json to the app/ folder.

Build the APK:

bash
./gradlew assembleDebug
(Note: The GitHub Actions workflow automatically builds and releases the APK on every push to the main branch).

License
MIT License

Copyright (c) 2026 [Tu Nombre Aquí]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


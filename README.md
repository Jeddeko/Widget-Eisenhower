# Eisenhower Widget (Android + Desktop)

A full-stack productivity application that combines the Eisenhower Matrix with a dynamic monthly calendar. Built with Kotlin, Jetpack Compose (for widgets), and Firebase, with a cross-platform desktop companion built in Tauri.

## ✨ Features

- **Interactive Matrix Widget:** A 2x2 grid widget that allows you to check off tasks directly from the home screen.
- **Dynamic Calendar Widget:** A full monthly calendar view that automatically adapts to the widget's size and allows scrolling.
- **Advanced Event Logic:** Tasks can be configured as "Events" that dynamically change quadrants as the due date approaches (Phase 1, Phase 2, Phase 3).
- **Recurring Tasks:** Support for daily, weekly, and monthly recurrence.
- **"Rollover" System:** Incomplete tasks automatically move to the next day, ensuring nothing is forgotten.
- **Productivity Tracking:** If a task is completed late, the calendar highlights the original due date in gray and shows the actual completion date with a "COMPLETED X DAYS LATER" prefix.
- **Cloud Sync & Calendar Integration:** Real-time synchronization via Firebase Firestore and automatic insertion into Google Calendar.

## 🛠 Tech Stack

- **Mobile:** Kotlin, Android SDK (API 26+), RemoteViews, SharedPreferences, WorkManager.
- **Desktop:** Tauri (Rust + TypeScript).
- **Backend:** Firebase Firestore (NoSQL), Firebase Auth (Google Sign-In).
- **CI/CD:** GitHub Actions (automated APK and Windows installer builds).

## 🏗 Architecture

The app follows a modular architecture where the UI interacts with a central `TaskRepository`, which handles local storage and synchronizes with Firebase.

```text
[ Android App ] <---> [ TaskRepository ] <---> [ Firebase Firestore ] <---> [ Desktop App ]
                             |                          |
                    [ Local JSON Storage ]      [ Auth (Google) ]
                             |
                    [ Widgets (RemoteViews) ]

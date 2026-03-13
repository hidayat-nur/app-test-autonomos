# App Automation - Project Configuration

## Project Structure
- `app/`: Native Android application (Kotlin + Jetpack Compose)
- `admin-web/`: Web-based admin dashboard (Next.js + TypeScript)

---

## 📱 Android App (`app/`)
### Tech Stack
- **Language**: Kotlin 1.9.20
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt 2.48
- **Local Database**: Room 2.6.1
- **Background Tasks**: WorkManager & Foreground Services
- **Automation**: AccessibilityService (Gestures & Interaction)
- **Backend/Analytics**: Firebase (Firestore, Crashlytics, Analytics)

### Important Commands
- `./gradlew assembleDebug`: Build debug APK
- `./gradlew bundleRelease`: Build release AAB
- `./gradlew test`: Run unit tests
- `./gradlew connectedAndroidTest`: Run instrumentation tests
- `./gradlew lint`: Run linting

### Coding Standards
- **Naming**: `PascalCase` for Widgets/Classes, `camelCase` for variables/functions.
- **State**: Use `StateFlow` and `collectAsStateWithLifecycle()` in Compose.
- **Dependency**: Always inject via Hilt; avoid manual instantiation of Managers/Repositories.
- **Resources**: Use `stringResource()` for all text (i18n ready).

---

## 🌐 Admin Web (`admin-web/`)
### Tech Stack
- **Framework**: Next.js 15+ (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS 4
- **State Management**: React Hooks + Firebase SDK
- **Backend Integrations**: Firebase Firestore & Auth

### Important Commands
- `npm run dev`: Start development server
- `npm run build`: Build for production
- `npm run lint`: Run ESLint
- `npm start`: Start production server

### Coding Standards
- **Components**: Functional components only. Use `App Router` (`app/` directory).
- **Types**: Always define interfaces/types for props and data models.
- **Firebase**: Use the Modular SDK (`firebase/firestore`, etc.).

---

## 🛠️ General Guidelines
- **Project Goal**: Automate app launching and interactions (tap/scroll) using Accessibility Services.
- **Permissions**: Core app depends on `Accessibility Service` and `Usage Stats`.
- **Firebase**: Shared database (Firestore) between Android app and Admin Web.
- **Design**: Maintain a premium, modern aesthetic with dark mode support.
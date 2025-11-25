# AsanaAndroidApp

Android front-end client for the Asana-style task management backend  
([AsanaBackend](https://github.com/denys-kuzhyk/AsanaBackend)).

The app is built with **Kotlin** and **Jetpack Compose** and communicates with the Flask backend via **Retrofit**.  
It allows users to sign up, log in, view tasks, create/edit/delete tasks, and manage their account.

---

## Project Structure

The main application code lives in:

`app/src/main/java/com/example/asanaapp`

Key packages / files:

- `data/`
  - `model/`
    - `ApiModels` – request/response DTOs
  - `network/`
    - `ApiService` – Retrofit interface for all API endpoints.
    - `AuthRepository` – networking + token handling (login, sign up, refresh, tasks, etc.).
    - `RetrofitInstance` – Retrofit client configured with `BASE_URL`.
  - `storage/`
    - `TokenManager` – stores access/refresh tokens using `EncryptedSharedPreferences`.
    - `AuthManager` – exposes login state (`StateFlow<Boolean>`).
    - `UserManager` – stores user profile, current project, statuses and projects map in DataStore.
- `navigation/`
  - `AppNavigation` – single NavHost, handles navigation between login, sign up and app screens.
- `ui/`
  - `app/`
    - `HomeViewModel` – main app ViewModel: tasks, create/edit/delete, refresh, change password, logout.
    - `Home` – home screen (project selection, stats, navigation buttons).
    - `TasksScreen` – list of tasks with edit/delete actions.
    - `NewTaskScreen` – create task screen.
    - `EditTaskScreen` – edit existing task.
    - `Account` – account details (id, name, email, role) + change password button.
    - `ChangePassword` – change password flow.
  - `auth/login/`
    - `LoginScreen` – login form.
    - `LoginViewModel` – login logic and state.
  - `auth/signup/`
    - `EmailScreen` – email step for sign up.
    - `PasswordScreen` – password step for sign up.
    - `SignUpViewModel` – sign up logic and state.
  - `components/`
    - Shared UI components: `AsanaButton`, `AsanaTextField`, `MessageDialog`,
      `TaskItem`, `ItemDropdown`, helpers like `isEmailValid` and `isDateValid`.
- `MainActivity.kt`
  - Creates `TokenManager`, `AuthManager`, `UserManager`, `AuthRepository`.
  - Instantiates `LoginViewModel`, `SignUpViewModel`, `HomeViewModel`.
  - Calls `AppNavigation` as the root composable.

---

## Features

- **Authentication**
  - Email/password sign up (two-step: email → password).
  - Login with JWT-based authentication.
  - Persistence of access/refresh tokens.
  - Auto-redirect to Home when already logged in.

- **Projects & Roles**
  - User’s role and projects are loaded from the backend.
  - Managers can select a project (via dropdown) and see its tasks.
  - Selected project is stored in DataStore and reused across sessions.

- **Tasks**
  - Fetch tasks for the current project.
  - Display tasks with expandable details:
    - Description
    - Status
    - Due date
    - Assignee (visible to Managers)
  - Show statistics (Open / Completed / Total).
  - Create a new task (name, description, due date, assignee, status).
  - Edit existing tasks (due date, status, assignee for Managers).
  - Delete tasks (Managers only).

- **Account Management**
  - View user ID, name, email, and role.
  - Change password (with validation & loading states).
  - Logout clears tokens and user data and returns to login.

- **Validation & UX**
  - Email format validation.
  - Strict due date validation (`YYYY-MM-DD`) using `java.time`.
  - Loading indicators per operation (login, sign up, tasks, change password, etc.).
  - Auto-hiding message dialogs after a timeout or on user dismiss.
  - Token refresh:
    - If the backend returns an auth error (expired/invalid token),
      the app calls `/refresh`, retries the request, or logs the user out if refresh fails.

---

## Requirements

- **Android Studio** Iguana (or newer)
- **JDK** 17
- **Android SDK**
  - `minSdk`: 26  
  - `compileSdk`: 35  
  - `targetSdk`: 35

Main libraries (managed via Gradle):

- Kotlin
- Jetpack Compose (Material 3)
- AndroidX Navigation Compose
- AndroidX Lifecycle (ViewModel, Runtime)
- AndroidX DataStore (Preferences)
- AndroidX Security Crypto (`EncryptedSharedPreferences`, `MasterKey`)
- Retrofit2 + Gson Converter
- Gson

---

## Backend Dependency

This Android app is designed to work with the Flask backend:

- **Backend repo:** `https://github.com/denys-kuzhyk/AsanaBackend`

The API endpoints used by the app include:

- `POST /login`
- `POST /signup`
- `POST /refresh`
- `GET /get-tasks`
- `PUT /edit-task`
- `POST /create-task`
- `DELETE /delete-task`
- `PUT /change-password`

Make sure the backend is running and reachable from the device/emulator.

---

## Configuration

### 1. API Base URL

The base URL is defined in:

`app/src/main/java/com/example/asanaapp/data/network/RetrofitInstance.kt`

```kotlin
private const val BASE_URL = "http://10.0.2.2:5000/"
```

- For **Android Emulator + backend running on your local machine**, keep:
  - `http://10.0.2.2:5000/`
- For a **real device** or a deployed backend, change `BASE_URL` to the actual server address, e.g.:
  - `https://your-domain.com/`
  - or `http://192.168.0.10:5000/`

Rebuild the app after changing the URL.

---

## Running the App

1. **Clone the repository**

```bash
git clone https://github.com/denys-kuzhyk/AsanaAndroidApp.git
cd AsanaAndroidApp
```

2. **Open in Android Studio**

- Open Android Studio → *Open* → select the `AsanaAndroidApp` folder.
- Let Gradle sync and download all dependencies.

3. **Configure the backend URL**

- Edit `RetrofitInstance.kt` if needed and set `BASE_URL` to your backend address.

4. **Run**

- Select a device or emulator.
- Click **Run** in Android Studio.

You should see:

- Login screen → option to sign up.
- After login, the Home screen with your tasks and navigation to Account / Tasks / New Task.

---

## Notes

- All auth and user-related state is persisted via `TokenManager` and `UserManager`.
- If a token expires, the app automatically tries to refresh it before logging the user out.
- No secrets (tokens, keys) are stored in the repo; they are obtained at runtime from the backend.

# Revolt for Android

## Description

This is the official Android app for the [Revolt](https://revolt.chat) chat platform.  
The codebase includes the app itself, as well as an internal library for interacting with the Revolt
API.

| Module         | Description                             |
|----------------|-----------------------------------------|
| `:app`         | The main app module.                    |
| `:lettertrees` | A library for parsing text into an AST. |

The API library is part of the `app` module, and is not intended to be used as a standalone library,
as it makes liberal use of Android-specific APIs for reactivity.

The app is currently in alpha, and is not yet ready for production use.

The app is written in Kotlin, and uses
the [Jetpack Compose](https://developer.android.com/jetpack/compose) UI toolkit, the current state
of the art for Android UI development.

## Stack

- [Kotlin](https://kotlinlang.org/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Ktor](https://ktor.io/)
- [Dagger](https://dagger.dev/) with [Hilt](https://dagger.dev/hilt/)

## Resources

### Revolt

- [Revolt Project Board](https://github.com/revoltchat/revolt/discussions) (Submit feature requests
  here)
- [Revolt Testers Server](https://app.revolt.chat/invite/Testers)
- [Contribution Guide](https://developers.revolt.chat/contributing)

## Quick Start

Open the project in Android Studio. You can then run the app on an emulator or a physical device by
running the `app` module.

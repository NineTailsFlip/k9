# K9 — Keyboard on 9 keys

A T9 keyboard for Android phones with a keypad, with its settings in the Nine look.

[![Build](https://github.com/NineTailsFlip/k9/actions/workflows/build.yml/badge.svg)](https://github.com/NineTailsFlip/k9/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/NineTailsFlip/k9)](https://github.com/NineTailsFlip/k9/releases/latest)
[![License](https://img.shields.io/badge/license-Apache--2.0%20%2B%20GPL--3.0-blue)](#license)

K9 is [NineTailsFlip](https://github.com/NineTailsFlip)'s fork of
[Traditional T9](https://github.com/sspanak/tt9) (TT9) by [sspanak](https://github.com/sspanak) and contributors.
Typing works as in TT9; every settings screen is rebuilt on
[nine-core](https://github.com/NineTailsFlip/nine-core), so K9 looks and handles like the other
Nine apps: one column of text on black, the D-pad, a numbered `*` menu, a hint bar.

## Features

From TT9:
- Predictive (T9), ABC and 123 input in 50+ languages, with dictionaries and words you add.
- Configurable hotkeys, clipboard, undo/redo, an optional on-screen keypad.
- It [doesn't spy](docs/privacy.md) on you.

Ours:
- **Settings in the Nine look:** keyboard, keys, predictive and ABC modes, typing, mind reading,
  compatibility, the on-screen keypad, diagnostics, help.
- **Nine screens** for languages and their dictionaries, hotkeys, the characters on each key,
  added words, the manual and privacy policy, logs, and a "try typing" page.
- **Layouts for the system:** `LanguagesProvider` lets the phone's own Setup and Settings list
  K9's layouts and choose which are on — any number of them.
- **Updates from GitHub Releases**, and the look chosen in the phone's first-time setup.

## Installation

Download `k9-<version>.apk` from [Releases](https://github.com/NineTailsFlip/k9/releases/latest)
and install it, then turn it on: Settings → System → Keyboard. On the
[thor2 image](https://github.com/kloploid/android_vendor_thor2) it comes preinstalled and is the
keyboard in use from the first screen.

Releases are TT9's `lite` flavor: dictionaries are downloaded the first time a language is used.
The package is `io.github.ninetailsflip.k9`, so K9 installs next to TT9.

## Usage

Type with the number keys; the hotkeys (mode, language, backspace, and more) are set in
Settings → Keys → Hotkeys. The user manual is in the app (Settings → Help) and in
[docs/help](docs/help) in several languages.

## Building

Requirements: JDK 17+, Android SDK (platform 34).

```bash
git clone --recursive https://github.com/NineTailsFlip/k9.git
cd k9
./gradlew :app:assembleLiteDebug
```

Tests: `./gradlew :app:testLiteDebugUnitTest`.

The version comes from git: `versionCode` is the number of commits, `versionName` is
`<number of v-tags>.<commits since the last tag>`. A release is a tag `v<next>.0` (one more than
the current tags), then — because workflows don't run on their own in a fork —
`gh workflow run Build --ref v<next>.0`. Signing needs `app/keystore.properties` (git-ignored),
which CI writes from secrets.

## Project structure

| Path | What's there |
|---|---|
| `app/src/main/java/io/github/sspanak/tt9/` | TT9: the input method, languages, dictionaries |
| `app/src/main/java/io/github/ninetailsflip/k9/` | Ours: the Nine settings screens, `K9Screens`, `LanguagesProvider`, `K9Application` |
| `app/languages/` | Language definitions and dictionaries (TT9) |
| `nine-core/` | Git submodule: [nine-core](https://github.com/NineTailsFlip/nine-core) |
| `docs/` | TT9's manual, privacy policy and installation guide |

## Staying in sync with upstream

K9 holds TT9's full history with our changes on top. Most of ours live in
`io.github.ninetailsflip.k9`; a few touch TT9's own code (opening the settings screens). Merge
upstream releases with `git pull https://github.com/sspanak/tt9 master` and resolve those spots.

## Contributing

Issues and pull requests are welcome; see
[CONTRIBUTING](https://github.com/NineTailsFlip/.github/blob/main/CONTRIBUTING.md). Typing and
dictionary problems that also happen in TT9 belong upstream:
<https://github.com/sspanak/tt9/issues>.

## License

TT9's code is under the Apache License 2.0 — see [LICENSE.txt](LICENSE.txt). K9 builds include
[nine-core](https://github.com/NineTailsFlip/nine-core), which is GPL-3.0, so the K9 app as a
whole is distributed under GPL-3.0-or-later.

# Obsidian Android Notes Widget

> **⚠️ AI-GENERATED CODE — All code in this repository has been entirely written by AI (GitHub Copilot / Claude). It was built through iterative prompting with no manual code authoring. Use at your own discretion.**

---

An Android home screen widget that displays a filtered list of your [Obsidian](https://obsidian.md) notes by tag, with one-tap to open directly in Obsidian.

Inspired by the UpNote widget.

## Screenshots

![Widget home screen](screenshots/widget_home.png)
![Widget search](screenshots/widget_search.png)

## Features

- 📋 Displays notes filtered by a specific tag
- 🔍 Search across all your notes from the widget header
- 📂 Tap any note title to open it directly in Obsidian
- �� Refresh button to reload notes on demand
- 🌙 Dark theme matching Obsidian's visual style

## Requirements

- Android 8.0 (API 26) or higher
- [Obsidian](https://obsidian.md) installed on your device
- Your Obsidian vault stored in a location accessible via Android's Storage Access Framework (local storage, not a virtual cloud sync folder)

## Installation

1. Go to the [Releases page](https://github.com/ArthurGoupil/obsidian-android-notes-widget/releases/latest) and download `app-debug.apk`
2. On your Android device, open the downloaded file — you'll be prompted to allow installation from unknown sources, which is normal for apps not distributed via the Play Store
3. Follow the on-screen instructions to install

<details>
<summary>Build from source</summary>

```bash
git clone https://github.com/ArthurGoupil/obsidian-android-notes-widget.git
cd obsidian-android-notes-widget
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires Android Studio or the Android SDK with Gradle.
</details>

## Setup

1. **Open the app** — tap the "Obsidian Notes Widget" launcher icon
2. **Select your vault folder** — tap "Select Folder" and pick the parent folder that contains your vault (i.e. the folder *containing* your vault folder, not the vault itself)
3. **Enter your vault name** — type the exact name of your vault folder (must match the folder name on disk precisely)
4. Tap **Save**
5. **Add the widget** to your home screen via your launcher's widget picker
6. **Configure the widget** — choose the tag you want to filter by

## How It Works

The app uses Android's [Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) to read `.md` files from your vault directory without requiring broad storage permissions. It scans for YAML frontmatter `tags:` fields and filters notes accordingly.

Notes are opened via the `obsidian://open` URI scheme:
```
obsidian://open?vault=YourVaultName&file=path/to/note.md
```

## Known Limitations

- **Local vaults only** — the vault must be accessible via SAF (local storage). Cloud-synced vaults may not work if files are virtual
- **Tag format** — only YAML frontmatter tags are supported (e.g. `tags: [widget]` or `tags:\n  - widget`). Inline `#tags` in the note body are not scanned
- **No auto-refresh** — the widget does not auto-refresh in the background; tap the refresh button to reload
- **Vault name must match exactly** — the vault name entered in settings must match the folder name on disk character-for-character

## License

MIT — see [LICENSE](LICENSE)

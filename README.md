# SiteBlock for Android

A private, single-user Android app that blocks distracting apps and websites with one rule:
**easy to add blocks, slow to remove them.**

- **Strengthen instantly:** turning blocking on, adding an app or site, and resuming are one tap.
- **Weaken slowly:** turning blocking off, removing an item, or pausing the web filter needs a
  **5-minute wait → a 2-minute confirm window → your phone PIN** (no fingerprint). Miss the window
  and you start over. A reboot cancels a pending unlock. Blocking stays on.
- **Apps are blocked by ID,** so an app you block stays blocked if you uninstall and reinstall it,
  and you can block apps you don't have installed yet.
- **Websites are blocked in every app,** including links opened from Telegram, Chrome Custom
  Tabs and in-app browsers, by an on-phone DNS filter.

## How it works

| Piece | What it does |
| --- | --- |
| `:core` | Pure Kotlin, fully unit-tested: friction gate, block list, enforcement policy, settings-guard rules, DNS packet parsing and filtering. |
| Web filter (`FilterVpnService`) | A local VPN that only carries DNS lookups. Blocked names get "not found". Other traffic never enters it, so there's no speed cost. Also blocks known encrypted-DNS services so browsers can't skip it. |
| App blocker (`BlockerAccessibilityService`) | Sees which app comes to the front. If its ID is blocked, shows a full-screen block page with today's attempt count. While blocking is on, it also bounces you out of the settings screens that could switch SiteBlock off. |
| Device admin | Stops SiteBlock from being uninstalled while it's active. |

## Build and install

Requires Android Studio (or JDK 17+ and the Android SDK) and a phone on Android 11+.

```sh
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :core:test          # unit tests
./gradlew :app:assembleDebug  # builds app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then open SiteBlock and work through **Setup**. See [`docs/MANUAL_VERIFICATION.md`](docs/MANUAL_VERIFICATION.md).

## Known limits

- **Safe Mode** turns off all third-party apps, SiteBlock included. A future "Device Owner" mode would close this.
- Telegram **link previews** (text shown inside the chat) come from Telegram's servers, so the
  filter can't stop them. Blocking `twimg.com` stops X images and video in them.
- The settings lock reads on-screen text in English. With the phone set to another language it
  may not recognise SiteBlock's App info page.
- The web filter covers DNS lookups that use the phone's normal DNS (every app that doesn't
  hard-code its own DNS server). An app that sends DNS straight to a fixed server it chose itself
  can skip it.
- Only one VPN can run at a time. Pause the web filter (behind the gate) to use another VPN.

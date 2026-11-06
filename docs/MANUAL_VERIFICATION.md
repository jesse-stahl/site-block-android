# Manual verification (on the phone)

These need a real phone. Run them on the Samsung Galaxy after every significant change.

## 0. Install

1. Enable Developer options and USB debugging on the phone and plug it in.
2. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. If Android shows "Restricted setting" when you turn on Accessibility, open
   Settings → Apps → SiteBlock → ⋮ → **Allow restricted settings**, then try again.

## 1. Setup

Open SiteBlock → Setup. Every row should reach ✓ (step 8 has no check):
screen lock, Accessibility, VPN permission, Device admin, battery, notifications, Private DNS.
For Samsung, also add SiteBlock to Settings → Battery → Background usage limits → **Never auto sleeping apps**.
Turn on **Always-on VPN** for SiteBlock and leave "Block connections without VPN" **off**.

## 2. Core checks

| # | Do this | Expect |
| --- | --- | --- |
| 2.1 | Add **X** from Common, turn blocking ON | Status says "Blocking is ON" with no warnings |
| 2.2 | Open the X app | Block page: "X (Twitter) is blocked · You tried to open it once today" |
| 2.3 | In Telegram, tap an x.com link | Page fails to load ("site can't be reached") |
| 2.4 | Open x.com and twitter.com in Chrome | Both fail to load |
| 2.5 | Uninstall X, reinstall it from Play, open it | Block page appears on first launch |
| 2.6 | Add an app you don't have by ID (e.g. `com.reddit.frontpage`), install it, open it | Block page |
| 2.7 | Tap "Turn blocking OFF…" | 5:00 countdown; Confirm is disabled |
| 2.8 | Wait 5 min, tap Confirm with PIN | PIN prompt only, no fingerprint option. Correct PIN turns blocking off |
| 2.9 | Start a turn-off, wait past the 2-min window | Pending card disappears; you must start over |
| 2.10 | Start a turn-off, reboot | After reboot: still blocking, nothing pending |
| 2.11 | Remove an item | Same wait → confirm → PIN gate, even while blocking is off |

## 3. Settings guard (blocking ON)

Each should send you to the home screen with a "SiteBlock locks this screen" toast:
- Settings → Apps → SiteBlock (App info: Force stop / Uninstall)
- Long-press SiteBlock's icon → Uninstall
- Settings → Accessibility → Installed apps
- Settings → Security → Device admin apps
- Settings → Connections → More connection settings (shows VPN and Private DNS)

Should NOT bounce: Settings → Apps (the full list), Display, Sound, Wi-Fi.

## 4. Web filter pause

| # | Do this | Expect |
| --- | --- | --- |
| 4.1 | Pause web filter → 15 minutes → gate | Sites load; apps stay blocked; the VPN key icon goes away |
| 4.2 | Wait 15 min (or tap Resume now) | Sites blocked again |
| 4.3 | Pause 1 day, then reboot | After reboot the filter is back on (a reboot ends a timed pause) |
| 4.4 | Pause "until I turn it back on", reboot | Still paused after reboot |
| 4.5 | During a pause, open VPN settings | Not bounced (so you can use another VPN) |

## 5. Failure states

| # | Do this | Expect |
| --- | --- | --- |
| 5.1 | Set Private DNS to a provider hostname (blocking off), turn blocking on | Warning: "Private DNS is set to a specific provider…" |
| 5.2 | Turn on another VPN app while the filter runs (blocking off first, then on) | "Web filter stopped" notification; status shows the gap |
| 5.3 | Turn Accessibility off (blocking off) then turn blocking on | Status warns that app blocking is off |

## 6. Speed

Run fast.com with blocking off, then on. Download speed should be the same. Browsing should feel the same.

## Known gap

Safe Mode disables SiteBlock. This is accepted for v1.

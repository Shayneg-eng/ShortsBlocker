# Shorts Blocker (Android)

Blocks Instagram Reels and YouTube Shorts. When either opens, the app taps the
Home tab for you (or presses Back if it can't find it).

## Build and install

1. Install [Android Studio](https://developer.android.com/studio).
2. `File > Open` and choose this `ShortsBlocker` folder. Let Gradle sync.
3. On your phone: enable Developer options and USB debugging, plug it in.
4. Press Run. The app installs.
5. Open **Shorts Blocker**, tap **Open Accessibility settings**, pick
   **Shorts Blocker** and turn it on.

If Android says "Restricted setting" when you try to enable it: open
Settings > Apps > Shorts Blocker > three-dot menu > **Allow restricted settings**,
then try again. (Android does this for sideloaded accessibility apps.)

## How it works

An Android `AccessibilityService` listens only to `com.instagram.android` and
`com.google.android.youtube` (see `accessibility_config.xml`). On each screen
change it checks the on-screen view tree for the Reels/Shorts player or the
selected Reels/Shorts tab, then triggers the exit. Nothing is stored or sent.

## When it stops working

Instagram and YouTube rename internal view IDs in updates. To find the current
ones with the screen showing a Reel / Short:

    adb shell uiautomator dump /sdcard/ui.xml && adb pull /sdcard/ui.xml

Search `ui.xml` for `resource-id` values near the video pager (`clips_viewer...`
for Instagram, `reel_...` for YouTube), then update `isInstagramReels` /
`isYouTubeShorts` in `ShortsBlockerService.kt`.

## Limits

- Only works while the service is enabled; you can turn it off in Settings, so
  this is a speed bump, not a lock.
- Instagram Reels opened from a DM or shared link are also exited.
- The IDs in the code are best guesses and have not been tested on a device.

# LGTweaks
A collection of Xposed tweaks for my LG V60.

You should probably not use this directly but you can cherry-pick the tweaks you want into your own Xposed module.

# Tweak list
- [Double haptics](https://www.reddit.com/r/LGV60/comments/lw07g2/double_haptics_with_nav_buttons_on_android_11/) is disabled
- Prevent the screen from waking when power is connected/disconnected
- Long press volume keys to skip tracks
- Lockscreen album art has been disabled
- Quick settings tiles now vibrate when you tap them
- Peek has been disabled for easier knock-off
- Allow [KDE Connect](https://kdeconnect.kde.org/) to access the clipboard in the background
- Enable rotating the phone upside down
- Long-press the Google Assistant key to toggle the flashlight
- Single-click the Google Assistant key to perform any action you want (controllable via Tasker, I have mine set on play-pause)
- Double-click the Google Assistant key to perform any action you want (controllable via Tasker)
- Triple-click the Google Assistant key to perform any action you want (controllable via Tasker)
- `FLAG_SECURE` is disabled globally. Screenshot any app you want!
- The contents of specific apps have been hidden from recents. This does not mean they have `FLAG_SECURE` though, this only affects recents. You can still screenshot them.
- Fix lag when opening recents sometimes
- Disables system updates
- Allow installing any app in Dual App
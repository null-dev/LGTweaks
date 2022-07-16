package ax.nd.lgtweaks

import ax.nd.lgtweaks.systemui.behavior.*
import ax.nd.lgtweaks.systemui.lockscreen.DisableLockscreenAlbumArtHook
import ax.nd.lgtweaks.systemui.notifications.DisablePeekHook
import ax.nd.lgtweaks.systemui.notifications.quicktiles.HapticQuickTilesHook
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Module : IXposedHookLoadPackage {
    /**
     * This method is called when an app is loaded. It's called very early, even before
     * [Application.onCreate] is called.
     * Modules can set up their app-specific hooks here.
     *
     * @param lpparam Information about the app.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // Hook SystemUi
        if (lpparam.packageName == "com.android.systemui") {
            DisableLockscreenAlbumArtHook.setup(lpparam)
            HapticQuickTilesHook.setup(lpparam)
            DisablePeekHook.setup(lpparam)
            LongPressVolumeKeyHook.setupSysUi(lpparam)
        }
        if (lpparam.packageName == "android") {
            DisableWakeScreenOnPowerHook.setup(lpparam)
            LongPressVolumeKeyHook.setup(lpparam)
            DisableDoubleHapticsHook.setup(lpparam)
            ClipboardWhitelistHook.setup(lpparam)
            EnableAllRotationsHook.setup(lpparam)
            AIKeyHandlerHook.setup(lpparam)
            DisableFlagSecureHook.setup(lpparam)
            HideRecentsHook.setup(lpparam)
        }
        if(lpparam.packageName == "com.android.settings") {
            EnableAllDualAppsHook.setup(lpparam)
        }
        if (lpparam.packageName == "com.lge.launcher3") {
            RecentsLagFixHook.setup(lpparam)
        }
        // Hook all apps with the broken updater
        // Currently only known user is: com.google.android.gms
        DisableSystemUpdatesHook.setup(lpparam)
    }
}
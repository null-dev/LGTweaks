package ax.nd.lgtweaks.systemui.behavior

import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Disable FLAG_SECURE for all apps.
 */
object DisableFlagSecureHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.android.server.wm.WindowState",
            lpparam.classLoader,
            "isSecureLocked",
            XC_MethodReplacement.returnConstant(false)
        )
    }
}
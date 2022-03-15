package ax.nd.lgtweaks.systemui.behavior

import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object DisableWakeScreenOnPowerHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.android.server.power.PowerManagerService",
            lpparam.classLoader,
            "readConfigurationLocked",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    XposedHelpers.setBooleanField(param.thisObject, "mWakeUpWhenPluggedOrUnpluggedConfig", false)
                }
            }
        )
    }
}
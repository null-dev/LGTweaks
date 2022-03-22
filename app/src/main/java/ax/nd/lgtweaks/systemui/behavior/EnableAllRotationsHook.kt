package ax.nd.lgtweaks.systemui.behavior

import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.XposedHelpersExt
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Enable upside down rotation
 */
object EnableAllRotationsHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = lpparam.classLoader.loadClass("com.android.server.wm.DisplayRotation")
        XposedHelpersExt.runAfterClassConstructed(clazz) { param ->
            XposedHelpers.setIntField(param.thisObject, "mAllowAllRotations", 1)
        }
    }
}
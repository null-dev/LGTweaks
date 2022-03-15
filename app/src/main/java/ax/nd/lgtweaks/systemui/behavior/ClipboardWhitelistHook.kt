package ax.nd.lgtweaks.systemui.behavior

import android.app.AppOpsManager
import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.asAccessible
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object ClipboardWhitelistHook : Hook {
    private val WHITELIST = hashSetOf(
        "org.kde.kdeconnect_tp"
    )
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = lpparam.classLoader.loadClass("com.android.server.clipboard.ClipboardService")
        val appOpsField = clazz.getDeclaredField("mAppOps").asAccessible()
        XposedHelpers.findAndHookMethod(
            clazz,
            "clipboardAccessAllowed",
            Int::class.javaPrimitiveType!!,
            String::class.java,
            Int::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val pkg = param.args[1] as String
                    val uid = param.args[2] as Int
                    if(pkg in WHITELIST) {
                        val appOps = appOpsField.get(param.thisObject) as AppOpsManager
                        appOps.checkPackage(uid, pkg) // Check that package name matches uid
                        param.result = true
                    }
                }
            }
        )
    }
}
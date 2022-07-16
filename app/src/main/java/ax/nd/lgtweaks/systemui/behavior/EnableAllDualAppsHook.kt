package ax.nd.lgtweaks.systemui.behavior

import android.content.Context
import android.content.pm.ApplicationInfo
import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object EnableAllDualAppsHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.android.settingslib.utils.DualAppManager",
            lpparam.classLoader,
            "getDualAppWhiteList",
            Context::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    val context = param.args[0] as Context
                    val pm = context.packageManager
                    return pm.getInstalledApplications(0)
                        .filter { a -> (a.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                        .map { a -> a.packageName }
                }
            }
        )

        val pref = lpparam.classLoader.loadClass("androidx.preference.Preference")
        val prefGetTitle = pref.getDeclaredMethod("getTitle")

        XposedHelpers.findAndHookMethod(
            "com.android.settings.lge.dualapp.DualAppSettingsUIHelper",
            lpparam.classLoader,
            "getAvailableAppsPreferences",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val list = param.result as? MutableList<*>
                    list?.sortBy { it -> prefGetTitle.invoke(it).toString() }
                }
            }
        )
    }
}
package ax.nd.lgtweaks.systemui.notifications

import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object DisablePeekHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.statusbar.phone.PanelViewController",
            lpparam.classLoader,
            "runPeekAnimation",
            Long::class.javaPrimitiveType,
            Float::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            XC_MethodReplacement.returnConstant(null)
        )
    }
}
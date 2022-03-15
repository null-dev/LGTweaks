package ax.nd.lgtweaks.systemui.lockscreen

import android.graphics.Bitmap
import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object DisableLockscreenAlbumArtHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        /*
        // Intercept setHasArtwork and make it a no-op
        XposedHelpers.findAndHookMethod(
            "com.lge.lockscreen.model.KeyguardModel",
            lpparam.classLoader,
            "setHasArtwork",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            }
        )*/
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.statusbar.NotificationMediaManager",
            lpparam.classLoader,
            "finishUpdateMediaMetaData",
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            Bitmap::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            }
        )
    }
}
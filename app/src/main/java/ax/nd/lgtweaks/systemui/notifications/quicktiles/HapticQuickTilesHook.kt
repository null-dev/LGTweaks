package ax.nd.lgtweaks.systemui.notifications.quicktiles

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import android.view.View
import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object HapticQuickTilesHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.android.systemui.qs.tileimpl.QSTileBaseView",
            lpparam.classLoader,
            "init",
            View.OnClickListener::class.java,
            View.OnClickListener::class.java,
            View.OnLongClickListener::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    var listener = param.args.first() as View.OnClickListener?
                    val originalListener = listener
                    listener = View.OnClickListener {
                        it?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
                        originalListener?.onClick(it)
                    }
                    param.args[0] = listener
                }
            }
        )
    }
}
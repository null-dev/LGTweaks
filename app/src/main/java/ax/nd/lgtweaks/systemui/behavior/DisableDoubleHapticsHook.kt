package ax.nd.lgtweaks.systemui.behavior

import android.os.VibrationEffect
import android.view.HapticFeedbackConstants
import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.asAccessible
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object DisableDoubleHapticsHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val vibrationClazz = lpparam.classLoader.loadClass("com.android.server.VibratorService\$Vibration")
        val effectField = vibrationClazz.getDeclaredField("effect").asAccessible()
        val prebakedClazz = lpparam.classLoader.loadClass("android.os.VibrationEffect\$Prebaked")
        val shouldFallback = prebakedClazz.getDeclaredMethod("shouldFallback").asAccessible()
        XposedHelpers.findAndHookMethod(
            "com.android.server.VibratorService",
            lpparam.classLoader,
            "vibratorPerformEffect",
            Long::class.javaPrimitiveType!!,
            Long::class.javaPrimitiveType!!,
            vibrationClazz,
            Boolean::class.javaPrimitiveType!!,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if(param.args[0] == VibrationEffect.EFFECT_TICK.toLong()) {
                        val prebaked = param.args[2]?.let { effectField.get(it) }
                        // Only disable effect if we are not going to fallback
                        if(prebaked != null && prebakedClazz.isInstance(prebaked) && shouldFallback.invoke(prebaked) == false) {
                            // Pretend that the effect does not exist
                            param.result = 0
                        }
                    }
                }
            }
        )

        // Turn on vibration for a couple other cases where it's still good to have it on
        // Doesn't seem to matter though as most apps don't seem to respect this
        val getVibrationEffect = VibrationEffect::class.java.getDeclaredMethod(
            "get",
            Int::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!
        ).asAccessible()
        XposedHelpers.findAndHookMethod(
            "com.android.server.policy.PhoneWindowManager",
            lpparam.classLoader,
            "getVibrationEffect",
            Int::class.javaPrimitiveType!!,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    when (param.args[0]) {
                        10, // ENTRY_BUMP
                        11, // DRAG_CROSSING
                        HapticFeedbackConstants.TEXT_HANDLE_MOVE ->
                            param.result = getVibrationEffect.invoke(null, VibrationEffect.EFFECT_TICK, true)
                    }
                }
            }
        )
    }
}
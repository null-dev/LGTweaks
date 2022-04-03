package ax.nd.lgtweaks.systemui.behavior

import android.content.Context
import android.content.Intent
import android.util.Log
import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.DEBUG_LOG_TAG
import ax.nd.xposedutil.asAccessible
import ax.nd.xposedutil.withContext
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object AIKeyHandlerHook : Hook {
    private val TAG = AIKeyHandlerHook::class.simpleName

    private const val INTENT_TOGGLE_FLASHLIGHT = "ax.nd.lgtweaks.toggle-flashlight"
    private const val KEY_SINGLE_CLICK = 1
    private const val KEY_LONG_PRESS_DOWN = 2
    private const val KEY_LONG_PRESS_UP = 4

    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = lpparam.classLoader.loadClass("com.android.server.policy.HotKeyController")
        val contextField = clazz.getDeclaredField("mContext").asAccessible()
        // Ensure hotkey still works when apps go fullscreen
        lpparam.withContext(
            clazz,
            "isHotkeyLaunchEnabled",
            Boolean::class.javaPrimitiveType!!
        ) { isHotkeyLaunchEnabledContext ->
            isHotkeyLaunchEnabledContext.withContext(
                "com.android.server.policy.PhoneWindowManager\$10",
                "isFullScreen"
            ) { isFullScreen ->
                isFullScreen.enter { param ->
                    param.result = false
                }
            }
        }
        XposedHelpers.findAndHookMethod(
            clazz,
            "executeAIHotKeyPress",
            Boolean::class.javaPrimitiveType!!,
            Int::class.javaPrimitiveType!!,
            object : XC_MethodReplacement() {
                /**
                 * Shortcut for replacing a method completely. Whatever is returned/thrown here is taken
                 * instead of the result of the original method (which will not be called).
                 *
                 *
                 * Note that implementations shouldn't call `super(param)`, it's not necessary.
                 *
                 * @param param Information about the method call.
                 * @throws Throwable Anything that is thrown by the callback will be passed on to the original caller.
                 */
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    val context by lazy(LazyThreadSafetyMode.NONE) {
                        contextField.get(param.thisObject) as Context
                    }

                    // 1 = single click
                    // 2 = long press down
                    // 4 = long press up
                    try {
                        val screenOff = param.args[0] as Boolean
                        val key = param.args[1] as Int

                        if(key == KEY_LONG_PRESS_DOWN) {
                            toggleFlashlight(context)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to handle AI key press!")
                    }
                    return null
                }
            }
        )
    }

    private fun toggleFlashlight(context: Context) {
        Log.d(DEBUG_LOG_TAG, "Send intent!")
        context.sendBroadcast(Intent(INTENT_TOGGLE_FLASHLIGHT))
    }
}
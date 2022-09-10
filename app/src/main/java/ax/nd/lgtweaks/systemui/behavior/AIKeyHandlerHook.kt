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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread

object AIKeyHandlerHook : Hook {
    private val TAG = AIKeyHandlerHook::class.simpleName

    private const val INTENT_TOGGLE_FLASHLIGHT = "ax.nd.lgtweaks.toggle-flashlight"
    private const val INTENT_DOUBLE_CLICK = "ax.nd.lgtweaks.double-click"
    private const val INTENT_TRIPLE_CLICK = "ax.nd.lgtweaks.triple-click"
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
        val clickManager = ClickManager()
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
                            clickManager.reset()
                            toggleFlashlight(context)
                        } else if(key == KEY_SINGLE_CLICK) {
                            clickManager.addClick(listOf(
                                { toggleSingleClick(context) },
                                { toggleDoubleClick(context) },
                                { toggleTripleClick(context) }
                            ))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to handle AI key press!", e)
                    }
                    return null
                }
            }
        )
    }

    private fun toggleFlashlight(context: Context) {
        Log.d(TAG, "Long click!")
        context.sendBroadcast(Intent(INTENT_TOGGLE_FLASHLIGHT))
    }

    private fun toggleSingleClick(context: Context) {
        // Do nothing
    }

    private fun toggleDoubleClick(context: Context) {
        Log.d(TAG, "Double click!")
        context.sendBroadcast(Intent(INTENT_DOUBLE_CLICK))
    }

    private fun toggleTripleClick(context: Context) {
        Log.d(TAG, "Triple click!")
        context.sendBroadcast(Intent(INTENT_TRIPLE_CLICK))
    }
}

private const val TIME_BETWEEN_CLICKS = 400

class ClickManager {
    private val clickStack = mutableListOf<Long>()
    private var topClickStackCallbacks = listOf<() -> Unit>()
    private var runningThread: Thread? = null

    fun addClick(cbs: List<() -> Unit>) {
        val time = System.currentTimeMillis()
        var newThread: Thread? = null
        synchronized(this) {
            clickStack.add(time)
            topClickStackCallbacks = cbs
            if(runningThread == null) {
                newThread = thread(start = false) {
                    clickThread()
                }
                runningThread = newThread
            }
        }
        newThread?.start()
    }

    private fun clickThread() {
        // Sleep until TIME_BETWEEN_CLICKS after top click
        var lastClickIndex: Int
        while(true) {
            val topClick = synchronized(this) {
                lastClickIndex = clickStack.lastIndex
                if(lastClickIndex != -1) {
                    clickStack[lastClickIndex]
                } else null
            }
            if (topClick != null) {
                val requiredSleepTime = (topClick + TIME_BETWEEN_CLICKS) - System.currentTimeMillis()
                if(requiredSleepTime > 0) {
                    Thread.sleep(requiredSleepTime)
                } else {
                    break
                }
            }
        }
        val callback: (() -> Unit)?
        synchronized(this) {
            callback = if(lastClickIndex != -1) {
                topClickStackCallbacks.getOrNull(lastClickIndex)
            } else null

            clickStack.clear()
            topClickStackCallbacks = emptyList()
            runningThread = null
        }
        callback?.invoke()
    }

    fun reset() {
        synchronized(this) {
            clickStack.clear()
            topClickStackCallbacks = emptyList()
        }
    }
}
package ax.nd.xposedutil

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import java.lang.Exception

object XposedHelpersExt {
    /**
     * Log the stack trace and call arguments for any calls to this method
     */
    fun findAndLogCallsToMethod(className: String, classLoader: ClassLoader, methodName: String, vararg parameterTypes: Any) {
        XposedHelpers.findAndHookMethod(className, classLoader, methodName, *parameterTypes, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                Log.d(DEBUG_LOG_TAG, "[$methodName] called, full call path: $className.$methodName")
                Log.d(DEBUG_LOG_TAG, "[$methodName] called, args: ${param.args.contentToString()}")
                Log.d(DEBUG_LOG_TAG, "[$methodName] this: ${param.thisObject}")
                Log.d(DEBUG_LOG_TAG, "[$methodName] stack trace:", Exception())
            }

            override fun afterHookedMethod(param: MethodHookParam) {
                Log.d(DEBUG_LOG_TAG, "[$methodName] result: ${param.result}")
            }
        })
    }
}
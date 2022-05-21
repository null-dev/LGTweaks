package ax.nd.lgtweaks.systemui.behavior

import android.os.Handler
import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.asAccessible
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

interface UpdateEngineCallbackProxy {
    fun onPayloadApplicationComplete(errorCode: Int)
    fun onStatusUpdate(status: Int, percent: Float)
}

object DisableSystemUpdatesHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val updateEngine = lpparam.classLoader.loadClass("android.os.UpdateEngine")
        for(constructor in updateEngine.constructors) {
            XposedBridge.hookMethod(
                constructor,
                XC_MethodReplacement.DO_NOTHING,
            )
        }

        val updateEngineCallbackClazz = lpparam.classLoader.loadClass("android.os.UpdateEngineCallback")
        val onPayloadApplicationCompleteMethod = updateEngineCallbackClazz.declaredMethods.find { it.name == "onPayloadApplicationComplete" }!!
        val onStatusUpdateMethod = updateEngineCallbackClazz.declaredMethods.find { it.name == "onStatusUpdate" }!!
        val updateEngineCallbackFieldName = "update_engine_callback"

        fun getProxy(param: XC_MethodHook.MethodHookParam): UpdateEngineCallbackProxy? {
            return XposedHelpers.getAdditionalInstanceField(param.thisObject, updateEngineCallbackFieldName) as? UpdateEngineCallbackProxy
        }

        XposedHelpers.findAndHookMethod(
            updateEngine,
            "bind",
            updateEngineCallbackClazz,
            Handler::class.java,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    val handler = param.args[1] as? Handler
                    val proxy = object : UpdateEngineCallbackProxy {
                        override fun onPayloadApplicationComplete(errorCode: Int) {
                            fun invoke() = onPayloadApplicationCompleteMethod.invoke(param.args[0], errorCode)
                            if(handler != null) {
                                handler.post {
                                    invoke()
                                }
                            } else {
                                invoke()
                            }
                        }

                        override fun onStatusUpdate(status: Int, percent: Float) {
                            fun invoke() = onStatusUpdateMethod.invoke(param.args[0], status, percent)
                            if(handler != null) {
                                handler.post {
                                    invoke()
                                }
                            } else {
                                invoke()
                            }
                        }
                    }
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, updateEngineCallbackFieldName, proxy)
                    proxy.onStatusUpdate(UPDATE_STATUS_DISABLED, 0f)
                    return true
                }
            }
        )

        val allocateSpaceResultClazz = lpparam.classLoader.loadClass("android.os.UpdateEngine\$AllocateSpaceResult")
        val allocateSpaceResultConstructor = allocateSpaceResultClazz.declaredConstructors.first().asAccessible()
        val allocateSpaceResult = allocateSpaceResultConstructor.newInstance()
        XposedHelpers.setIntField(allocateSpaceResult, "mErrorCode", ERROR_CONSTANT_DEVICE_CORRUPTED)

        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "allocateSpace" },
            XC_MethodReplacement.returnConstant(allocateSpaceResult),
        )

        for(applyPayloadMethod in updateEngine.declaredMethods.filter { it.name == "applyPayload" }) {
            XposedBridge.hookMethod(
                applyPayloadMethod,
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any? {
                        getProxy(param)?.apply {
                            onStatusUpdate(UPDATE_STATUS_DISABLED, 0f)
                            onPayloadApplicationComplete(ERROR_CONSTANT_DEVICE_CORRUPTED)
                        }
                        return null
                    }
                }
            )
        }

        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "cancel" },
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any? {
                    getProxy(param)?.apply {
                        onStatusUpdate(UPDATE_STATUS_DISABLED, 0f)
                    }
                    return null
                }
            }
        )
        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "resetStatus" },
            XC_MethodReplacement.DO_NOTHING
        )
        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "resume" },
            XC_MethodReplacement.DO_NOTHING
        )
        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "suspend" },
            XC_MethodReplacement.DO_NOTHING
        )

        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "cleanupAppliedPayload" },
            XC_MethodReplacement.returnConstant(ERROR_CONSTANT_DEVICE_CORRUPTED)
        )
        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "unbind" },
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam): Any {
                    XposedHelpers.setAdditionalInstanceField(param.thisObject, updateEngineCallbackFieldName, null)
                    return true
                }
            }
        )
        XposedBridge.hookMethod(
            updateEngine.declaredMethods.find { it.name == "verifyPayloadMetadata" },
            XC_MethodReplacement.returnConstant(false)
        )
    }

    /**
     * Error code: the device is corrupted and no further updates may be applied.
     *
     *
     * See [UpdateEngine.cleanupAppliedPayload].
     */
    const val ERROR_CONSTANT_DEVICE_CORRUPTED = 61

    /**
     * Update status code: update engine is in disabled state.
     */
    const val UPDATE_STATUS_DISABLED = 9
}
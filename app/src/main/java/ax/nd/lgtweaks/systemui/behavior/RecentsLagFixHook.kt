package ax.nd.lgtweaks.systemui.behavior

import ax.nd.lgtweaks.Hook
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Fix lag when opening recents page
 * Lag was caused by an animation being GCed before it could be played due to it being held
 * in a WeakRef.
 */
object RecentsLagFixHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val wrapper = lpparam.classLoader.loadClass("com.android.launcher3.WrappedLauncherAnimationRunner")
        val rap = lpparam.classLoader.loadClass("com.android.quickstep.util.RemoteAnimationProvider")
        val targettedRap = lpparam.classLoader.loadClass("com.android.quickstep.OverviewCommandHelper\$RecentsActivityCommand\$1")
        val runnerImpl = lpparam.classLoader.loadClass("com.android.launcher3.WrappedAnimationRunnerImpl")
        val runnerImplMethod = runnerImpl.declaredMethods.find { it.name == "onCreateAnimation" }!!

        val inTargetRap = ThreadLocal.withInitial { false }
        XposedBridge.hookMethod(
            rap.declaredMethods.find { it.name == "toActivityOptions" },
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if(param.thisObject.javaClass === targettedRap)
                        inTargetRap.set(true)
                }

                override fun afterHookedMethod(param: MethodHookParam) {
                    if(param.thisObject.javaClass === targettedRap)
                        inTargetRap.set(false)
                }
            }
        )
        val strongRefName = "runner_strong"
        XposedBridge.hookMethod(
            wrapper.constructors.first(),
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if(inTargetRap.get()!!) {
                        XposedHelpers.setAdditionalInstanceField(
                            param.thisObject,
                            strongRefName,
                            param.args[0]
                        )
                    }
                }
            }
        )
        XposedBridge.hookMethod(
            wrapper.declaredMethods.find { it.name == "onCreateAnimation" },
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val strongRef = XposedHelpers.getAdditionalInstanceField(param.thisObject, strongRefName)
                    if(strongRef != null) {
                        runnerImplMethod.invoke(strongRef, param.args[0], param.args[1], param.args[2])
                        param.result = null
                    }
                }
            }
        )
    }
}
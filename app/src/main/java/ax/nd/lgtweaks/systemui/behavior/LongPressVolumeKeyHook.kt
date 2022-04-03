package ax.nd.lgtweaks.systemui.behavior

import android.app.AndroidAppHelper
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.IOnVolumeKeyLongPressListener
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.View
import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.asAccessible
import ax.nd.xposedutil.getSystemService
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

object LongPressVolumeKeyHook : Hook {
    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.android.server.media.MediaSessionService",
            lpparam.classLoader,
            "onStart",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = XposedHelpers.getObjectField(param.thisObject, "mContext") as Context
                    val sessionManager = XposedHelpers.getObjectField(param.thisObject, "mSessionManagerImpl")
                    val listener = object : IOnVolumeKeyLongPressListener.Stub() {
                        override fun onVolumeKeyLongPress(p0: KeyEvent) {
                            if(p0.action == ACTION_DOWN && p0.repeatCount == 0) {
                                when (p0.keyCode) {
                                    KEYCODE_VOLUME_UP -> sendMediaButtonEvent(
                                        context,
                                        KEYCODE_MEDIA_NEXT
                                    )
                                    KEYCODE_VOLUME_DOWN -> sendMediaButtonEvent(
                                        context,
                                        KEYCODE_MEDIA_PREVIOUS
                                    )
                                }
                            }
                        }
                    }
                    XposedHelpers.callMethod(
                        sessionManager,
                        "setOnVolumeKeyLongPressListener",
                        arrayOf(IOnVolumeKeyLongPressListener::class.java),
                        listener
                    )
                }
            }
        )
        // Fix long press not being fired when screen off on app that uses volume keys
        /*XposedHelpers.findAndHookMethod(
            "com.android.server.media.MediaKeyDispatcher",
            lpparam.classLoader,
            "getOverriddenKeyEvents",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val context = AndroidAppHelper.currentApplication()
                    val displayManager: DisplayManager? = context.getSystemService()
                    if (displayManager == null) {
                        Log.w(LongPressVolumeKeyHook::class.simpleName, "Display manager is null!")
                    } else {
                        val mainDisplayOn = displayManager.getDisplay(Display.DEFAULT_DISPLAY)?.state == Display.STATE_ON
                        // Screen off, say we are not locked
                        if (!mainDisplayOn) {
                            // Disable overriding of the volume keys when we are locked
                            param.result?.let { result ->
                                result as MutableMap<Int, Int>
                                result.put(KEYCODE_VOLUME_UP, 0)
                                result.put(KEYCODE_VOLUME_DOWN, 0)
                            }
                        }
                    }
                }
            }
        )*/
    }
    fun setupSysUi(lpparam: XC_LoadPackage.LoadPackageParam) {
        /*XposedHelpersExt.findAndLogCallsToMethod(
            "android.media.session.ISessionManager.Stub.Proxy",
            lpparam.classLoader,
            "dispatchVolumeKeyEvent",
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType!!,
            KeyEvent::class.java,
            Int::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!
        )*/
        val inputStageClazz = Class.forName("android.view.ViewRootImpl\$InputStage")
        val viewRootImplClazz = Class.forName("android.view.ViewRootImpl")
        val queuedInputEventClazz = Class.forName("android.view.ViewRootImpl\$QueuedInputEvent")
        val mEventField = queuedInputEventClazz.getDeclaredField("mEvent").asAccessible()
        val thisField = inputStageClazz.getDeclaredField("this$0").asAccessible()
        val mViewField = viewRootImplClazz.getDeclaredField("mView").asAccessible()
        val mAddedField = viewRootImplClazz.getDeclaredField("mAdded").asAccessible()
        // Fix shouldDropInputEvent falsely dropping volume key events on screen off
        // It complains that SystemUI doesn't have focus, but SystemUI should always have focus when
        // the screen is off.
        XposedHelpers.findAndHookMethod(
            "android.view.ViewRootImpl.InputStage",
            lpparam.classLoader,
            "shouldDropInputEvent",
            queuedInputEventClazz,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if(param.result == true) {
                        val us = param.thisObject
                        val this0 = thisField.get(us)
                        val mView = mViewField.get(this0) as View?
                        val mAdded = mAddedField.get(this0) as Boolean
                        if (mView == null || !mAdded) {
                            // root view removed, super judgement is correct
                            return
                        }

                        val queuedInputEvent = param.args.first()
                        val mEvent = queuedInputEvent?.let {
                            mEventField.get(queuedInputEvent)
                        }
                        if (mEvent is KeyEvent) {
                            val keyCode = mEvent.keyCode
                            if (keyCode == KEYCODE_VOLUME_UP || keyCode == KEYCODE_VOLUME_DOWN) {
                                val powerManager: PowerManager? =
                                    AndroidAppHelper.currentApplication().getSystemService()
                                if (powerManager?.isInteractive == false) {
                                    Log.d(LongPressVolumeKeyHook::class.simpleName, "Overriding drop of volume key!")

                                    param.result = false
                                    return
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    private fun sendMediaButtonEvent(context: Context, code: Int) {
        val eventtime = SystemClock.uptimeMillis()
        val keyIntent = Intent(Intent.ACTION_MEDIA_BUTTON, null)
        var keyEvent: KeyEvent? = KeyEvent(eventtime, eventtime, KeyEvent.ACTION_DOWN, code, 0)
        keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        dispatchMediaButtonEvent(context, keyEvent)
        keyEvent = KeyEvent.changeAction(keyEvent, KeyEvent.ACTION_UP)
        keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
        dispatchMediaButtonEvent(context, keyEvent)
    }

    private fun dispatchMediaButtonEvent(context: Context, keyEvent: KeyEvent?) {
        val audioManager: AudioManager? = context.getSystemService()
        audioManager?.dispatchMediaKeyEvent(keyEvent)
    }
}
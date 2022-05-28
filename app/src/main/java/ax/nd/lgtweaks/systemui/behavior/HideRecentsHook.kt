package ax.nd.lgtweaks.systemui.behavior

import android.content.ComponentName
import android.content.pm.ActivityInfo
import ax.nd.lgtweaks.Hook
import ax.nd.xposedutil.asAccessible
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Do not display window content for some apps in recents
 */
object HideRecentsHook : Hook {
    private val RECENTS_BLACKLIST = hashSetOf(
        "jp.pxv.android", // Pixiv
        "eu.kanade.tachiyomi.sy", // TachiyomiSy
        "onlymash.flexbooru.play", // Flexbooru
        "com.andrewshu.android.reddit" // reddit is fun
    )

    override fun setup(lpparam: XC_LoadPackage.LoadPackageParam) {
        val clazz = lpparam.classLoader.loadClass("com.android.server.wm.TaskSnapshotController")
        val taskClazz = lpparam.classLoader.loadClass("com.android.server.wm.Task")

        val topRunningActivityMethod = taskClazz.getDeclaredMethod("topRunningActivity").asAccessible()
        val activityRecordClazz = lpparam.classLoader.loadClass("com.android.server.wm.ActivityRecord")
        val activityInfoField = activityRecordClazz.getDeclaredField("info").asAccessible()

        val origActivityField = taskClazz.getDeclaredField("origActivity").asAccessible()
        val realActivityField = taskClazz.getDeclaredField("realActivity").asAccessible()

        XposedHelpers.findAndHookMethod(
            clazz,
            "getSnapshotMode",
            taskClazz,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val task = param.args[0] ?: return

                    // Find top activity
                    val record = topRunningActivityMethod.invoke(task) ?: return
                    val activityInfo = activityInfoField.get(record) as? ActivityInfo ?: return
                    val topActivityPackageName = activityInfo.applicationInfo?.packageName

                    // Find app that task belongs to
                    val origActivity = origActivityField.get(task) as? ComponentName
                    val realActivity = realActivityField.get(task) as? ComponentName
                    // Same calculation as in Launcher3
                    val sourceComponent = origActivity ?: realActivity
                    val appPackageName = sourceComponent?.packageName

                    if(topActivityPackageName in RECENTS_BLACKLIST
                        || appPackageName in RECENTS_BLACKLIST) {
                        param.result = 1 // SNAPSHOT_MODE_APP_THEME
                    }
                    // Otherwise, use super judgement
                }
            }
        )
    }
}
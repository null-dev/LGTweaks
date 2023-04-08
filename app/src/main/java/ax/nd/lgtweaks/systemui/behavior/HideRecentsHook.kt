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
    private val BLACKLISTED_PACKAGE_NAMES = hashSetOf(
        "jp.pxv.android", // Pixiv
        "eu.kanade.tachiyomi.sy", // TachiyomiSy
        "onlymash.flexbooru.play", // Flexbooru
        "com.andrewshu.android.reddit", // reddit is fun
        "com.klinker.android.twitter_l", // Talon

        "com.project.nutaku", // Nutaku

        "org.mozilla.fennec_fdroid.nn9", // Fennec NN9
    )

    // Uses matchEntire()
    //language=regexp
    private val BLACKLISTED_PACKAGE_NAME_REGEXES = hashSetOf(
        // Anything made by Pinkcore (e.g. TenkafuMA!, Daraku Gear)
        """com\.pinkcore\..+""",
        // Anything made by CrispyTofuGames (e.g. Tavern of Sins)
        """com\.crispytofu\..+"""
    ).map { Regex(it) }

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

                    if(isPackageNameBlacklisted(topActivityPackageName) || isPackageNameBlacklisted(appPackageName)) {
                        param.result = 1 // SNAPSHOT_MODE_APP_THEME
                    }
                    // Otherwise, use super judgement
                }
            }
        )
    }

    private fun isPackageNameBlacklisted(packageName: String?): Boolean {
        if(packageName == null) return false
        return packageName in BLACKLISTED_PACKAGE_NAMES || BLACKLISTED_PACKAGE_NAME_REGEXES.any {
            it.matchEntire(packageName) != null
        }
    }
}
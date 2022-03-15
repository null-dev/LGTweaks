package ax.nd.lgtweaks

import de.robv.android.xposed.callbacks.XC_LoadPackage

interface Hook {
    fun setup(lpparam: XC_LoadPackage.LoadPackageParam)
}
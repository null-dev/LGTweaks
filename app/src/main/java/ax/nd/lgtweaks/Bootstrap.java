package ax.nd.lgtweaks;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Delegates all operations to Module.kt. This is mainly just to Xposed doesn't interact wierdly with
 * Kotlin.
 */
public class Bootstrap implements IXposedHookLoadPackage {
    private final Module module = new Module();

    /**
     * This method is called when an app is loaded. It's called very early, even before
     * {@link Application#onCreate} is called.
     * Modules can set up their app-specific hooks here.
     *
     * @param lpparam Information about the app.
     * @throws Throwable Everything the callback throws is caught and logged.
     */
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        module.handleLoadPackage(lpparam);
    }
}

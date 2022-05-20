package ax.nd.lgtweaks.systemui.behavior

import java.util.*

class MemoryLeakDetector {
    private val tracker = WeakHashMap<Any, Unit>()

    fun track(obj: Any) {
        tracker[obj] = Unit
    }

    fun count(): Int {
        return tracker.size
    }
}
package android.view;

public final class ViewRootImpl {
    public View mView = null;
    public boolean mAdded = false;
    public static final class QueuedInputEvent {
        public QueuedInputEvent mNext;

        public InputEvent mEvent;
        public int mFlags;

        @Override
        public String toString() {
            throw new RuntimeException("Stub!");
        }
    }

    public abstract class InputStage {
        public ViewRootImpl this$0 = null;
    }
}

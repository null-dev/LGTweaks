package android.media.session;

import android.view.*;
import android.os.*;

public interface IOnVolumeKeyLongPressListener extends IInterface
{
    void onVolumeKeyLongPress(final KeyEvent p0) throws RemoteException;

    public abstract static class Stub extends Binder implements IOnVolumeKeyLongPressListener
    {
        public Stub() {
        }

        public IBinder asBinder() {
            return (IBinder)this;
        }
    }
}

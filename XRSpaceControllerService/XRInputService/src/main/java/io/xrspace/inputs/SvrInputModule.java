//-----------------------------------------------------------------------------
//  Copyright (c) 2017 Qualcomm Technologies, Inc.
//  All Rights Reserved. Qualcomm Technologies Proprietary and Confidential.
//-----------------------------------------------------------------------------

package io.xrspace.inputs;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
//import android.os.Handler;
import android.os.IBinder;
//import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.qualcomm.svrapi.controllers.IControllerInterface;
import com.qualcomm.svrapi.controllers.IControllerInterfaceCallback;

/**
 *
 */
@SuppressLint("LongLogTag")
@SuppressWarnings("unused")
public abstract class SvrInputModule extends Service {

    IControllerInterfaceCallback callback;

    /**
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        //TODO: stopAll()
        SvrInputModule.this.ControllerStopAll();
        return new IControllerInterface.Stub() {

            @Override
            public void registerCallback(IControllerInterfaceCallback cb) throws RemoteException {
                callback = cb;
            }

            @Override
            public void unregisterCallback() throws RemoteException {
                callback = null;
            }

            @Override
            public void Start(int handle, String desc, ParcelFileDescriptor pfd, int fdSize, ParcelFileDescriptor qvrFd, int qvrFdSize) throws RemoteException {
                SvrInputModule.this.ControllerStart(handle, desc, pfd, fdSize);
                Log.e(TAG, "Start");
            }

            @Override
            public void Stop(int handle) throws RemoteException {
                Log.e(TAG, "Stop");
                SvrInputModule.this.ControllerStop(handle);
            }

            @Override
            public void SendMessage(int handle, int type, int arg1, int arg2) throws RemoteException {
                Log.e(TAG, "SendMessage");
                SvrInputModule.this.ControllerSendMessage(handle, type, arg1, arg2);
            }

            @Override
            public int QueryInt(int handle, int what) throws RemoteException {
                return SvrInputModule.this.ControllerQueryInt(handle, what);
            }

            @Override
            public String QueryString(int handle, int what) throws RemoteException {
                //Log.e(TAG, "QueryString");
                return SvrInputModule.this.ControllerQueryString(handle, what);
            }
        };
    }

    /**
     *
     * @param intent
     * @return
     */
    @Override
    public boolean onUnbind(Intent intent) {
        boolean bResult = super.onUnbind(intent);
        Log.v(TAG,"onUnbind!");
        SvrInputModule.this.ControllerExit();
        return bResult;
    }

    protected abstract void ControllerStop(int handle);
    protected abstract void ControllerStart(int handle, String desc, ParcelFileDescriptor pfd, int fdSize);
    protected abstract void ControllerSendMessage(int handle, int what, int arg1, int arg2);
    protected abstract int ControllerQueryInt(int handle, int what);
    protected abstract void ControllerStopAll( );
    protected abstract void ControllerExit();
    protected abstract String ControllerQueryString(int handle, int what);
    private static final String TAG = "XRSpace-SvrControllerModule";
}

package io.xrspace.inputs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
//import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import static java.lang.Thread.sleep;

public class XRSpaceInputService extends SvrInputModule {


    private static final String TAG = "XRSpace-Module";
    private int count = 0;
    private static int MonoPics = 0;
    private static ScheduledThreadPoolExecutor mTaskScheduler;
    List<DeviceInfo> listOfDevices = new ArrayList<>();
    boolean bInitialized = false;
    boolean projectLightOn = false;
    int handPosLoseCount = 0;
    private Handler mHandlerTime = new Handler();
    private static boolean monoMode = false;
    private static boolean fakeMode = false;
    private static boolean getMonoPic = false;
    XRSpaceInputService.DeviceInfo di = null;
    final static int threadCore = 1;
    public static native void bindToCpu(int cpu);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    class DeviceInfo {
        String identifier;
        boolean isInUse;

        DeviceInfo(String s) {
            identifier = s;
            isInUse = false;
        }

        void MarkInUse() {
            isInUse = true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    void updateDeviceList() {
        listOfDevices.add(new DeviceInfo("XCobra-" + count));
    }

    static {
        System.loadLibrary("affinity");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG,"Service onCreate!");
    }

    private static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private static String getStringFromFile(String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();

        return ret;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public void ControllerStop(int id) {
        Log.e(TAG, "ControllerStop id = " + id);
        if(bInitialized) {
            bInitialized = false;
            mHandlerTime.removeCallbacks(timerRun);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    DeviceInfo GetAvailableDevice() {
        DeviceInfo di = null;
        for (int i = 0; i < listOfDevices.size(); i++) {
            if (listOfDevices.get(i).isInUse == false) {
                di = listOfDevices.get(i);
                break;
            }
        }
        return di;
    }

    /**
     * Initialize
     */
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void initializeIfNeeded() {
        if (bInitialized == false) {
            //CVinitConfig();

        }
        bInitialized = true;
        updateDeviceList();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void ControllerStart(int handle, final String desc, ParcelFileDescriptor pfd, int fdSize) {
        Log.e(TAG, "ControllerStart, handle is " + handle);
        initializeIfNeeded();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected void ControllerExit() {
            //XDeviceApi.exit();
            Log.v(TAG, "ControllerExit!!: killProcess for controller connection");
            //stopSelf();
            Log.v(TAG, "ControllerExit!!: killProcess for controller connection End");
            android.os.Process.killProcess(android.os.Process.myPid());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void ControllerStopAll() {

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    protected int ControllerQueryInt(int handle, int what) {
        int result = 0;

        //if (controllerContext != null) {
            switch (what) {
                case SvrControllerApi.svrControllerQueryType_kControllerQueryBatterRemaining:
                    //result = XDeviceApi.getInt(controllerContext.deviceId, XDeviceConstants.kField_BatteryLevel, 0);
                    break;
                case SvrControllerApi.svrControllerQueryType_kControllerQueryControllerCaps:
                    //Log.e(TAG, "ControllerStart Query Capability");
                    result = 0;
                    break;
                case SvrControllerApi.svrControllerQueryType_kControllerQueryActiveButtons:
                    //Log.e(TAG, "ControllerStart Query ActiveButtons");
                    result = 4;
                    break;
                case SvrControllerApi.svrControllerQueryType_kControllerQueryActive2DAnalogs:
                    //Log.e(TAG, "ControllerStart Query Active2DAnalogs");
                    result = 1;
                    break;
                case SvrControllerApi.svrControllerQueryType_kControllerQueryActive1DAnalogs:
                    //Log.e(TAG, "ControllerStart Query Active1DAnalogs");
                    result = 0;
                    break;
                case SvrControllerApi.svrControllerQueryType_kControllerQueryActiveTouchButtons:
                    //Log.e(TAG, "ControllerStart Query ActiveTouchButtons");
                    result = 1;
                    break;
            }
        //}
        return result;
    }

    protected String ControllerQueryString(int handle, int what) {
        String result = null;
        //if(handle ==  gDeviceConfigList.size()) return "";
        //ControllerContext controllerContext = listOfControllers.get(gDeviceConfigList.get(handle));
        //if (controllerContext != null) {
            switch (what) {
                case SvrControllerApi.svrControllerQueryType_kControllerQueryDeviceManufacturer:
                    //Log.e(TAG, "ControllerStart Query DeviceManufacturer");
                    result = "XRS" + handle;
                    break;
                case SvrControllerApi.svrControllerQueryType_kControllerQueryDeviceIdentifier:
                    //Log.e(TAG, "ControllerStart Query DevziceIdentifier");
                    result = "1234 - " + handle;
                    break;
            }

        //}
        return result;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void ControllerSendMessage(int handle, int what, int arg1, int arg2) {
        //TODO:
        //if(handle == gDeviceConfigList.size()) {
        //    Log.e(TAG, "the handle is much than list size! Return!");
        //    return;
        //}
        //Log.v(TAG,"the handle is " + handle + "what is " + what + "arg1 = " + arg1);
        ///ControllerContext controllerContext = listOfControllers.get(handle);
        //if (controllerContext != null) {
            switch (what) {
                case SvrControllerApi.svrControllerMessageType_kControllerMessageRecenter:
                    //controllerContext.reset();
                    //gLeMgr.reCenter("XCobra-0");
                    break;
                case SvrControllerApi.svrControllerMessageType_kControllerMessageGetMonoPic:
                    if(getMonoPic == false){
                        String sDir = "/sdcard/DCIM/Camera/Mono/";
                        File dir = new File(sDir);
                        deleteAllFiles(dir);

                        getMonoPic = true;
                        MonoPics = 0;
                        Log.v(TAG, "GetMonoPic::getMonoPic: " + getMonoPic);
                    }
                    //mHandlerTime.postDelayed(timerRun2,5000);
                    break;
            }
        //} else {
            //Log.e(TAG, "controllerContext == null!");
        //}
    }

    private String copyAssetsFile(Context c, String src, String dest) {
        byte[] buffer = new byte[1024];
        int read;
        InputStream inFd;
        OutputStream out;
        Boolean status = true;
        File outFile;
        Log.v(TAG, "the path is " + c.getExternalFilesDir(null));
        try {
            File f1 = new File(c.getExternalFilesDir(null), src);
            if (f1.exists()) f1.delete();
            inFd = c.getAssets().open(src);
            outFile = new File(c.getExternalFilesDir(null), dest);
            out = new FileOutputStream(outFile);
            while (status) {
                read = inFd.read(buffer);
                if (read != -1)
                    out.write(buffer, 0, read);
                else
                    status = false; }return outFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "Exception! " + e.toString()); }return "";
    }

    private final Runnable timerRun = new Runnable()
    {
        public void run()
        {
            mHandlerTime.postDelayed(this, 3000);
            //Log.d(TAG, "dFrameInfo : depth fps = " + mMonitorFPS.getDepthFPS()/3 +
            //        ", mono input fps = " + mMonitorFPS.getMonoFPS()/3 +
            //        ", Hmi input fps = " + (int)Math.ceil(mMonitorFPS.getHMIFPS()/3) +
            //        ", Update fps = " + (int)Math.ceil(mMonitorFPS.getUpdateFPS()/3));
            //mMonitorFPS.CleanFPS();

            int mode = readProp("fakemode");
            //Log.i(TAG, "readProp::Echo_fakemode = " + mode);

            if(mode == 1 && fakeMode == false){
                fakeMode = true;
                Log.v(TAG, "Set fake Mode!" + fakeMode);
            }else if(mode == 0 && fakeMode == true){
                fakeMode = false;
                Log.v(TAG, "Set fake Mode!" + fakeMode);
            }
        }
    };

    private int readProp(String prop) {
        try {
            Process process = Runtime.getRuntime().exec("getprop " + prop);
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            BufferedReader input = new BufferedReader(ir);
            String value = input.readLine();
            StringBuilder sb = new StringBuilder();

            if(!value.equals("")){
                sb.append(value);
                value = sb.toString();
                //Log.i(TAG, "readProp:: " + prop + " value: " + value);
                return Integer.parseInt(value);
            }else{
                //Log.i(TAG, "readProp::prop value don't exist");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "readProp::exception");
        }
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG,"onDestroy and Release CVService and HMI!");
        //Log.v(TAG,"onDestroy and Release CVService and HMI End!");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(TAG, "onUnbind!");
        return super.onUnbind(intent);
    }


    private void deleteAllFiles(File root) {
        File files[] = root.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteAllFiles(f);
                    try {
                        f.delete();
                    } catch (Exception e) {
                    }
                } else {
                    if (f.exists()) {
                        deleteAllFiles(f);
                        try {
                            f.delete();
                        } catch (Exception e) {
                        }
                    }
                }
            }
    }

    //Test code
    public static void writeToFile(byte[] array, int index, long timestamp)
    {
        //byte[] buffer = new byte[1280*400];
        byte[] buffer = array;
        try
        {
            String sDir = "/sdcard/DCIM/Camera/Mono/";
            String path = sDir + Integer.toString(index) + "-" + Long.toString(timestamp) + ".pgm";
            File destDir = new File(sDir);
            File file = new File(path);
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            if(!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream stream = new FileOutputStream(file,true);
            String header = "P5\n" + Integer.toString(1280) + " " + Integer.toString(400) + "\n255\n";
            stream.write(header.getBytes());
            stream.write(buffer);
        } catch (FileNotFoundException e1) {
            Log.v(TAG,"the error is " + e1.toString());
        } catch (IOException e) {
            Log.v(TAG,"the error is " + e.toString());
        }
    }
}

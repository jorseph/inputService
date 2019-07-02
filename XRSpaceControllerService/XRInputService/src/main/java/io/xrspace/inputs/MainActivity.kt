package io.xrspace.controllers

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.PermissionChecker
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import android.widget.ListView
import io.xrspace.acedia.LeDeviceUtils.LeDeviceUtilsDaydream
import io.xrspace.acedia.LeDeviceUtils.LeDeviceUtilsXR
import io.xrspace.controllers.BLE.*
import io.xrspace.hmi.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_scan_list.view.*
import java.io.*
import java.util.*

class MainActivity : AppCompatActivity(), LibBleManager.LibBleScanListener, LibBleManager.LibBleDeviceListener {
    //>> variable ==================================================================================
    //- debug and unclassified
    private val kTag = this::class.java.simpleName

    //- BLE
    private val kRequestCodeForAccessCoarseLocation = 1
    private val kRequestCodeForAccessWriteExternalStorageStats = 2
    private val kRequestCodeForAccessReadExternalStorageStats = 3
    private val kMaxDeviceConnected = 6
    private lateinit var gLeMgr: LibBleManager
    //- 2018.10.09
    private val gLeConfig = LibBleConfig()
    //- 2018.10.17
    private var gCmdCnt = 0

    //- UI
    private lateinit var gViewScanList: View
    private lateinit var gLvScanList: ListView
    private val gDeviceConfigList = ArrayList<LibBleDeviceConfig>()
    private var gDlgScanList: AlertDialog? = null
    private var gAdptScanList: MutiDeviceScanAdapter? = null
    private var gCnctList = ArrayList<BluetoothDevice>()
    private var gAdptCnctList: AdapterConnect? = null
    private val gIIIConfig = LibMotionGestureConfig()
    private var gXimmerseConfig = AssetsFileToXimmerseConfig()
    //<< variable ----------------------------------------------------------------------------------

    //>> lifecycle =================================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //- BLE
        //-- (LeHint) Create LibBleManager to handler all about BLE control
        //-- (LeHint) Before here you need to used LibBleManager.getInstance in your MainActivity
        gLeMgr = LibBleManager.getInstance()
        //-- set BLE config
        //-- (LeHint) If you want not listen some of those callback, just using "null" here.
        //-- 2018.10.09
        gLeConfig.listenerScan = this
        gLeConfig.listenerDevice = this
        gLeConfig.devices.add(DeviceXimmerse())
        gLeConfig.devices.add(DeviceMOTi())
        gLeConfig.devices.add(LeDeviceUtilsDaydream())
        gLeConfig.devices.add(LeDeviceUtilsXR())
        //gLeConfig.devices.add(DeviceMOTi())
        //-- (LeHint) Register callback to getting message from LibBleManager
        gLeMgr.register(gLeConfig)
        gXimmerseConfig.SetAssetsFileToXimmerseConfig(this.applicationContext)
        //gXimmerseConfig = AssetsFileToXimmerseConfig(this.applicationContext)
        motionGestureInit()

        //- set scan button clicked event
        LMainAct_btn.setOnClickListener {
            //- disconnected device
            gLeMgr.disconnectLeDevice()
            //- clear and init. UI
            gLvScanList.clearChoices()
            gAdptScanList?.clear()
            gCnctList.clear()
            gAdptCnctList?.clear()
            gDlgScanList?.setTitle(getString(R.string.dlg_title, gLvScanList.checkedItemCount.toString()))
            gDlgScanList?.show()
            //- scan
            //-- (LeHint) If you want to scan device and get their address, you can use this function to scan.
            gLeMgr.scanLeDevice(LibBleManager.COMMAND.START)
        }

        //- setup scan device list dialog
        gViewScanList = layoutInflater.inflate(R.layout.dialog_scan_list, null)
        gLvScanList = gViewScanList.LMutidevice_alert_dlg_list_view
        gAdptScanList = MutiDeviceScanAdapter(this)
        gLvScanList.adapter = gAdptScanList
        gLvScanList.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        gLvScanList.setOnItemClickListener { _, _, position, _ ->
            if (kMaxDeviceConnected >= gLvScanList.checkedItemCount)
                gDlgScanList?.setTitle(getString(R.string.dlg_title, gLvScanList.checkedItemCount.toString()))
            else
                gLvScanList.setItemChecked(position, false)
        } // setOnItemClickListener()

        gAdptCnctList = AdapterConnect(this)
        LMainAct_recyclerview.layoutManager = LinearLayoutManager(this)
        LMainAct_recyclerview.adapter = gAdptCnctList
        gDlgScanList = AlertDialog.Builder(this)
                .setTitle(getString(R.string.dlg_title, gLvScanList.checkedItemCount.toString()))
                .setIcon(R.drawable.abc_ic_search_api_material)
                .setView(gViewScanList)
                .setPositiveButton("Connect", fun(_: DialogInterface, _: Int) {
                    for (idx in 0 until gLvScanList.adapter.count) {
                        if (gLvScanList.checkedItemPositions[idx]) {
                            //-- (LeHint) Recorded device's address, those are for BLE Manager to connected device
                            //-- (LeHint) If you has already know your device's address, you can add it directly without using dialog to scan and get.
                            gCnctList.add((gAdptScanList?.getItem(idx) as BluetoothDevice))
                        } // if()
                    } // for()
                    //-- (LeHint) When you have prepared your connect devices address list, you can call this function to connecting them.
                    var count = 0
                    var count1 = 0
                    var count2 = 0
                    for(s in gCnctList) {
                        var name = s.name
                        var address = s.address
                        Log.v(kTag, "the name is " + name + " the address is " + address)
                        if((name.contains("X3C01")) || (address.contains("F4:81"))) {
                            if(count1 == 0) {
                                gXimmerseConfig.WriteXimmerseConfig(address)
                                gDeviceConfigList.add(setConfig(LibDeviceId.EVENT_TYPE_CONTROLLER_RIGHT_HAND, address))
                            }
                            else if(count1 == 1){
                                gDeviceConfigList.add(setConfig(LibDeviceId.EVENT_TYPE_CONTROLLER_LEFT_HAND, address))
                            }
                            count1++;
                        }
                        else if(address.contains("DD:D6:EB:EF")) {  //Hand ring
                            if (count == 0) {
                                gDeviceConfigList.add(setConfig(LibDeviceId.EVENT_TYPE_RING_LEFT_HAND, address))
                            } else if (count == 1) {
                                gDeviceConfigList.add(setConfig(LibDeviceId.EVENT_TYPE_RING_RIGHT_HAND, address))
                            }
                            count++
                        } else {  //Foot Ring
                            if(count2 == 0) {
                                gDeviceConfigList.add(setConfig(LibDeviceId.EVENT_TYPE_RING_RIGHT_FOOT, address))
                            } else if(count2 == 1){
                                gDeviceConfigList.add(setConfig(LibDeviceId.EVENT_TYPE_RING_LEFT_FOOT, address))
                            }
                            count2++
                        }
                    }
                    //gLeMgr.setConnectLeDeviceConfig(gDeviceConfigList)
                    //gLeMgr.connectLeDevice(gCnctList)

                    val pref = getSharedPreferences("BLEdata", Context.MODE_PRIVATE)
                    val editor = pref.edit()
                    if(pref != null) {
                        editor.clear().apply()
                    }

                    for(config in gDeviceConfigList) {
                        editor.putString(config.no.toString(), config.address)
                        gAdptCnctList?.add(config.no, config.address)
                    }
                    editor.apply()

                })
                .setOnDismissListener {
                    gLeMgr.scanLeDevice(LibBleManager.COMMAND.STOP)
                }
                .create()

        //- check BLE permission
        //-- if have not permission, request it.
//
        if (PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA) ||
                PackageManager.PERMISSION_GRANTED != PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_COARSE_LOCATION),
                    kRequestCodeForAccessCoarseLocation)
        } else {
            gLeMgr.createByActivity(this)
        }

        //- 2018.10.17
        //- using to test write command
        LMainAct_btn1.setOnClickListener {
            gLeMgr?.also {
                if (1 < gCmdCnt)
                    gCmdCnt = 0
                //-- (LeHint)
                //-- if you connected multi-device, please handle your device connected number
                //-- if not, just use "0"
                //it.sendCommandToLeDevice(0, gCmdCnt)
                gCmdCnt++
                Log.d(kTag, "tap")
            }
        }

        val pref = getSharedPreferences("BLEdata", Context.MODE_PRIVATE)
        for(i in 0 .. 6) {
            val data = pref.getString(i.toString(), null)
            if(data == null) continue
            else gAdptCnctList?.add(i, data)
        }
    } // lifecycle/onCreate()

    override fun onPause() {
        super.onPause()
        Log.i(kTag, "onPause")

        gLeMgr.destroyByActivity(this)
        Log.i(kTag, "destroyByActivity")
    }

    override fun onStop() {
        super.onStop()
        Log.i(kTag, "onStop")
        android.os.Process.killProcess(android.os.Process.myPid())
        Log.i(kTag, "killProcess")
    }


    override fun onDestroy() {
        //gLeMgr.destroyByActivity(this)
        super.onDestroy()
    } // lifecycle/onDestroy()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            kRequestCodeForAccessCoarseLocation -> {
                gLeMgr.createByActivity(this)
                Log.i(kTag, "get permission kRequestCodeForAccessCoarseLocation")
            }
            kRequestCodeForAccessWriteExternalStorageStats->{
                // println(getFileContent("/sdcard/xrspace/depth/123.txt"))
                Log.i(kTag, "get permission kRequestCodeForAccessWriteExternalStorageStats")
            }
            kRequestCodeForAccessReadExternalStorageStats->{
                //println(getFileContent("/sdcard/xrspace/depth/123.txt"))
                Log.i(kTag, "get permission kRequestCodeForAccessReadExternalStorageStats")
            }
            else -> Log.i(kTag, "request permission failed")
        }
    } // lifecycle/onRequestPermissionsResult()
    //<< lifecycle ---------------------------------------------------------------------------------
    private fun motionGestureInit() {
        gIIIConfig.fireThreshold = 0.7f
        gIIIConfig.maxFireThreshold = 0.8f
        gIIIConfig.durationThreshold = 10
        gIIIConfig.algorithmName = "Default"
        gIIIConfig.motion_mode = 1

        try {
            gIIIConfig.protoxt = copyAssetsFile(this, "test.proto", "test.proto")
            gIIIConfig.caffemodel = copyAssetsFile(this, "SWTS.model", "SWTS.model")
            gIIIConfig.trainingconfig = copyAssetsFile(this, "training.config", "training.config")
            gIIIConfig.gestureType = copyAssetsFile(this, "gesture.type", "gesture.type")
            Log.d(kTag, "Paul " + "protoxt " + gIIIConfig.protoxt)
            Log.d(kTag, "Paul " + "caffemodel " + gIIIConfig.caffemodel)
            Log.d(kTag, "Paul " + "trainingconfig " + gIIIConfig.trainingconfig)
            Log.d(kTag, "Paul " + "gestureType " + gIIIConfig.gestureType)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //gLeMgr.register(gIIIConfig)
    }
    //>> function ==================================================================================
    //- implement
    //-- (LeHint) If you using scan function, you can get device information here when we find device around you.
    override fun onLeDeviceScanResult(device: BluetoothDevice) {
        runOnUiThread {
            gAdptScanList?.add(device)
        }
    } // function/implement/LibBleManager.LibBleScanListener/onLeDeviceScanResult()

    //-- (LeHint) When device connected, you can get GATT here.
    override fun onLeDeviceConnected(deviceNo: Int, gatt: BluetoothGatt) {
        runOnUiThread {
            //gAdptCnctList?.add(deviceNo, gatt.get)
        }
    } // function/implement/LibBleManager.LibBleDeviceListener/onLeDeviceConnected()

    //-- (LeHint) When device disconnected, you can get related information here.
    override fun onLeDeviceDisconnected(deviceNo: Int, gatt: BluetoothGatt) {
    } // function/implement/LibBleManager.LibBleDeviceListener/onLeDeviceDisconnected()

    //-- (LeHint) You can get some data from device when data arrived.
    override fun onDataAvailable(packet: LibBlePacket) {
        Log.v(kTag,"onDataAvailable " + packet.no + " " + packet.name + " " + packet.rotation[0] + " " +
                packet.rotation[1] + " " + " " + packet.rotation[2])
        runOnUiThread {
            gAdptCnctList?.update(packet)?.takeIf {
                it < 0
            }?.apply {
                //gAdptCnctList?.add(packet.no)
            }
           // showLog("onDataAvailable:", packet)
        }
    } // function/implement/LibBleManager.LibBleDeviceListener/onDataAvailable()

    override fun onSendCommandResponse(cmd: ByteArray) {
        Log.d(kTag, Arrays.toString(cmd))
    } // function/implement/LibBleManager.LibBleDeviceListener/onSendCommandResponse()


    override fun onEventAvailable(packet: LibBleEvtPacket) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //- private
    fun setConfig(no:Int, address:String):LibBleDeviceConfig {
        var config = LibBleDeviceConfig()
        config.no = no
        config.address = address
        return config
    }

    @Throws(IOException::class)
    private fun copyAssetsFile(c: Context, src: String, dest: String): String {
        val buffer = ByteArray(1024)
        var read: Int
        Log.v(kTag, "the path is " + c.getExternalFilesDir(null))
        val f1 = File(c.getExternalFilesDir(null), src)
        val inFd: InputStream
        val out: OutputStream
        var status:Boolean = true
        if (f1.exists()) {
            f1.delete()
        }

        inFd = c.assets.open(src)
        val outFile = File(c.getExternalFilesDir(null), dest)
        out = FileOutputStream(outFile)

        while (status) {
            read = inFd.read(buffer)
            if (read !=-1)
                out.write(buffer, 0, read)
            else
                status = false
        }
        return outFile.absolutePath
    }


    //- public
    //<< function ----------------------------------------------------------------------------------
} // MainActivity

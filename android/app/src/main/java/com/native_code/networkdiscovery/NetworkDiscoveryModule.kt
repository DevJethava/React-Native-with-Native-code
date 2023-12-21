package com.native_code.networkdiscovery

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.Gson
import com.native_code.R
import com.native_code.networkdiscovery.Network.HostBean
import com.native_code.networkdiscovery.Network.NetInfo
import com.native_code.networkdiscovery.Utils.Constant
import com.native_code.networkdiscovery.discoverymodule.AbstractDiscovery2
import com.native_code.networkdiscovery.discoverymodule.DefaultDiscovery2
import com.native_code.networkdiscovery.networkactivity.ActivityDiscovery
import com.native_code.networkdiscovery.portauthority.Host
import com.native_code.networkdiscovery.portauthority.MainAsyncResponse
import com.native_code.networkdiscovery.portauthority.ScanHostsAsyncTask
import com.native_code.networkdiscovery.portauthority.Wireless
import com.native_code.networkdiscovery.portauthority.Wireless.NoConnectivityManagerException
import com.native_code.networkdiscovery.portauthority.Wireless.NoWifiManagerException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.SocketException
import java.net.UnknownHostException
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger


class NetworkDiscoveryModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val TAG = "NetworkDiscoveryModule"
    var mDiscoveryTask: AbstractDiscovery2? = null
    private var currentNetwork = 0
    private var network_ip: Long = 0
    private var network_start: Long = 0
    private var network_end: Long = 0

    private var hosts: ArrayList<HostBean> = ArrayList()

    private var prefs: SharedPreferences? = null

    private val TIMER_INTERVAL = 1500

    private var scanHandler: Handler? = null
    private var mHosts = Collections.synchronizedList(ArrayList<Host>())

    override fun getName() = "NetworkDiscoveryModule";

    @ReactMethod
    fun navigateToNetworkDiscoveryActivity() {
        val activity = currentActivity
        if (activity != null) {
            val intent = Intent(activity, ActivityDiscovery::class.java)
            activity.startActivity(intent)
        }
    }

    @ReactMethod
    fun getNetworkDiscovery(promise: Promise) {
        hosts.clear()
        var progressInt = 0
        CoroutineScope(Dispatchers.Main).launch {
            currentActivity?.let { mActivity ->
                val net = NetInfo(mActivity)
                prefs = PreferenceManager.getDefaultSharedPreferences(mActivity)

                if (currentNetwork != net.hashCode()) {
                    Log.i(TAG, "Network info has changed")
                    currentNetwork = net.hashCode()

                    // Cancel running tasks
                    if (mDiscoveryTask != null) {
                        mDiscoveryTask!!.cancel(true)
                        mDiscoveryTask = null
                    }
                }

                // Get ip information
                network_ip = NetInfo.getUnsignedLongFromIp(net.ip)
                if (prefs?.getBoolean(Constant.KEY_IP_CUSTOM, Constant.DEFAULT_IP_CUSTOM) == true) {
                    // Custom IP
                    network_start = NetInfo.getUnsignedLongFromIp(
                        prefs?.getString(
                            Constant.KEY_IP_START,
                            Constant.DEFAULT_IP_START
                        )
                    )
                    network_end = NetInfo.getUnsignedLongFromIp(
                        prefs?.getString(
                            Constant.KEY_IP_END,
                            Constant.DEFAULT_IP_END
                        )
                    )
                } else {
                    // Custom CIDR
                    if (prefs?.getBoolean(
                            Constant.KEY_CIDR_CUSTOM,
                            Constant.DEFAULT_CIDR_CUSTOM
                        ) == true
                    ) {
                        net.cidr =
                            prefs?.getString(Constant.KEY_CIDR, Constant.DEFAULT_CIDR)?.toInt()!!
                    }
                    // Detected IP
                    val shift: Int = 32 - net.cidr
                    if (net.cidr < 31) {
                        network_start = (network_ip shr shift shl shift) + 1
                        network_end = (network_start or ((1 shl shift) - 1).toLong()) - 1
                    } else {
                        network_start = network_ip shr shift shl shift
                        network_end = network_start or ((1 shl shift) - 1).toLong()
                    }
                }


                mDiscoveryTask =
                    DefaultDiscovery2(
                        mActivity,
                        object :
                            DefaultDiscovery2.Progress {
                            override fun onHostBeanUpdate(bean: HostBean?) {
                                bean?.let {
                                    Log.e(TAG, it.toString())
                                    hosts.add(it)
                                    reactContext
                                        .getJSModule(
                                            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                                        )
                                        .emit("onHostBeanUpdate", Gson().toJson(it))
//                            val networkDiscoveryData =
//                                NetworkDiscoveryData(hosts, progressInt, false)
//                            try {
//                                promise.resolve(Gson().toJson(networkDiscoveryData))
//                            } catch (e: Throwable) {
//                                promise.reject("Error: onHostBeanUpdate", e)
//                            }
                                }
                            }

                            @SuppressLint("LongLogTag")
                            override fun onProgressUpdate(progress: Int?) {
                                progress?.let {
//                            Log.e("$TAG Pro", it.toString())
                                    progressInt = it
                                    val jsonObject = JSONObject()
                                    jsonObject.put("progress", (it / 100).toInt())
                                    reactContext
                                        .getJSModule(
                                            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                                        )
                                        .emit("onProgressUpdate", jsonObject.toString())
                                    val networkDiscoveryData = NetworkDiscoveryData(
                                        hosts = hosts,
                                        progressInt = progressInt,
                                        isCompleted = false
                                    )
//                            try {
//                                promise.resolve(Gson().toJson(networkDiscoveryData))
//                            } catch (e: Throwable) {
//                                promise.reject("Error: onProgressUpdate", e)
//                            }
                                }
                            }

                            override fun onCancel() {
                                Log.e(TAG, "onCancel")
                                val jsonObject = JSONObject()
                                jsonObject.put("isCancel", true)
                                reactContext
                                    .getJSModule(
                                        DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                                    )
                                    .emit("onCancel", jsonObject.toString())
//                        try {
//                            promise.resolve("On Cancel Success !")
//                        } catch (e: Throwable) {
//                            promise.reject("Error: onProgressUpdate", e)
//                        }
                            }

                            @SuppressLint("StaticFieldLeak")
                            override fun onPostExecute() {
                                Log.e(TAG, "onPostExecute")
                                val networkDiscoveryData = NetworkDiscoveryData(
                                    hosts = hosts,
                                    progressInt = progressInt,
                                    isCompleted = true
                                )
//                                try {
//                                    promise.resolve(Gson().toJson(networkDiscoveryData))
//                                } catch (e: Throwable) {
//                                    promise.reject("Error: onPostExecute", e)
//                                }
                                reactContext
                                    .getJSModule(
                                        DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                                    )
                                    .emit("onExecuteComplete", Gson().toJson(networkDiscoveryData))
                            }

                        })
                mDiscoveryTask?.setNetwork(network_ip, network_start, network_end)
//                mDiscoveryTask?.progress =
                mDiscoveryTask?.execute()
            }
        }
    }

    @ReactMethod
    fun cancelNetworkDiscovery() {
        mDiscoveryTask?.cancel(true)
        mDiscoveryTask = null
    }

    private data class NetworkDiscoveryData(
        var hosts: ArrayList<HostBean>,
        var progressInt: Int,
        var isCompleted: Boolean
    )

    @SuppressLint("LongLogTag")
    @ReactMethod
    fun getNetworkDiscovery2() {
        Log.e(
            "getNetworkDiscovery2 => ",
            "Called"
        )
        mHosts.clear()
        scanHandler = Handler(Looper.getMainLooper())
        val wifi = Wireless(currentActivity)
        val info: NetworkInfo? =
            currentActivity?.intent?.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)


        if (info != null) {
            getNetworkInfo(info, wifi)
        }

        val resources: Resources? = currentActivity?.resources
        try {
            if (!wifi.isEnabled) {
                Log.e(
                    "getNetworkDiscovery2 => ",
                    resources?.getString(R.string.wifiDisabled).toString()
                )
//                return
            }
            if (!wifi.isConnectedWifi) {
                Log.e(
                    "getNetworkDiscovery2 => ",
                    resources?.getString(R.string.notConnectedWifi).toString()
                )
//                return
            }
        } catch (e: NoWifiManagerException) {
            Log.e(
                "getNetworkDiscovery2 => ",
                resources?.getString(R.string.failedWifiManager).toString()
            )
//            return
        } catch (e: NoConnectivityManagerException) {
            Log.e(
                "getNetworkDiscovery2 => ",
                resources?.getString(R.string.failedWifiManager).toString()
            )
//            return
        }

        val numSubnetHosts: Int = try {
            wifi.numberOfHostsInWifiSubnet
        } catch (e: NoWifiManagerException) {
            Log.e(
                "getNetworkDiscovery2 => ",
                resources?.getString(R.string.failedSubnetHosts).toString()
            )
//            return
        }

        Log.e("numSubnetHosts", numSubnetHosts.toString())

        try {
            var progressInt = 0;
            val ip = wifi.getInternalWifiIpAddress(wifi.type)
            ScanHostsAsyncTask(currentActivity?.applicationContext, object : MainAsyncResponse {

                /**
                 * Delegate to update the host list and dismiss the progress dialog
                 * Gets called when host discovery has finished
                 *
                 * @param hostEntry The host to add to the list of discovered hosts
                 * @param numberOfHosts Number of hosts
                 */
                override fun processFinish(hostEntry: Host?, numberOfHosts: AtomicInteger?) {
                    scanHandler?.post {
                        if (hostEntry != null) {
                            mHosts.add(hostEntry)
                            reactContext
                                .getJSModule(
                                    DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                                )
                                .emit("onNetworkHostUpdate", Gson().toJson(hostEntry))
                            Log.e("Hosts => ", Gson().toJson(mHosts))
                        }
                        Log.e("Host Count => ", " (" + mHosts.size + ")")
                    }
                }

                /**
                 * Delegate to update the progress of the host discovery scan
                 *
                 * @param progress The amount of progress to increment by
                 */
                override fun processFinish(progress: Int) {
                    progressInt += progress
                    Log.e("Progress => ", progressInt.toString())
                    val jsonObject = JSONObject()
                    jsonObject.put("progressCount", progressInt)
//                    jsonObject.put("progressTill", numSubnetHosts)
                    jsonObject.put("isFinished", false)
                    reactContext
                        .getJSModule(
                            DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                        )
                        .emit("onNetworkProgress", jsonObject.toString())
                }

                /**
                 * Delegate to handle setting the external IP in the UI
                 *
                 * @param externalIp External IP
                 */
                override fun processFinish(externalIp: String?) {
                    Log.e("externalIp => ", externalIp.toString())
                }

                /**
                 * Delegate to dismiss the progress dialog
                 *
                 * @param isFinished
                 */
                override fun processFinish(isFinished: Boolean) {
                    if (isFinished) {
                        Log.e("Hosts => ", Gson().toJson(mHosts))
                        val jsonObject = JSONObject()
                        jsonObject.put("progressCount", -1)
//                    jsonObject.put("progressTill", numSubnetHosts)
                        jsonObject.put("isFinished", true)
                        reactContext
                            .getJSModule(
                                DeviceEventManagerModule.RCTDeviceEventEmitter::class.java
                            )
                            .emit("onNetworkProgress", jsonObject.toString())

                        val vibrator =
                            currentActivity?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    Constant.VIBRATE,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            vibrator.vibrate(200)
                        }
                    }
                }

                /**
                 * Delegate to handle bubbled up errors
                 *
                 * @param output The exception we want to handle
                 * @param <T>    Exception
                 */
                override fun <T : Throwable?> processFinish(output: T) {
                    Log.e("Exception => ", output?.localizedMessage.toString())
                }

            }).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                ip,
                wifi.internalWifiSubnet,
                Constant.DEFAULT_HOST_SOCKET_TIMEOUT.toInt()
            )
        } catch (e: UnknownHostException) {
            Log.e(
                "getNetworkDiscovery2 => ",
                resources?.getString(R.string.notConnectedWifi).toString()
            )
        } catch (e: NoWifiManagerException) {
            Log.e(
                "getNetworkDiscovery2 => ",
                resources?.getString(R.string.notConnectedWifi).toString()
            )
        }
    }

    /**
     * Gets network information about the device and updates various UI elements
     */
    private fun getNetworkInfo(info: NetworkInfo, wifi: Wireless) {
        setupMac(wifi)

        val resources: Resources? = currentActivity?.resources
        try {
            val enabled: Boolean = wifi.isEnabled
            if (!info.isConnected || !enabled) {
                Log.e("getNetworkInfo => ", Wireless.getInternalMobileIpAddress())
            }
            if (!enabled) {
                Log.e("getNetworkInfo => ", resources?.getString(R.string.wifiDisabled).toString())
//                return
            }
        } catch (e: NoWifiManagerException) {
            Log.e("getNetworkInfo => ", resources?.getString(R.string.failedWifiManager).toString())
        }
        if (!info.isConnected) {
            Log.e("getNetworkInfo => ", resources?.getString(R.string.noWifiConnection).toString())
//            return
        }
        val signalHandler = Handler()
        signalHandler.postDelayed(object : Runnable {
            override fun run() {
                val speed: Int = try {
                    wifi.linkSpeed
                } catch (e: NoWifiManagerException) {
                    Log.e(
                        "getNetworkInfo => ",
                        resources?.getString(R.string.failedLinkSpeed).toString()
                    )
//                    return
                }
                val signal: Int = try {
                    wifi.signalStrength
                } catch (e: NoWifiManagerException) {
                    Log.e(
                        "getNetworkInfo => ",
                        resources?.getString(R.string.failedSignal).toString()
                    )
//                    return
                }

                Log.e(
                    "getNetworkInfo => ", String.format(
                        resources?.getString(R.string.signalLink).toString(),
                        signal,
                        speed
                    )
                )
                signalHandler.postDelayed(this, TIMER_INTERVAL.toLong())
            }
        }, 0)
        val wifiSsid: String = try {
            wifi.ssid
        } catch (e: NoWifiManagerException) {
            Log.e(
                "getNetworkInfo => ",
                resources?.getString(R.string.failedSsid).toString()
            )
            "return"
        }
        val wifiBssid: String = try {
            wifi.bssid
        } catch (e: NoWifiManagerException) {
            Log.e(
                "getNetworkInfo => ",
                resources?.getString(R.string.failedBssid).toString()
            )
            "return"
        }

        Log.e("ssid", wifiSsid)
        Log.e("bssid", wifiBssid)
    }

    /**
     * Sets up the device's MAC address and vendor
     */
    private fun setupMac(wifi: Wireless) {
        var macAddress = ""
        try {
            if (!wifi.isEnabled) {
                macAddress = currentActivity?.getString(R.string.wifiDisabled).toString()
                return
            }
            val mac: String = wifi.macAddress
            macAddress = mac
        } catch (e: UnknownHostException) {
            macAddress = currentActivity?.getString(R.string.noWifiConnection).toString()
        } catch (e: SocketException) {
            macAddress = currentActivity?.getString(R.string.noWifiConnection).toString()
        } catch (e: NoWifiManagerException) {
            macAddress = currentActivity?.getString(R.string.noWifiConnection).toString()
        } catch (e: Wireless.NoWifiInterface) {
            macAddress = currentActivity?.getString(R.string.noWifiInterface).toString()
        } finally {
            Log.e("setupMac => ", macAddress)
        }
    }
}
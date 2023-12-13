package com.native_code.networkdiscovery

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.gson.Gson
import com.native_code.networkdiscovery.Network.HostBean
import com.native_code.networkdiscovery.Network.NetInfo
import com.native_code.networkdiscovery.Utils.Constant
import com.native_code.networkdiscovery.discoverymodule.AbstractDiscovery2
import com.native_code.networkdiscovery.discoverymodule.DefaultDiscovery2
import com.native_code.networkdiscovery.networkactivity.ActivityDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject


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
}
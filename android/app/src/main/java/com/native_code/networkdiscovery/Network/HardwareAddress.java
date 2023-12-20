/*
 * Copyright (C) 2009-2010 Aubort Jean-Baptiste (Rorist)
 * Licensed under GNU's GPL 2, see README
 */

// http://standards.ieee.org/regauth/oui/oui.txt

package com.native_code.networkdiscovery.Network;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.util.Log;

import com.google.gson.Gson;
import com.native_code.networkdiscovery.Network.NetInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HardwareAddress {

    private final static String TAG = "HardwareAddress";
    //    private final static String REQ = "select vendor from oui where mac=?";
    // 0x1 is HW Type:  Ethernet (10Mb) [JBP]
    // 0x2 is ARP Flag: completed entry (ha valid)
    private final static String MAC_RE = "^%s\\s+0x1\\s+0x2\\s+([:0-9a-fA-F]+)\\s+\\*\\s+\\w+$";
    private final static int BUF = 8 * 1024;
    private WeakReference<Activity> mActivity;

    public HardwareAddress(Activity activity) {
    }

    public static String getHardwareAddress(String ip) {
        String hw = NetInfo.NOMAC;
        BufferedReader bufferedReader = null;
        try {
            if (ip != null) {
                String ptrn = String.format(MAC_RE, ip.replace(".", "\\."));
                Pattern pattern = Pattern.compile(ptrn);
                bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"), BUF);
                String line;
                Matcher matcher;
                while ((line = bufferedReader.readLine()) != null) {
                    matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hw = matcher.group(1);
                        break;
                    }
                }
            } else {
                Log.e(TAG, "ip is null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't open/read file ARP: " + e.getMessage());
            return hw;
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return hw;
    }

    private static String getHardwareAddress10(String ip) {
        String hw = NetInfo.NOMAC;
        BufferedReader bufferedReader = null;
        try {
            if (ip != null) {
                String ptrn = String.format(MAC_RE, ip.replace(".", "\\."));
                Pattern pattern = Pattern.compile(ptrn);
                Runtime runtime = Runtime.getRuntime();
                Process proc = runtime.exec("ip neigh show");
                proc.waitFor();

                bufferedReader = new BufferedReader(new InputStreamReader(proc.getInputStream()), BUF);
                String line;
                Matcher matcher;
                while ((line = bufferedReader.readLine()) != null) {
                    Log.e("getHardwareAddress10", line);
                    matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        hw = matcher.group(1);
                        break;
                    }
                }
            } else {
                Log.e(TAG, "ip is null");
            }
        } catch (IOException e) {
            Log.e(TAG, "Can't open/read file ARP: " + e.getMessage());
            return hw;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
                Log.e("getHardwareAddress10", e.getMessage());
            }
        }
        return hw;
    }

    public static String Lookup(String ip) {
        Log.e("Lookup => Called", ip + "");
        String hw = NetInfo.NOMAC;
        BufferedReader reader = null;
        try {
            if (ip != null) {
                String ptrn = String.format(MAC_RE, ip.replace(".", "\\."));
                Pattern pattern = Pattern.compile(ptrn);

                Runtime runtime = Runtime.getRuntime();
                Process proc = runtime.exec("ip neigh show");
                proc.waitFor();
//            InputStreamReader inputStreamReader = new InputStreamReader(proc.getInputStream());
                reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Log.e("Lookup => reader", reader.lines().collect(Collectors.joining()));
                }
                String line;
                Matcher matcher;
                while ((line = reader.readLine()) != null) {
                    String[] clientInfo = line.split(" +");
                    Log.e("Lookup => Line : ", Arrays.toString(clientInfo));
                    if (!clientInfo[3].equalsIgnoreCase("type")) {
                        String mac = clientInfo[3];
                        String ipAddress = clientInfo[0];
                        //textView.append("\n\nip: " + ip + " Mac: " + mac);
                        Log.e("Lookup => OG IP : ", ip);
                        Log.e("Lookup => IP : ", ipAddress);
                        Log.e("Lookup => Mac : ", mac);

                        matcher = pattern.matcher(line);
                        if (matcher.matches()) {
                            hw = matcher.group(1);
                            break;
                        }
                    }
                }
            } else {
                Log.e("Lookup", "ip is null");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return hw;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return hw;
    }

//    public static String getNicVendor(String hw) throws SQLiteDatabaseCorruptException {
//        String ni = null;
//        try {
//            SQLiteDatabase db = SQLiteDatabase.openDatabase(Db.PATH + Db.DB_NIC, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
//            if (db != null) {
//                // Db request
//                if (db.isOpen()) {
//                    Cursor c = db.rawQuery(REQ, new String[]{hw.replace(":", "")
//                            .substring(0, 6).toUpperCase()});
//                    if (c.moveToFirst()) {
//                        ni = c.getString(0);
//                    }
//                    c.close();
//                }
//                db.close();
//            }
//        } catch (IllegalStateException e) {
//            Log.e(TAG, e.getMessage());
//        } catch (SQLiteException e) {
//            Log.e(TAG, e.getMessage());
//            // FIXME: Reset db
//            //Context ctxt = d.getApplicationContext();
//            //Editor edit = PreferenceManager.getDefaultSharedPreferences(ctxt).edit();
//            //edit.putInt(Prefs.KEY_RESET_NICDB, 1);
//            //edit.commit();
//        }
//        return ni;
//    }
}

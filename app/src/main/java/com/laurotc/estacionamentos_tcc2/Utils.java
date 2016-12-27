package com.laurotc.estacionamentos_tcc2;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Created by laurotc on 10/16/16.
 */

public class Utils {

    /*
    * Returns the MAC Address of WiFi for all versions of Android
    * @params context Context from where is running the application
    *
    * (Source from Stackoverflow: http://stackoverflow.com/questions/33103798/how-to-get-wi-fi-mac-address-in-android-marshmallow)
    */
    public static String getMacAddr(Context context) {
        //Checks the Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            //For Android Versions >= 5
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }

                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", b));
                    }

                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            }
            catch (Exception ex) {}
            return "02:00:00:00:00:00";
        }
        else {
            //For Android Versions < 5
            WifiManager wimanager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            String macAddress = wimanager.getConnectionInfo().getMacAddress();
            if (macAddress == null) {
                macAddress = "Device don't have mac address or wi-fi is disabled";
            }
            return macAddress;
        }
    }


    /**
     * Handles incoming intents.
     * @param deviceInfo DeviceInfo with all the information about the device
     * @param action int with the action that is going to be done: update or read
     * @param context Context the execution
     */
    public static String makeJsonDevice(DeviceInfo deviceInfo, int action, Context context) throws JSONException {
        //Put in json all parameters to send to server
        final JSONObject deviceData = new JSONObject();
        deviceData.put("mac_addr", Utils.getMacAddr(context));
        deviceData.put("latitude", deviceInfo.getLatitude());
        deviceData.put("longitude", deviceInfo.getLongitude());
        deviceData.put("status", deviceInfo.getStatus());
        deviceData.put("action", action);
        return deviceData.toString();
    }
}

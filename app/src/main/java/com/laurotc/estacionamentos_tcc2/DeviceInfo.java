package com.laurotc.estacionamentos_tcc2;

import com.google.android.gms.location.DetectedActivity;

/**
 * Created by laurotc on 10/11/16.
 */

public class DeviceInfo {

    private double latitude = 0;
    private double longitude = 0;
    private int status = -1;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

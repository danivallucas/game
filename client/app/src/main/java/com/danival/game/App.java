package com.danival.game;

import android.app.Application;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class App extends Application {

    private Socket mSocket;
    private int tag = 0;

    {
        try {
            //Runtime.getRuntime().exec("logcat -c");
            Runtime.getRuntime().exec("logcat -f" + " /sdcard/Logcat.txt");
        } catch (java.io.IOException e ) {}

        try {
            mSocket = IO.socket(Constants.SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

    public int getTag() { return tag; }

    public void setTag(int _tag) { tag = _tag; }

    public double metersToLat(double lat, double lng, int meters) {
        return (meters / 6378100f) * (180 / Math.PI);
    }

    public double metersToLng(double lat, double lng, int meters) {
        return (meters / 6378100f) * (180 / Math.PI) / Math.cos(lat * Math.PI/180);
    }

}
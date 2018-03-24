package com.danival.game;

import android.app.Application;
import android.util.Log;

import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class App extends Application {

    private Socket mSocket;
    private int tag = 0;

    {
        //initSocket();
    }

    public void initSocket() {
        try {
            IO.Options opts = new IO.Options();
            opts.forceNew = true;
            opts.reconnection = false;
            opts.timeout = 5000;
            mSocket = IO.socket(Constants.SERVER_URL, opts);
            Log.e("game", "App: mSocket = IO.socket(Constants.SERVER_URL, opts);");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        Log.e("game", "App: getSocket()");
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
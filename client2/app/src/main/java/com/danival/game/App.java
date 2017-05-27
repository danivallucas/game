package com.danival.game;

import android.app.Application;
import io.socket.client.IO;
import io.socket.client.Socket;
import java.net.URISyntaxException;

public class App extends Application {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constants.SERVER_URL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}
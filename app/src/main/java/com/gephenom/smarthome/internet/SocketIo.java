package com.gephenom.smarthome.internet;


import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class SocketIo {
    public Socket mSocket;
    public SocketIo(String url){
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {}
    }
}

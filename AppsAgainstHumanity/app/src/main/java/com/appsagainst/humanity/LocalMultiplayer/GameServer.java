package com.appsagainst.humanity.LocalMultiplayer;

import android.util.Log;

import com.appsagainst.humanity.Events.ClientAdded;
import com.appsagainst.humanity.Global;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by User on 09/05/2015.
 */
public class GameServer {
    AsyncHttpServer server;
    ArrayList<WebSocket> clientSockets = new ArrayList<>();

    private static final String TAG = "GameServer";

    private int mPort = -1;

    public GameServer() {
        server = new AsyncHttpServer();
        server.websocket("/", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                clientSockets.add(webSocket);
                Global.getInstance().bus.post(new ClientAdded(request.toString()));

                //Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                Log.e("WebSocket", "Error");
                        } finally {
                            clientSockets.remove(webSocket);
                        }
                    }
                });
            }
        });

        Random r = new Random();
        mPort = r.nextInt(65530);
        server.listen(mPort);
    }

    public void tearDown() {
        server.stop();
    }

    public int getLocalPort() {
        return mPort;
    }

    public void sendMessage(String msg) {
        for (WebSocket socket : clientSockets) {
            socket.send(msg);
        }
    }
}
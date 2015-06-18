package com.appsagainst.humanity.LocalMultiplayer;

import android.util.Log;

import com.appsagainst.humanity.Global;
import com.appsagainst.humanity.POJO.DataObject;
import com.appsagainst.humanity.Protocol.JsonResolver;
import com.google.gson.Gson;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by User on 09/05/2015.
 */
public class GameClient {

    private int PORT;

    private final String TAG = "GameClient";

    WebSocket socket;
    Gson gson;

    public GameClient(InetAddress address, int port) {
        this.PORT = port;
        setupClient("http://" + address.getHostAddress() + ":" + PORT);
    }

    public GameClient(String url, int port) {
        this.PORT = port;
        setupClient(url + ":" + PORT);
    }

    public void setupClient(String url){
        Log.d(TAG, "Creating GameClient");

        this.gson = new Gson();

        AsyncHttpClient.WebSocketConnectCallback mWebSocketConnectCallback = new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }

                socket = webSocket;

                socket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        JsonResolver.resolveObject(gson.fromJson(s, DataObject.class));
                    }
                });
                informPlayersOfName();
            }
        };

        AsyncHttpClient mAsyncHttpClient = AsyncHttpClient.getDefaultInstance();
        mAsyncHttpClient.websocket(url, null, mWebSocketConnectCallback);
    }

    public void informPlayersOfName(){
        Log.d(TAG, "INFORMING PLAYERS OF NAME");

        DataObject obj = new DataObject();
        obj.action = JsonResolver.playerJoinedLobby;
        obj.data.playerName = Global.getInstance().name;
        obj.data.uniqueID = Global.getInstance().uniqueID;
        sendMessage(gson.toJson(obj));
    }

    public void startSession(){
        DataObject obj = new DataObject();
        obj.action = JsonResolver.startGameSession;
        sendMessage(gson.toJson(obj));
    }

    public void selectBlackCardPlayer(String newBlackCardPlayer){
        DataObject obj = new DataObject();
        obj.action = JsonResolver.selectBlackCardPlayer;
        obj.data.blackCardPlayerUniqueID = newBlackCardPlayer;
        sendMessage(gson.toJson(obj));
    }

    public void selectCard(int selectedCard){
        DataObject obj = new DataObject();
        obj.action = JsonResolver.submitWhiteCardToServer;
        obj.data.whiteCardID = selectedCard;
        obj.data.uniqueID = Global.getInstance().uniqueID;
        sendMessage(gson.toJson(obj));
    }

    public void allCardsSubmitted(){
        DataObject obj = new DataObject();
        obj.action = JsonResolver.allCardsSubmitted;
        sendMessage(gson.toJson(obj));
    }

    public void chooseWinner(String uniqueID, int whiteCardID){
        DataObject obj = new DataObject();
        obj.action = JsonResolver.chooseWinnerEvent;
        obj.data.uniqueID = uniqueID;
        obj.data.whiteCardID = whiteCardID;
        sendMessage(gson.toJson(obj));
    }


    public void distributeWhiteCards(HashMap<String, ArrayList<Integer>> cardIDs){
        DataObject obj = new DataObject();
        obj.action = JsonResolver.distributeWhiteStartingCards;
        obj.data.initialCards = cardIDs;
        sendMessage(gson.toJson(obj));
    }


    private WebSocket getSocket() {
        return socket;
    }

    public void tearDown() {
        getSocket().close();
    }

    public void sendMessage(String msg) {
        Log.d(TAG, "SENDING MESSAGE: " + msg);
        getSocket().send(msg);
    }
}

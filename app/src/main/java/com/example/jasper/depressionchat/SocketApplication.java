package com.example.jasper.depressionchat;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.jasper.depressionchat.model.SocketEvent;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;


public class SocketApplication extends Application {

    private Socket socket;
    private RequestQueue requestQueue;
    private Activity currentActivity = null;
    private final String TAG = "Application";
    private final int ACTION_START_CALL = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            socket = IO.socket(Constants.SIGNALING_URI);
        }catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        socket.on(Constants.SOCKET_CALL, onCall);
        socket.on(Socket.EVENT_RECONNECT, onReconnect);
        registerSocketEvent(Constants.SOCKET_ADD_MESSAGE);
        registerSocketEvent(Constants.SOCKET_ADD_USER);
        registerSocketEvent(Constants.SOCKET_CONNECT_ROOM);
        registerSocketEvent(Constants.SOCKET_CALL);
        registerSocketEvent(Constants.SOCKET_ANSWER);
        registerSocketEvent(Constants.SOCKET_CREATE_OFFER);
        registerSocketEvent(Constants.SOCKET_CANDIDATE);
        registerSocketEvent(Constants.SOCKET_OFFER);
        registerSocketEvent(Constants.SOCKET_UPDATE_FRIEND);
        registerSocketEvent(Constants.SOCKET_CHECK_FRIEND_STATUS);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = pref.getInt("userId", Constants.DEFAULT_ID);
        if ( userId != Constants.DEFAULT_ID) {
            socket.connect();
            try {
                socket.emit(Constants.SOCKET_ADD_USER, new JSONObject().put("userId", userId));
            } catch (JSONException e){
                throw new RuntimeException(e);
            }
        }
        requestQueue = Volley.newRequestQueue(getApplicationContext());
    }


    public Socket getSocket() {
        return socket;
    }


    public RequestQueue getRequestQueue() {
        return requestQueue;
    }


    public Activity getCurrentActivity() { return this.currentActivity; }


    public void setCurrentActivity(Activity currentActivity) {this.currentActivity = currentActivity;}


    private void registerSocketEvent(final String eventName) {
        socket.on(eventName, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                Log.i(TAG, "new socket event: " + eventName);
                EventBus.getDefault().post(new SocketEvent(eventName, data));
            }
        });
    }



    // create a call dialog on the running Activity
    // TODO: Notification for app in background?
    private Emitter.Listener onCall = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            if (currentActivity != null) {
                currentActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject data = (JSONObject) args[0];
                        String name;
                        final int roomId;
                        try {
                            name = data.getString("name");
                            roomId = data.getInt("roomId");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        final Dialog dialog = new Dialog(currentActivity);
                        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                        dialog.setCancelable(false);
                        dialog.setContentView(R.layout.dialog_call);

                        TextView text = dialog.findViewById(R.id.caller_text);
                        String sender = name + " calling...";
                        text.setText(sender);

                        FloatingActionButton acceptCallButton = dialog.findViewById(R.id.btn_accept_call);
                        FloatingActionButton declineCallButton = dialog.findViewById(R.id.btn_decline_call);
                        acceptCallButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    socket.emit(Constants.SOCKET_ANSWER_CALL, new JSONObject().put("accept", true)
                                    .put("roomId", roomId));
                                    dialog.dismiss();
                                    Intent intent = new Intent(currentActivity, CallActivity.class);
                                    intent.putExtra("initiator", false);
                                    intent.putExtra("roomId", roomId);
                                    currentActivity.startActivityForResult(intent, ACTION_START_CALL);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        declineCallButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    socket.emit(Constants.SOCKET_ANSWER_CALL, new JSONObject().put("accept", false)
                                            .put("roomId", roomId));
                                    dialog.dismiss();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                        dialog.show();
                    }
                });
            }
        }
    };

    // register user information on signalling server
    private Emitter.Listener onReconnect = new Emitter.Listener() {
        @Override
       public void call(Object... args) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int userId = pref.getInt(Constants.PREF_USER_ID, Constants.DEFAULT_ID);
           if (userId != Constants.DEFAULT_ID) {
                // Update info on signalling server
                try {
                    socket.emit(Constants.SOCKET_ADD_USER, new JSONObject().put("userId", userId));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    };

}

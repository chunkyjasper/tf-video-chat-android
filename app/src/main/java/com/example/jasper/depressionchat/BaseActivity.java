package com.example.jasper.depressionchat;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.example.jasper.depressionchat.model.SocketEvent;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyStandardDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import io.socket.client.Socket;

/**
 * Created by jasper on 26/12/17.
 */

abstract class BaseActivity extends AppCompatActivity {

    protected Socket socket;
    protected RequestQueue requestQueue;
    protected SocketApplication app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        socket = ((SocketApplication) getApplication()).getSocket();
        requestQueue = ((SocketApplication) getApplication()).getRequestQueue();
        app = (SocketApplication) this.getApplicationContext();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        // Redirect to login page if no login information
        if (pref.getInt("userId", Constants.DEFAULT_ID) == Constants.DEFAULT_ID) {
            Intent intent = new Intent(this.getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

    }

    protected void onResume() {
        super.onResume();
        app.setCurrentActivity(this);
    }
    protected void onPause() {
        clearReferences();
        super.onPause();
    }
    protected void onDestroy() {
        clearReferences();
        super.onDestroy();
    }

    private void clearReferences(){
        Activity currActivity = app.getCurrentActivity();
        if (this.equals(currActivity))
            app.setCurrentActivity(null);
    }

}

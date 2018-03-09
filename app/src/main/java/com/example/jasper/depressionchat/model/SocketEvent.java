package com.example.jasper.depressionchat.model;

import org.json.JSONObject;

// Represents a socket.on events passed through event bus to different activities
public class SocketEvent {

    private String eventName;
    private JSONObject data;

    public SocketEvent(String eventName, JSONObject data) {
        this.eventName = eventName;
        this.data = data;
    }
    public String getEventName() {
        return this.eventName;
    }

    public JSONObject getData() {
        return this.data;
    }
}

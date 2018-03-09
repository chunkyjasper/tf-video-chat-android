package com.example.jasper.depressionchat.model;


import org.json.JSONException;
import org.json.JSONObject;

import static com.example.jasper.depressionchat.model.Friend.Status.UNKNOWN;

public class Friend extends User{

    private int chatId;
    private Message mostRecentMessage;
    private Status status;

    public enum Status {
        UNKNOWN,
        ONLINE,
        OFFLINE;
    }

    public int getChatId() {
        return chatId;
    }


    public Message getMostRecentMessage() {
        return mostRecentMessage;
    }

    public void setMostRecentMessage(Message mostRecentMessage) {
        this.mostRecentMessage = mostRecentMessage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }



    public Friend(String name, String email, int id, int chatId) {
        super(name, email, id);
        this.chatId = chatId;
        this.status = UNKNOWN;
    }

    public Friend(JSONObject obj) throws JSONException{
        super(obj.getJSONObject("friend"));
        this.chatId = obj.getInt("friendship_id");
        Message msg;
        if (!obj.getString("most_recent_msg").equals("null")) {
            msg = new Message(obj.getJSONObject("most_recent_msg"));
        } else {
            msg = new Message();
        }
        this.mostRecentMessage = msg;
        this.status = UNKNOWN;
    }
}

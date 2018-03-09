package com.example.jasper.depressionchat.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class Chat {
    private ArrayList<Message> message_list;


    public Chat() {message_list = new ArrayList<>();}

    public Chat parseMessageList(JSONArray jsonArray) {
        try {
            message_list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject message_json = jsonArray.getJSONObject(i);
                message_list.add(new Message(message_json));
            }
        } catch (JSONException je) {
            je.printStackTrace();
        }
        return this;
    }

    public ArrayList<Message> getMessageList() {
        return this.message_list;
    }

    public ArrayList<Message> addMessage(Message msg) {
        message_list.add(msg);
        return message_list;
    }

    public int getChatSize() {
        return message_list.size();
    }

}

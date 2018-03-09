package com.example.jasper.depressionchat.model;

import com.example.jasper.depressionchat.Constants;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Message {

    private int fromUser;
    private int toUser;
    private String text;
    private Date timestamp;
    private boolean isEmpty = false;

    public Message(JSONObject jsonObj) throws JSONException{
        fromUser =jsonObj.getInt("from_user");
        toUser =jsonObj.getInt("to_user");
        text=jsonObj.getString("text");
        String timestamp_str = jsonObj.getString("timestamp");
        SimpleDateFormat format = new SimpleDateFormat(Constants.DATE_FORMAT);
        Date date = new Date();

        try {
            date = format.parse(timestamp_str.substring(0,timestamp_str.length()-4));
        } catch (ParseException e) {
            try {
                date = format.parse(timestamp_str);
            } catch (ParseException e2) {
                e2.printStackTrace();
            }
        }
        timestamp = date;
    }

    public Message(int fromUser, int toUser, String text, Date timestamp){
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.text = text;
        this.timestamp = timestamp;
    }

    public Message() {
        this.isEmpty = true;
    }

    public boolean isEmpty() {
        return this.isEmpty;
    }

    public int getFromUser(){
        return this.fromUser;
    }

    public int getToUser() {
        return this.toUser;
    }

    public Date getTimestamp() {
        return this.timestamp;
    }

    public String getText() {
        return this.text;
    }
}

package com.example.jasper.depressionchat.model;

import org.json.JSONException;
import org.json.JSONObject;



public class User {

    private String name;
    private String email;
    private int id;

    // status, message, avatar
    public User(String name, String email, int id) {
        this.name = name;
        this.email = email;
        this.id = id;
    }

    public User(JSONObject obj) throws JSONException{
        this.name = obj.getString("nickname");
        this.email = obj.getString("email");
        this.id = obj.getInt("id");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}
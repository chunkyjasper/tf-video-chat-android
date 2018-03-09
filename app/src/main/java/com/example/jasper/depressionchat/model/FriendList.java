package com.example.jasper.depressionchat.model;

import android.util.SparseIntArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;




public class FriendList {

    public enum Status {
        UNKNOWN,
        ONLINE,
        OFFLINE;
    }

    public FriendList() {
    }

    public FriendList(JSONArray jsonArray) throws JSONException{
        idPosMap = new SparseIntArray();
        friendList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            Friend friend = new Friend(jsonArray.getJSONObject(i));
            idPosMap.put(friend.getId(), i);
            friendList.add(friend);
        }
    }

    private ArrayList<Friend> friendList;

    private SparseIntArray idPosMap;

    public ArrayList<Friend> getFriendList() {
        return friendList;
    }

    public Friend getFriendWithId(int id) {
        int pos = idPosMap.get(id);
        return friendList.get(pos);
    }

    public void updateFriendWithId(int id, Friend friend) {
        int pos = idPosMap.get(id);
        Friend mfriend = friendList.get(pos);
        mfriend.setStatus(friend.getStatus());
        mfriend.setMostRecentMessage(friend.getMostRecentMessage());
        mfriend.setName(friend.getName());
        mfriend.setEmail(friend.getEmail());
        if (mfriend.getId() == friend.getId()) {
            throw new RuntimeException("Attempting to update friend with different id");
        }
    }

    public void addFriend(Friend friend) {
        friendList.add(0, friend);
        idPosMap.put(friend.getId(), 0);
    }

    public SparseIntArray getIdPosMap() {
        return idPosMap;
    }

    public int getPosition(int id) {
        return idPosMap.get(id);
    }

    public ArrayList<Integer> getIdList(){
        ArrayList<Integer> idList = new ArrayList<>();
        for (Friend f : friendList){
            idList.add(f.getId());
        }
        return idList;
    }


}

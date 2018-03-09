package com.example.jasper.depressionchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.jasper.depressionchat.model.Friend;
import com.example.jasper.depressionchat.model.FriendList;
import com.example.jasper.depressionchat.model.Message;
import com.example.jasper.depressionchat.model.SocketEvent;
import com.google.gson.Gson;
import com.yarolegovich.lovelydialog.LovelyInfoDialog;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;
import com.yarolegovich.lovelydialog.LovelyTextInputDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.hdodenhof.circleimageview.CircleImageView;
import io.socket.client.Socket;

/***
 * TODO:
 * Search
 * confirm friend
 * show friend pending
 * empty view when no friend
 * DiffUtil for update friendlist?
 * Create a test friend for calling alone?
 * Find way to remove the need of initializing float button here and pass to menu activity
 * cache result
 */
public class FriendsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private RecyclerView recyclerFriendList;
    private FriendListAdapter adapter;
    public FragFriendClickFloatButton onClickFloatButton;
    private FriendList friendList;
    private LovelyProgressDialog dialogFindAllFriend;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int userId;
    private String token;
    private RequestQueue queue;
    private Socket socket;
    public static int ACTION_START_CHAT = 1;
    private SharedPreferences pref;

    public FriendsFragment() {
        onClickFloatButton = new FragFriendClickFloatButton();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
        View layout = inflater.inflate(R.layout.fragment_people, container, false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        queue = ((SocketApplication) (getActivity().getApplication())).getRequestQueue();
        socket = ((SocketApplication) (getActivity().getApplication())).getSocket();
        recyclerFriendList = layout.findViewById(R.id.recycleListFriend);
        recyclerFriendList.setLayoutManager(linearLayoutManager);
        swipeRefreshLayout = layout.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this);
        userId = pref.getInt(Constants.PREF_USER_ID, Constants.DEFAULT_ID);
        token = pref.getString(Constants.PREF_OAUTH_TOKEN, null);
        requestFriend();
        return layout;
    }

    private void requestFriend() {
        dialogFindAllFriend = new LovelyProgressDialog(getContext());
        dialogFindAllFriend.setCancelable(false)
                .setIcon(R.drawable.ic_add_friend)
                .setTitle("Retrieving friends information....")
                .setTopColorRes(R.color.primary)
                .show();
        APIRequest friendRequest = new APIRequest("users/" + userId + "/friends", token,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            friendList = new FriendList(response);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        adapter = new FriendListAdapter(friendList, FriendsFragment.this);
                        recyclerFriendList.setAdapter(adapter);
                        pref.edit().putString(Constants.PREF_FRIENDLIST, new Gson().toJson(friendList)).apply();
                        try {
                            JSONObject data = new JSONObject().put("friend_ids", friendList.getIdList());
                            socket.emit(Constants.SOCKET_SUB_FRIEND, data);
                            socket.emit(Constants.SOCKET_CHECK_FRIEND_STATUS, data);
                        } catch (JSONException e){
                            throw new RuntimeException(e);
                        }
                        swipeRefreshLayout.setRefreshing(false);
                        dialogFindAllFriend.dismiss();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String jsonError = "Cannot read network response";
                if (networkResponse != null && networkResponse.data != null) {
                    jsonError = new String(networkResponse.data);
                }
                Toast.makeText(FriendsFragment.this.getContext(), jsonError, Toast.LENGTH_LONG).show();
                friendList = new FriendList();
                adapter = new FriendListAdapter(friendList, FriendsFragment.this);
                recyclerFriendList.setAdapter(adapter);
                dialogFindAllFriend.dismiss();
            }
        });
        queue.add(friendRequest);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (ACTION_START_CHAT == requestCode && data != null) {
            return;
            // TODO
        }
    }

    @Override
    public void onRefresh() {
        requestFriend();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    // Add friend button
    public class FragFriendClickFloatButton implements View.OnClickListener {
        Context context;
        LovelyProgressDialog dialogWait;

        public FragFriendClickFloatButton() {
        }


        public FragFriendClickFloatButton getInstance(Context context) {
            this.context = context;
            dialogWait = new LovelyProgressDialog(context);
            return this;
        }

        @Override
        public void onClick(final View view) {
            new LovelyTextInputDialog(view.getContext())
                    .setTopColorRes(R.color.primary)
                    .setTitle("Add friend")
                    .setMessage("Enter friend email")
                    .setIcon(R.drawable.ic_add_friend)
                    .setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
                    .setInputFilter("Email not found", new LovelyTextInputDialog.TextFilter() {
                        @Override
                        public boolean check(String text) {
                            Pattern VALID_EMAIL_ADDRESS_REGEX =
                                    Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
                            Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(text);
                            return matcher.find();
                        }
                    })
                    .setConfirmButton(android.R.string.ok, new LovelyTextInputDialog.OnTextInputConfirmListener() {
                        @Override
                        public void onTextInputConfirmed(String text) {
                            // TODO: Add friend logic
                            addFriendService(text);
                        }
                    })
                    .show();
        }

        private void addFriendService(String email) {
            final LovelyProgressDialog dialog = new LovelyProgressDialog(getContext());
            dialog.setCancelable(false)
                    .setIcon(R.drawable.ic_add_friend)
                    .setTitle("Adding friend...")
                    .setTopColorRes(R.color.primary)
                    .show();
            JSONObject postForm = new JSONObject();
            try {
                postForm.put("email", email);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            String url = "users/" + userId + "/friends";
            APIRequest friendRequest = new APIRequest(url, postForm, APIRequest.ContentType.FORM, token,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            try {
                                JSONObject obj = response.getJSONObject(0);
                                Friend friend = new Friend(obj);
                                friendList.addFriend(friend);
                                ArrayList<Integer> friend_ids = new ArrayList<>();
                                friend_ids.add(friend.getId());
                                socket.emit(Constants.SOCKET_CHECK_FRIEND_STATUS,
                                        new JSONObject().put("friend_ids", friend_ids));
                                adapter.notifyItemInserted(0);
                                // TODO: notify friend
                                // TODO: prompt for accept before being friend
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                            dialog.dismiss();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    NetworkResponse networkResponse = error.networkResponse;
                    String jsonError = "Cannot read network response";
                    if (networkResponse != null && networkResponse.data != null) {
                        jsonError = new String(networkResponse.data);
                    }
                    Toast.makeText(FriendsFragment.this.getContext(), jsonError, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });
            queue.add(friendRequest);
        }

    }

    // Socket events
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(SocketEvent event) {
        String eventName = event.getEventName();
        JSONObject obj = event.getData();
        switch (eventName) {
            case Constants.SOCKET_UPDATE_FRIEND:
                onUpdateFriend(obj);
                break;
            case Constants.SOCKET_CHECK_FRIEND_STATUS:
                onCheckFriendStatus(obj);
                break;
        }
    }

    private void onUpdateFriend(JSONObject obj){
        try {
            int id = obj.getInt("id");
            String field = obj.getString("field");
            switch (field) {
                case "status":
                    boolean online = obj.getBoolean("online");
                    if (online) {
                        friendList.getFriendWithId(id).setStatus(Friend.Status.ONLINE);
                    } else {
                        friendList.getFriendWithId(id).setStatus(Friend.Status.OFFLINE);
                    }
                    break;
                case "message":
                    Message msg = new Message(obj.getJSONObject("message"));
                    friendList.getFriendWithId(id).setMostRecentMessage(msg);
                    break;
            }
            adapter.notifyItemChanged(friendList.getPosition(id));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    // Iterate through obj and set friend's online status, notifying adapter
    private void onCheckFriendStatus(JSONObject obj) {

        Iterator<?> keys = obj.keys();
        while(keys.hasNext()) {
            String key = (String)keys.next();
            Friend.Status status;
            try {
                if (obj.getBoolean(key)) {
                    status = Friend.Status.ONLINE;
                } else {
                    status = Friend.Status.OFFLINE;
                }
            } catch (JSONException e) {
                status = Friend.Status.UNKNOWN;
            }
            int id = Integer.parseInt(key);
            friendList.getFriendWithId(id).setStatus(status);
            adapter.notifyItemChanged(friendList.getPosition(id));
        }
    }

    class FriendListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private FriendList friendList;
        private Context context;
        private FriendsFragment fragment;
        private LovelyProgressDialog dialogWaitDeleting;

        public FriendListAdapter(FriendList friendList, FriendsFragment fragment) {
            this.friendList = friendList;
            this.context = FriendsFragment.this.getContext();
            this.fragment = fragment;
            dialogWaitDeleting = new LovelyProgressDialog(context);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.rc_item_friend, parent, false);
            return new FriendViewHolder(context, view);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            FriendViewHolder fvholder = (FriendViewHolder) holder;
            final Friend friend = friendList.getFriendList().get(position);
            final String name = friend.getName();
            final int friendId = friend.getId();
            final int chatId = friend.getChatId();
            final Message msg = friend.getMostRecentMessage();
            GradientDrawable bgShape = (GradientDrawable)fvholder.status.getBackground();
            if (friend.getStatus() == Friend.Status.ONLINE) {
                bgShape.setColor(Color.GREEN);
            } else if (friend.getStatus() == Friend.Status.OFFLINE) {
                bgShape.setColor(Color.RED);
            } else {
                bgShape.setColor(Color.GRAY);
            }

            fvholder.txtName.setText(name);

            if (! msg.isEmpty()) {
                String txt = msg.getText();
                if (msg.getFromUser() != friendId) {
                    txt = "You: " + txt;
                }
                fvholder.txtMessage.setText(txt);
                SimpleDateFormat df = new SimpleDateFormat("MM/dd HH:mm");
                fvholder.txtTime.setText(df.format(msg.getTimestamp()));
            } else {
                String text = "No message yet";
                fvholder.txtMessage.setText(text);
            }

            // On card Click
            ((View) fvholder.txtName.getParent().getParent().getParent())
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Socket socket = ((SocketApplication) fragment.getActivity().getApplicationContext()).getSocket();
                            try {
                                socket.emit(Constants.SOCKET_CONNECT_ROOM, new JSONObject().put("chatId", chatId));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            ((FriendViewHolder) holder).txtMessage.setTypeface(Typeface.DEFAULT);
                            ((FriendViewHolder) holder).txtName.setTypeface(Typeface.DEFAULT);
                            Intent intent = new Intent(context, ChatActivity.class);
                            intent.putExtra(Constants.INTENT_KEY_CHAT_ID, friendId);
                            intent.putExtra(Constants.INTENT_KEY_CHAT_FRIEND, name);
                            intent.putExtra(Constants.INTENT_KEY_CHAT_ROOM_ID, chatId);
                            fragment.startActivityForResult(intent, FriendsFragment.ACTION_START_CHAT);

                        }
                    });
            // TODO: delete friend on long click
            // set avatar
            ((FriendViewHolder) holder).avatar.setImageResource(R.drawable.default_avatar);

            // check if friend is online or not, change ui accordingly
        }

        @Override
        public int getItemCount() {
            return friendList.getFriendList() != null ? friendList.getFriendList().size() : 0;
        }


        private void deleteFriend(final String idFriend) {
            if (idFriend != null) {
                // TODO: delete friend
            } else {
                dialogWaitDeleting.dismiss();
                new LovelyInfoDialog(context)
                        .setTopColorRes(R.color.primary)
                        .setTitle("Error")
                        .setMessage("Error occurred during deleting friend")
                        .show();
            }
        }

        class FriendViewHolder extends RecyclerView.ViewHolder {
            public CircleImageView avatar;
            public ImageView status;
            public TextView txtName, txtTime, txtMessage;
            public Context context;

            FriendViewHolder(Context context, View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.icon_avatar);
                txtName = itemView.findViewById(R.id.txtName);
                txtTime = itemView.findViewById(R.id.txtTime);
                txtMessage = itemView.findViewById(R.id.txtMessage);
                status = itemView.findViewById(R.id.icon_status);
                this.context = context;
            }
        }


    }
}







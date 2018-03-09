package com.example.jasper.depressionchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.jasper.depressionchat.model.Chat;
import com.example.jasper.depressionchat.model.Message;
import com.example.jasper.depressionchat.model.SocketEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import de.hdodenhof.circleimageview.CircleImageView;

/***
 * TODO:
 * chat box formatting
 * Improve load time
 * Disable call if not online
 * Read/not read
 * typing
 */
public class ChatActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView recyclerChat;
    public static final int VIEW_TYPE_USER_MESSAGE = 0;
    public static final int VIEW_TYPE_FRIEND_MESSAGE = 1;
    private ListMessageAdapter adapter;
    private int friendId;
    private int userId;
    private int roomId;
    private String userNickname;
    private String token;
    private Chat chat;
    private String friendName;
    private ImageButton btnSend;
    private EditText editWriteMessage;
    private LinearLayoutManager linearLayoutManager;
    private final String TAG = "ChatActivity";
    private final int ACTION_START_CALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_chat);
        Toolbar myToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(myToolbar);
        Intent intentData = getIntent();
        friendId = intentData.getIntExtra(Constants.INTENT_KEY_CHAT_ID,Constants.DEFAULT_ID);
        friendName = intentData.getStringExtra(Constants.INTENT_KEY_CHAT_FRIEND);
        roomId = intentData.getIntExtra(Constants.INTENT_KEY_CHAT_ROOM_ID, Constants.DEFAULT_ID);
        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
        chat = new Chat();
        editWriteMessage = (EditText) findViewById(R.id.editWriteMessage);
        if (friendId != Constants.DEFAULT_ID && friendName != null) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(friendName);
            }
            linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
            recyclerChat = (RecyclerView) findViewById(R.id.recyclerChat);
            recyclerChat.setLayoutManager(linearLayoutManager);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            userId = pref.getInt(Constants.PREF_USER_ID, Constants.DEFAULT_ID);
            token = pref.getString(Constants.PREF_OAUTH_TOKEN, null);
            userNickname = pref.getString(Constants.PREF_USER_NAME, null);
            chatMessageService();

        }
    }

    private void chatMessageService() {
        String url = "users/" + userId + "/" + friendId + "/messages";
        APIRequest msgRequest = new APIRequest(url, token,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        chat = new Chat().parseMessageList(response);
                        adapter = new ListMessageAdapter(getApplicationContext(), chat, userId, friendId, friendName);
                        recyclerChat.setAdapter(adapter);
                        recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                String jsonError = "Cannot read network response";
                if (networkResponse != null && networkResponse.data != null) {
                    jsonError = new String(networkResponse.data);
                }
                Toast.makeText(getApplicationContext(), jsonError, Toast.LENGTH_LONG).show();
            }
        });
        requestQueue.add(msgRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_call:
                initiateCall();
        }
        return true;
    }

    private void initiateCall(){
        try {
            socket.emit(Constants.SOCKET_CALL,
                    new JSONObject().put("friendId", friendId).put("roomId", roomId).put("name", "test"));
            Intent intent = new Intent(this, CallActivity.class);
            intent.putExtra("initiator", true);
            startActivityForResult(intent, ACTION_START_CALL);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onBackPressed() {
        Intent result = new Intent();
        result.putExtra("idFriend", friendId);
        setResult(RESULT_OK, result);
        this.finish();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnSend) {
            String content = editWriteMessage.getText().toString().trim();
            if (content.length() > 0) {
                editWriteMessage.setText("");
                // TODO: do it through message model
                JSONObject postBody = new JSONObject();
                try {
                    postBody.put("from_user", userId);
                    postBody.put("to_user", friendId);
                    postBody.put("text", content);
                    SimpleDateFormat df = new SimpleDateFormat(Constants.DATE_FORMAT);
                    postBody.put("timestamp", df.format(Calendar.getInstance().getTime()));
                } catch (JSONException je) {
                    je.printStackTrace();
                }
                try {
                    socket.emit(Constants.SOCKET_ADD_MESSAGE, new JSONObject().put("roomId", roomId)
                    .put("message", postBody).put("friendId", friendId));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String url = "users/" + userId + "/" + friendId + "/messages";
                APIRequest msgRequest = new APIRequest(url, postBody, APIRequest.ContentType.FORM, token,
                        new Response.Listener<JSONArray>() {
                            @Override
                            public void onResponse(JSONArray response) {
                                JSONObject data = new JSONObject();
                                try {
                                    data.put("roomId", roomId);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        NetworkResponse networkResponse = error.networkResponse;
                        String jsonError = "Cannot read network response";
                        if (networkResponse != null && networkResponse.data != null) {
                            jsonError = new String(networkResponse.data);
                        }
                        Toast.makeText(getApplicationContext(), jsonError, Toast.LENGTH_LONG).show();
                    }
                });
                requestQueue.add(msgRequest);



            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(SocketEvent event) {
        String eventName = event.getEventName();
        switch (eventName) {
            case Constants.SOCKET_ADD_MESSAGE:
                onAddMessage(event);
                break;
        }
    }

    private void onAddMessage(SocketEvent event) {
        JSONObject data = event.getData();
        Message msg;
        try {
            msg = new Message(data);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        //   removeTyping(username);
        addMessage(msg);
    }

    private void addMessage(Message msg) {
        chat.addMessage(msg);
        adapter.notifyItemInserted(chat.getChatSize() - 1);
        scrollToBottom();
    }

    private void scrollToBottom() {
        recyclerChat.scrollToPosition(adapter.getItemCount() - 1);
    }

    class ListMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context context;
        private Chat chat;
        private int userId;
        private int friendId;
        private String friendName;

        public ListMessageAdapter(Context context, Chat chat, int userId, int friendId, String friendName) {
            this.context = context;
            this.chat = chat;
            this.userId = userId;
            this.friendId = friendId;
            this.friendName = friendName;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ChatActivity.VIEW_TYPE_FRIEND_MESSAGE) {
                View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_friend_2, parent, false);
                return new ItemMessageFriendHolder(view);
            } else if (viewType == ChatActivity.VIEW_TYPE_USER_MESSAGE) {
                View view = LayoutInflater.from(context).inflate(R.layout.rc_item_message_user_2, parent, false);
                return new ItemMessageUserHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ItemMessageHolder castedHolder = (ItemMessageHolder) holder;
            Message msg = chat.getMessageList().get(position);
            castedHolder.textContent.setText(msg.getText());
            SimpleDateFormat df = new SimpleDateFormat("MM/dd HH:mm");
            castedHolder.textTime.setText(df.format(msg.getTimestamp()));
            castedHolder.avatar.setImageResource(R.drawable.default_avatar);

            if (castedHolder instanceof ItemMessageFriendHolder) {
                ((ItemMessageFriendHolder) castedHolder).textSender.setText(friendName);
            }

        }

        @Override
        public int getItemViewType(int position) {
            return chat.getMessageList().get(position).getToUser() == userId? ChatActivity.VIEW_TYPE_FRIEND_MESSAGE : ChatActivity.VIEW_TYPE_USER_MESSAGE;
        }

        @Override
        public int getItemCount() {
            return chat.getMessageList().size();
        }

        class ItemMessageUserHolder extends ItemMessageHolder {

            public ItemMessageUserHolder(View itemView) {
                super(itemView);
                textContent = itemView.findViewById(R.id.txtMessage);
                avatar = itemView.findViewById(R.id.imageView2);
                textTime = itemView.findViewById(R.id.txtTime);
            }
        }

        class ItemMessageFriendHolder extends ItemMessageHolder {
            public TextView textSender;
            public ItemMessageFriendHolder(View itemView) {
                super(itemView);
                textSender = itemView.findViewById(R.id.txtSenderName);
                textContent = itemView.findViewById(R.id.txtMessage);
                avatar = itemView.findViewById(R.id.imageView3);
                textTime = itemView.findViewById(R.id.txtTime);
            }
        }

        abstract class ItemMessageHolder extends RecyclerView.ViewHolder{

            public TextView textContent;
            public TextView textTime;
            public CircleImageView avatar;

            public ItemMessageHolder(View itemView) {
                super(itemView);
            }
        }
    }
}




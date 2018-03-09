package com.example.jasper.depressionchat;

public class Constants {

    // Restrict instance
    private Constants(){}

    public static final String  DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    // oauth2
    public static final String OAUTH_ID = "le1FnsdPxhw9r7gHuzLKlUg3uS9E8xkwKLDFwCAo";
    public static final String OAUTH_SECRET = "WOzg7QQErXV02p8rF8e49f1kcgq2CthRrSDc0FemWXKqwrB1JG1MGq0gZt8sN9VJB2xmprV3r2xbNkzoNagnMsdMBkem5tqpapPCIrwYHiG1cgeBaIZQ59f1SiTaMt37";
    public static final String DOMAIN = "http://192.168.1.171:8000/";
    public static final String OAUTH_GRANT_TYPE = "password";

    // Intent reference
    public static final String INTENT_KEY_CHAT_ID = "friendId";
    public static final String INTENT_KEY_CHAT_FRIEND = "friendName";
    public static final String INTENT_KEY_CHAT_AVATAR = "friendAvatar";
    public static final String INTENT_KEY_CHAT_ROOM_ID = "roomId";

    // Preferences
    public static final String PREF_OAUTH_TOKEN = "oauthToken";
    public static final String PREF_USER_ID = "userId";
    public static final String PREF_USER_EMAIL = "userEmail";
    public static final String PREF_USER_NAME = "nickname";
    public static final String PREF_FRIENDLIST = "friendList";
    public static final int DEFAULT_ID = -1;

    // Socket.io
    public static final String SIGNALING_URI = "http://192.168.1.171:3000";
    public static final String SOCKET_ADD_MESSAGE = "add message";
    public static final String SOCKET_CONNECT_ROOM = "connect room";
    public static final String SOCKET_ADD_USER = "add user";
    public static final String SOCKET_SUB_FRIEND = "subscribe friend";
    public static final String SOCKET_CALL = "call";
    public static final String SOCKET_ANSWER_CALL = "answer call";
    public static final String SOCKET_CREATE_OFFER = "create offer";
    public static final String SOCKET_OFFER = "sdp offer";
    public static final String SOCKET_ANSWER = "sdp answer";
    public static final String SOCKET_CANDIDATE = "ice candidate";
    public static final String SOCKET_UPDATE_FRIEND = "update friend";
    public static final String SOCKET_CHECK_FRIEND_STATUS = "check friend status";

    // Call activity
    public static final String CALL_VIDEO_TRACK_ID = "video1";
    public static final String CALL_AUDIO_TRACK_ID = "audio1";
    public static final String CALL_LOCAL_STREAM_ID = "stream1";
    public static final String CALL_SDP_MID = "sdpMid";
    public static final String CALL_SDP_M_LINE_INDEX = "sdpMLineIndex";
    public static final String CALL_SDP = "sdp";
}

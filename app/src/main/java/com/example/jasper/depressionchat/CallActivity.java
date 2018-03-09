package com.example.jasper.depressionchat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telecom.Call;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.jasper.depressionchat.model.SocketEvent;
import com.yarolegovich.lovelydialog.LovelyProgressDialog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.CameraEnumerationAndroid;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


/** TODO:
 *  activity lifecycle, clean up stuff
 *  Only able to call once per application lifecycle (can listen multiple times)
 *  remote stream not truely fullscreen
 *  On connection end, return to previous activity
 *  Display on chat
 *  Notify callee when caller quit the call to shutdown the call dialog
**/
public class CallActivity extends BaseActivity {

    private static final String TAGNAME = "Call Activity";

    private PeerConnectionFactory pcf;
    private VideoSource localVideoSource;
    private PeerConnection peerConnection;
    private MediaStream localMediaStream;
    private VideoRenderer otherPeerRenderer;
    private FloatingActionButton endCallButton;
    private boolean createOffer = false;
    private boolean initiator = false;
    private boolean ended = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);

        Intent intent = getIntent();

        //Full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_call);

        // Assure camera permission is granted
        assertPermission();

        endCallButton = findViewById(R.id.btn_end_call);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true  // Hardware Acceleration Enabled);
        );
        pcf = new PeerConnectionFactory();
        VideoCapturerAndroid vc = VideoCapturerAndroid.create(CameraEnumerationAndroid.getNameOfFrontFacingDevice(), null);

        try {
            localVideoSource = pcf.createVideoSource(vc, new MediaConstraints());
        } catch (Exception ex) {
            Log.e("exception", ex.getMessage());
        }
        VideoTrack localVideoTrack = pcf.createVideoTrack(Constants.CALL_VIDEO_TRACK_ID, localVideoSource);
        localVideoTrack.setEnabled(true);

        AudioSource audioSource = pcf.createAudioSource(new MediaConstraints());
        AudioTrack localAudioTrack = pcf.createAudioTrack(Constants.CALL_AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localMediaStream = pcf.createLocalMediaStream(Constants.CALL_LOCAL_STREAM_ID);
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);

        GLSurfaceView videoView = findViewById(R.id.glview_call);

        VideoRendererGui.setView(videoView, null);
        try {
            otherPeerRenderer = VideoRendererGui.createGui(0, 0, 100, 100, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRenderer renderer = VideoRendererGui.createGui(50, 50, 50, 50, RendererCommon.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        initiator = intent.getBooleanExtra("initiator", false);
        Toast.makeText(getApplicationContext(), "initiator = " + initiator, Toast.LENGTH_LONG).show();
        onConnect();
        if (!initiator) {
            createOffer();
        }
    }

    public void onConnect() {
        if (peerConnection != null) {
            Toast.makeText(this, "peer connection already exist", Toast.LENGTH_LONG).show();
            return;
        }
        // Add Ice servers
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));

        // Create peer connection and add streams
        peerConnection = pcf.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);

        peerConnection.addStream(localMediaStream);
        PeerConnection.SignalingState signalingState = peerConnection.signalingState();
        PeerConnection.IceConnectionState connectionState = peerConnection.iceConnectionState();
        PeerConnection.IceGatheringState iceGatheringState = peerConnection.iceGatheringState();

    }

    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            peerConnection.setLocalDescription(sdpObserver, sessionDescription);
            try {
                JSONObject obj = new JSONObject();
                obj.put(Constants.CALL_SDP, sessionDescription.description);
                if (createOffer) {
                    socket.emit(Constants.SOCKET_OFFER, obj);
                } else {
                    socket.emit(Constants.SOCKET_ANSWER, obj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {
            return;
        }

        @Override
        public void onCreateFailure(String s) {
            return;
        }

        @Override
        public void onSetFailure(String s) {
            return;
        }
    };

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {

        @Override
        public void onSignalingChange(final PeerConnection.SignalingState signalingState) {
            Log.d(TAGNAME, "onSignalingChange:" + signalingState.toString());
            CallActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CallActivity.this, "onSignalingChange:" + signalingState.toString(), Toast.LENGTH_SHORT).show();
                   switch(signalingState){
                       case CLOSED:
                           endCall();
                   }
                }
            });


        }

        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAGNAME, "onIceConnectionChange:" + iceConnectionState.toString());
            CallActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CallActivity.this, "onIceConnectionChange:" + iceConnectionState.toString(), Toast.LENGTH_SHORT).show();
                    switch (iceConnectionState) {
                        case FAILED:
                            endCall();
                            break;
                        case CHECKING:
                        case CONNECTED:
                        case DISCONNECTED:
                            endCall();
                            break;
                        default:
                    }
                }
            });

        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(Constants.CALL_SDP_MID, iceCandidate.sdpMid);
                obj.put(Constants.CALL_SDP_M_LINE_INDEX, iceCandidate.sdpMLineIndex);
                obj.put(Constants.CALL_SDP, iceCandidate.sdp);
                socket.emit(Constants.SOCKET_CANDIDATE, obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            mediaStream.videoTracks.getFirst().addRenderer(otherPeerRenderer);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }
    };

    // Socket events
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSocketEvent(SocketEvent event) {
        String eventName = event.getEventName();
        JSONObject obj = event.getData();
        switch (eventName) {
            case Constants.SOCKET_OFFER:
                onOffer(obj);
                break;

            case Constants.SOCKET_ANSWER:
                onAnswer(obj);
                break;

            case Constants.SOCKET_CANDIDATE:
                onCandidate(obj);
                break;

            case Socket.EVENT_CONNECT_ERROR:
                onConnectError(obj);
                break;

            case Constants.SOCKET_ANSWER_CALL:
                onCallAnswer(obj);
                break;
        }
    }

    // Only called by initiator
    private void onCallAnswer(JSONObject obj) {
        if (!initiator) {
            throw new RuntimeException("Should only be called by initiator");
        }
        try {
            Boolean accept = obj.getBoolean("accept");
            if (!accept) {
                endCall();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void onOffer(JSONObject obj) {
        try {
            PeerConnection.SignalingState signalingState = peerConnection.signalingState();
            PeerConnection.IceConnectionState connectionState = peerConnection.iceConnectionState();
            PeerConnection.IceGatheringState iceGatheringState = peerConnection.iceGatheringState();
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER,
                    obj.getString(Constants.CALL_SDP));
            peerConnection.setRemoteDescription(sdpObserver, sdp);
            peerConnection.createAnswer(sdpObserver, new MediaConstraints());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void onAnswer(JSONObject obj) {
        PeerConnection.SignalingState signalingState = peerConnection.signalingState();
        PeerConnection.IceConnectionState connectionState = peerConnection.iceConnectionState();
        PeerConnection.IceGatheringState iceGatheringState = peerConnection.iceGatheringState();
        try {
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,
                    obj.getString(Constants.CALL_SDP));
            peerConnection.setRemoteDescription(sdpObserver, sdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void createOffer() {
        PeerConnection.SignalingState signalingState = peerConnection.signalingState();
        PeerConnection.IceConnectionState connectionState = peerConnection.iceConnectionState();
        PeerConnection.IceGatheringState iceGatheringState = peerConnection.iceGatheringState();
        createOffer = true;
        peerConnection.createOffer(sdpObserver, new MediaConstraints());
    }


    private void onCandidate(JSONObject obj) {
        try {
            peerConnection.addIceCandidate(new IceCandidate(obj.getString(Constants.CALL_SDP_MID),
                    obj.getInt(Constants.CALL_SDP_M_LINE_INDEX),
                    obj.getString(Constants.CALL_SDP)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onConnectError(JSONObject obj) {
        Log.i(TAGNAME , "error: " + (obj != null ? obj.toString() : ""));
    }

    private void endCall() {
        if (!ended) {
            ended = true;
            peerConnection.close();
            peerConnection.removeStream(localMediaStream);
            localMediaStream.dispose();
            peerConnection.dispose();
            peerConnection = null;
            if (this.localVideoSource != null) {
                this.localVideoSource.stop();
            }
            socket.emit("end call");
            this.finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        endCall();
    }

    private void assertPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    1);
        }

        // Assure internet permission is granted
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.INTERNET},
                    1);
        }
    }

}


package io.comzy.videocore;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;
import android.view.View;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpParameters;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.MediaStreamTrack.MediaType;
import org.webrtc.PeerConnection.BundlePolicy;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.IceTransportsType;
import org.webrtc.PeerConnection.RtcpMuxPolicy;
import org.webrtc.PeerConnection.SdpSemantics;
import org.webrtc.PeerConnection.TcpCandidatePolicy;
import org.webrtc.PeerConnectionFactory.InitializationOptions;
import org.webrtc.SessionDescription.Type;

public class VideoCore {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};
    private static VideoCore instance;
    public final Context context;
    private static final String TAG = "VideoCallActivity";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final int VIDEO_RESOLUTION_WIDTH = 1920;
    private static final int VIDEO_RESOLUTION_HEIGHT = 1080;
    private static final int FPS = 30;
    private String remoteID;
    private String userId;
    private AudioManager audioManager;
    private boolean isInitiator = false;
    private boolean isChannelReady = false;
    private boolean isStarted = false;
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private SurfaceViewRenderer localVideoView;
    private SurfaceViewRenderer remoteVideoView;
    private CameraVideoCapturer videoCapturer;
    private boolean isMuted = false;
    private boolean isFrontCamera = true;
    private WebSocket webSocket;
    private List<RtpSender> senders = new ArrayList();
    private static final String SIGNALING_SERVER_URL = "wss://comzy.io:8443";

    public VideoCore(Context context) {
        this.context = context;
        this.localVideoView = new SurfaceViewRenderer(context);
        this.remoteVideoView = new SurfaceViewRenderer(context);
    }

    public static synchronized VideoCore getInstance(Context context) {
        if (instance == null) {
            instance = new VideoCore(context);
        }

        return instance;
    }

    public static void start(Context context, String userId, String remoteId) {
        VideoCore videoChatClient = getInstance(context);
        if (videoChatClient.checkPermissions(context)) {
            videoChatClient.setup(userId, remoteId);
        } else {
            Log.e("VideoCallActivity", "Permission required");
        }

    }

    private boolean checkPermissions(Context context) {
        for(String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != 0) {
                return false;
            }
        }

        return true;
    }

    private void requestPermissions(Context context, Activity activity) {
        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, 100);
    }

    private void setup(String userId, String remoteId) {
        this.userId = userId;
        this.remoteID = remoteId;
        this.eglBase = EglBase.create();
        this.localVideoView.init(this.eglBase.getEglBaseContext(), (RendererCommon.RendererEvents)null);
        this.localVideoView.setZOrderMediaOverlay(true);
        this.remoteVideoView.init(this.eglBase.getEglBaseContext(), (RendererCommon.RendererEvents)null);
        this.initializePeerConnectionFactory(this.context);
        this.createVideoTrackFromCamera(this.context);
        this.connectToSignallingServer();
    }

    private void connectToSignallingServer() {
        OkHttpClient client = new OkHttpClient();
        Request request = (new Request.Builder()).url("wss://comzy.io:8443").build();
        this.webSocket = client.newWebSocket(request, new WebSocketListener() {
            public void onOpen(WebSocket webSocket, Response response) {
                JSONObject message = new JSONObject();

                try {
                    message.put("type", "join");
                    message.put("userId", VideoCore.this.userId);
                    message.put("remoteId", VideoCore.this.remoteID);
                    webSocket.send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            public void onMessage(WebSocket webSocket, String text) {
                VideoCore.this.handleSignallingData(text);
            }

            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            }

            public void onClosed(WebSocket webSocket, int code, String reason) {
            }
        });
    }

    private void handleSignallingData(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            String type = json.getString("type");
            Log.d("VideoCallActivity", "Signal type: " + type);
            switch (type) {
                case "created":
                    this.isInitiator = true;
                    break;
                case "joined":
                    this.isChannelReady = true;
                    break;
                case "ready":
                    if (this.isInitiator && !this.isStarted) {
                        this.startCall();
                    }
                    break;
                case "message":
                    if (json.has("data")) {
                        JSONObject data = json.getJSONObject("data");
                        switch (data.getString("type")) {
                            case "offer":
                                Log.d("VideoCallActivity", "Received offer");
                                if (!this.isStarted) {
                                    this.startCall();
                                }

                                try {
                                    String sdp = data.getString("sdp");
                                    this.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(Type.OFFER, sdp));
                                    this.createAnswer();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                break;
                            case "answer":
                                if (this.isStarted) {
                                    try {
                                        String sdp = data.getString("sdp");
                                        this.peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(Type.ANSWER, sdp));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                break;
                            case "candidate":
                                if (this.isStarted) {
                                    try {
                                        JSONObject candidate = data.getJSONObject("candidate");
                                        String sdpMid = candidate.getString("sdpMid");
                                        int sdpMLineIndex = candidate.getInt("sdpMLineIndex");
                                        String sdp = candidate.getString("candidate");
                                        IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, sdp);
                                        this.peerConnection.addIceCandidate(iceCandidate);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                        }
                    }
                case "roomInfo":
                default:
                    break;
                case "bye":
                    Log.d("VideoCallActivity", "Remote user left");
            }
        } catch (JSONException e) {
            Log.e("VideoCallActivity", "Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

    }

    private void initializePeerConnectionFactory(Context context) {
        PeerConnectionFactory.InitializationOptions initializationOptions = InitializationOptions.builder(context).setEnableInternalTracer(true).createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(this.eglBase.getEglBaseContext(), true, true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(this.eglBase.getEglBaseContext());
        this.peerConnectionFactory = PeerConnectionFactory.builder().setOptions(options).setVideoEncoderFactory(defaultVideoEncoderFactory).setVideoDecoderFactory(defaultVideoDecoderFactory).createPeerConnectionFactory();
    }

    private void createVideoTrackFromCamera(Context context) {
        this.videoCapturer = this.createVideoCapturer(context);
        if (this.videoCapturer == null) {
            Log.e("VideoCallActivity", "Failed to create video capturer");
        } else {
            SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", this.eglBase.getEglBaseContext());
            VideoSource videoSource = this.peerConnectionFactory.createVideoSource(this.videoCapturer.isScreencast());
            this.videoCapturer.initialize(surfaceTextureHelper, context, videoSource.getCapturerObserver());
            this.videoCapturer.startCapture(1920, 1080, 30);
            this.localVideoTrack = this.peerConnectionFactory.createVideoTrack("ARDAMSv0", videoSource);
            this.localVideoTrack.setEnabled(true);
            this.localVideoTrack.addSink(this.localVideoView);
            MediaConstraints audioConstraints = new MediaConstraints();
            audioConstraints.optional.add(new MediaConstraints.KeyValuePair("googCpuOveruseDetection", "false"));
            audioConstraints.optional.add(new MediaConstraints.KeyValuePair("googHighBitrate", "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googEchoCancellation", "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googAutoGainControl", "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googHighpassFilter", "true"));
            audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("googNoiseSuppression", "true"));
            AudioSource audioSource = this.peerConnectionFactory.createAudioSource(audioConstraints);
            this.localAudioTrack = this.peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
            this.audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            this.audioManager.setSpeakerphoneOn(true);
            this.localAudioTrack.setEnabled(true);
        }
    }

    public View getLocalVideoView() {
        return this.localVideoView;
    }

    public View getRemoteVideoView() {
        return this.remoteVideoView;
    }

    private CameraVideoCapturer createVideoCapturer(Context context) {
        CameraVideoCapturer videoCapturer;
        if (Camera2Enumerator.isSupported(context)) {
            videoCapturer = this.createCameraCapturer(new Camera2Enumerator(context));
        } else {
            videoCapturer = this.createCameraCapturer(new Camera1Enumerator(true));
        }

        return videoCapturer;
    }

    private CameraVideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        String[] deviceNames = enumerator.getDeviceNames();

        for(String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer videoCapturer = enumerator.createCapturer(deviceName, (CameraVideoCapturer.CameraEventsHandler)null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for(String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                CameraVideoCapturer videoCapturer = enumerator.createCapturer(deviceName, (CameraVideoCapturer.CameraEventsHandler)null);
                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private void startCall() {
        if (!this.isStarted && (this.isChannelReady || this.isInitiator)) {
            Log.d("VideoCallActivity", "Starting call");
            this.isStarted = true;
            this.createPeerConnection();
            this.setPreferredCodec();
            this.addTracksToLocalPeerConnection();
            if (this.isInitiator) {
                this.createOffer();
            }
        }

    }

    private void addTracksToLocalPeerConnection() {
        if (this.peerConnection != null && this.localVideoTrack != null && this.localAudioTrack != null) {
            RtpSender videoSender = this.peerConnection.addTrack(this.localVideoTrack);
            RtpParameters parameters = videoSender.getParameters();

            for(RtpParameters.Encoding encoding : parameters.encodings) {
                encoding.maxBitrateBps = 2000000;
                encoding.maxFramerate = 30;
            }

            videoSender.setParameters(parameters);
            this.senders.add(videoSender);
            RtpSender audioSender = this.peerConnection.addTrack(this.localAudioTrack);
            this.senders.add(audioSender);
            Log.d("VideoCallActivity", "Added local audio and video tracks to peer connection");
        } else {
            Log.e("VideoCallActivity", "Cannot add tracks to PeerConnection, it's null");
        }
    }

    private void createPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList();
        iceServers.add(IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(IceServer.builder("turn:openrelay.metered.ca:80").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer());
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.bundlePolicy = BundlePolicy.MAXBUNDLE;
        rtcConfig.sdpSemantics = SdpSemantics.UNIFIED_PLAN;
        rtcConfig.iceTransportsType = IceTransportsType.ALL;
        rtcConfig.rtcpMuxPolicy = RtcpMuxPolicy.REQUIRE;
        rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.DISABLED;
        rtcConfig.enableCpuOveruseDetection = true;
        this.peerConnection = this.peerConnectionFactory.createPeerConnection(rtcConfig, new PeerConnectionObserver() {
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);

                try {
                    JSONObject candidateJson = new JSONObject();
                    candidateJson.put("sdpMid", iceCandidate.sdpMid);
                    candidateJson.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                    candidateJson.put("candidate", iceCandidate.sdp);
                    JSONObject candidateMessage = new JSONObject();
                    candidateMessage.put("type", "candidate");
                    candidateMessage.put("candidate", candidateJson);
                    JSONObject message = new JSONObject();
                    message.put("type", "message");
                    message.put("data", candidateMessage);
                    if (VideoCore.this.webSocket != null && VideoCore.this.webSocket.send(message.toString())) {
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            public void onTrack(RtpTransceiver transceiver) {
                super.onTrack(transceiver);
                Log.d("VideoCallActivity", "onTrack: " + transceiver.getMid());
                if (transceiver.getReceiver().track() instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack)transceiver.getReceiver().track();
                    remoteVideoTrack.setEnabled(true);
                    remoteVideoTrack.addSink(VideoCore.this.remoteVideoView);
                    Log.d("VideoCallActivity", "Added remote video track to renderer");
                }

            }

            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                super.onIceConnectionChange(iceConnectionState);
                Log.d("VideoCallActivity", " Connection State: " + iceConnectionState);
                if (iceConnectionState != IceConnectionState.CONNECTED && iceConnectionState != IceConnectionState.COMPLETED) {
                    if (iceConnectionState == IceConnectionState.FAILED || iceConnectionState == IceConnectionState.DISCONNECTED) {
                        Log.e("VideoCallActivity", "Connection failed or disconnected");
                    }
                } else {
                    Log.d("VideoCallActivity", "Connected to peer");
                }

                if (iceConnectionState != IceConnectionState.CONNECTED && iceConnectionState != IceConnectionState.COMPLETED) {
                    if (iceConnectionState == IceConnectionState.FAILED || iceConnectionState == IceConnectionState.DISCONNECTED) {
                        Log.e("VideoCallActivity", "Connection failed or disconnected");
                    }
                } else {
                    Log.d("VideoCallActivity", "Connected to peer");
                }

            }
        });
        if (this.peerConnection == null) {
            Log.e("VideoCallActivity", "Failed to create PeerConnection");
        }

    }

    private void setPreferredCodec() {
        if (this.peerConnection != null) {
            for(RtpTransceiver transceiver : this.peerConnection.getTransceivers()) {
                if (transceiver.getMediaType() == MediaType.MEDIA_TYPE_VIDEO) {
                    RtpParameters parameters = transceiver.getSender().getParameters();
                    if (parameters != null && parameters.encodings != null && !parameters.encodings.isEmpty()) {
                        for(RtpParameters.Encoding encoding : parameters.encodings) {
                            encoding.maxBitrateBps = 2500000;
                            encoding.minBitrateBps = 1000000;
                            encoding.maxFramerate = 30;
                        }

                        transceiver.getSender().setParameters(parameters);
                    }
                    break;
                }
            }

        }
    }

    private void createOffer() {
        if (this.peerConnection == null) {
            Log.e("VideoCallActivity", "PeerConnection is null when trying to connect");
        } else {
            Log.d("VideoCallActivity", "Creating offer...");
            MediaConstraints sdpMediaConstraints = new MediaConstraints();
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", "30"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", "24"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            this.peerConnection.createOffer(new SimpleSdpObserver() {
                public void onCreateSuccess(final SessionDescription sessionDescription) {
                    Log.d("VideoCallActivity", "Create connection success");
                    VideoCore.this.peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        public void onSetSuccess() {
                            try {
                                JSONObject offerJson = new JSONObject();
                                offerJson.put("type", "offer");
                                offerJson.put("sdp", sessionDescription.description);
                                JSONObject message = new JSONObject();
                                message.put("type", "message");
                                message.put("data", offerJson);
                                if (VideoCore.this.webSocket != null && VideoCore.this.webSocket.send(message.toString())) {
                                    Log.d("VideoCallActivity", "Sent offer: " + message);
                                } else {
                                    Log.e("VideoCallActivity", "Failed to send offer");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, sessionDescription);
                }

                public void onCreateFailure(String s) {
                }
            }, sdpMediaConstraints);
        }
    }

    private void createAnswer() {
        if (this.peerConnection == null) {
            Log.e("VideoCallActivity", "PeerConnection is null when trying to create connection");
        } else {
            MediaConstraints sdpMediaConstraints = new MediaConstraints();
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
            sdpMediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
            this.peerConnection.createAnswer(new SimpleSdpObserver() {
                public void onCreateSuccess(final SessionDescription sessionDescription) {
                    VideoCore.this.peerConnection.setLocalDescription(new SimpleSdpObserver() {
                        public void onSetSuccess() {
                            try {
                                JSONObject answerJson = new JSONObject();
                                answerJson.put("type", "answer");
                                answerJson.put("sdp", sessionDescription.description);
                                JSONObject message = new JSONObject();
                                message.put("type", "message");
                                message.put("data", answerJson);
                                if (VideoCore.this.webSocket != null && VideoCore.this.webSocket.send(message.toString())) {
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, sessionDescription);
                }

                public void onCreateFailure(String s) {
                }
            }, sdpMediaConstraints);
        }
    }

    public boolean toggleCamera() {
        if (this.videoCapturer != null) {
            this.videoCapturer.switchCamera((CameraVideoCapturer.CameraSwitchHandler)null);
            this.isFrontCamera = !this.isFrontCamera;
        }

        return this.isFrontCamera;
    }

    public boolean toggleMute() {
        if (this.localAudioTrack != null) {
            this.isMuted = !this.isMuted;
            this.localAudioTrack.setEnabled(!this.isMuted);
        }

        return this.isMuted;
    }

    public void endCall() {
        if (this.webSocket != null) {
            JSONObject message = new JSONObject();

            try {
                message.put("type", "bye");
                message.put("userId", this.userId);
                message.put("remoteId", this.remoteID);
                this.webSocket.send(message.toString());
                this.webSocket.close(1000, "User ended call");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        this.closeResources();
    }

    public void handleRemoteHangup() {
        this.isStarted = false;
        this.isChannelReady = false;
        this.senders.clear();
        if (this.peerConnection != null) {
            this.peerConnection.close();
            this.peerConnection = null;
        }

        if (this.remoteVideoView != null) {
            this.remoteVideoView.clearImage();
        }

    }

    private void closeResources() {
        this.isStarted = false;
        this.isChannelReady = false;
        this.senders.clear();
        if (this.peerConnection != null) {
            this.peerConnection.close();
            this.peerConnection = null;
        }

        if (this.videoCapturer != null) {
            try {
                this.videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            this.videoCapturer.dispose();
            this.videoCapturer = null;
        }

        if (this.localVideoView != null) {
            this.localVideoView.release();
        }

        if (this.remoteVideoView != null) {
            this.remoteVideoView.release();
        }

        if (this.webSocket != null) {
            this.webSocket.close(1000, "Activity destroyed");
            this.webSocket = null;
        }

    }

    private String preferCodec(String sdp, String codec, boolean isOffer) {
        String[] lines = sdp.split("\r\n");
        int mLineIndex = -1;
        String codecRtpMap = null;

        for(int i = 0; i < lines.length; ++i) {
            if (lines[i].startsWith("m=video")) {
                mLineIndex = i;
            }

            if (lines[i].contains("a=rtpmap") && lines[i].toLowerCase().contains(codec.toLowerCase())) {
                codecRtpMap = lines[i].split(" ")[0].split(":")[1];
            }
        }

        if (mLineIndex != -1 && codecRtpMap != null) {
            String[] parts = lines[mLineIndex].split(" ");
            StringBuilder newMLine = new StringBuilder();
            int partIndex = 0;
            newMLine.append(parts[partIndex++]);
            newMLine.append(" ").append(parts[partIndex++]);
            newMLine.append(" ").append(parts[partIndex++]);
            newMLine.append(" ").append(parts[partIndex++]);
            newMLine.append(" ").append(codecRtpMap);

            for(; partIndex < parts.length; ++partIndex) {
                if (!parts[partIndex].equals(codecRtpMap)) {
                    newMLine.append(" ").append(parts[partIndex]);
                }
            }

            lines[mLineIndex] = newMLine.toString();
            return String.join("\r\n", lines);
        } else {
            return sdp;
        }
    }

    private class SimpleSdpObserver implements SdpObserver {
        private SimpleSdpObserver() {
        }

        public void onCreateSuccess(SessionDescription sessionDescription) {
        }

        public void onSetSuccess() {
        }

        public void onCreateFailure(String s) {
        }

        public void onSetFailure(String s) {
        }
    }

    private class PeerConnectionObserver implements PeerConnection.Observer {
        private PeerConnectionObserver() {
        }

        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        }

        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        }

        public void onIceConnectionReceivingChange(boolean b) {
        }

        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        }

        public void onIceCandidate(IceCandidate iceCandidate) {
        }

        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        }

        public void onAddStream(MediaStream mediaStream) {
        }

        public void onRemoveStream(MediaStream mediaStream) {
        }

        public void onDataChannel(DataChannel dataChannel) {
        }

        public void onRenegotiationNeeded() {
        }

        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        }
    }
}


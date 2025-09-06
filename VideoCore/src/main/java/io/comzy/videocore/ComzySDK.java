package io.comzy.videocore;

import android.content.Context;
import android.view.View;

public class ComzySDK {
    public final Context context;
    private static ComzySDK instance;

    public ComzySDK(Context context) {
        this.context = context;
    }

    public static synchronized ComzySDK getInstance(Context context) {
        if (instance == null) {
            instance = new ComzySDK(context);
        }

        return instance;
    }

    public static void start(Context context, String userId, String remoteId) {
        VideoCore.start(context, userId, remoteId);
    }

    public View getLocalVideoView() {
        VideoCore videoChatClient = VideoCore.getInstance(this.context);
        return videoChatClient.getLocalVideoView();
    }

    public View getRemoteVideoView() {
        VideoCore videoChatClient = VideoCore.getInstance(this.context);
        return videoChatClient.getRemoteVideoView();
    }

    public boolean toggleMute() {
        VideoCore videoChatClient = VideoCore.getInstance(this.context);
        return videoChatClient.toggleMute();
    }

    public boolean toggleCamera() {
        VideoCore videoChatClient = VideoCore.getInstance(this.context);
        return videoChatClient.toggleCamera();
    }

    public void endCall() {
        VideoCore videoChatClient = VideoCore.getInstance(this.context);
        videoChatClient.endCall();
    }
}
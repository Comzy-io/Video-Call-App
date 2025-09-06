package io.comzy.video_calling_app;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.comzy.videocore.ComzySDK;

public class CallActivity extends AppCompatActivity {
    FrameLayout localview,remoteview;
    TextView remoteUserName;
    ImageButton muteBtn,endCall,btnCam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_call);

        String userId = getIntent().getStringExtra("USER_ID");
        String remoteId = getIntent().getStringExtra("REMOTE_ID");

        localview = findViewById(R.id.localVideoView);
        remoteview = findViewById(R.id.remoteVideoView);

        remoteUserName = findViewById(R.id.tvUserName);

        muteBtn = findViewById(R.id.btnMute);
        endCall = findViewById(R.id.btnEndCall);
        btnCam = findViewById(R.id.btnCam);


        remoteUserName.setText(remoteId);

        ComzySDK comzy = ComzySDK.getInstance(this);
        ComzySDK.start(CallActivity.this,userId,remoteId);

        View localVideo = comzy.getLocalVideoView();
        if (localVideo.getParent() != null) {
            ((ViewGroup) localVideo.getParent()).removeView(localVideo);
        }
        localview.addView(localVideo);

        View remoteVideo = comzy.getRemoteVideoView();
        if (remoteVideo.getParent() != null) {
            ((ViewGroup) remoteVideo.getParent()).removeView(remoteVideo);
        }
        remoteview.addView(remoteVideo);

        muteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comzy.toggleMute();
            }
        });

        btnCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comzy.toggleCamera();
            }
        });

        endCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                comzy.endCall();
                finish();
            }
        });
    }
}
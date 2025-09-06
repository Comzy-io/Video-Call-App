package io.comzy.video_calling_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    EditText editTextUserId, editTextRemoteId;
    LinearLayout nextBtn;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editTextUserId = findViewById(R.id.editTextUserId);
        editTextRemoteId = findViewById(R.id.editTextRemoteId);
        nextBtn = findViewById(R.id.buttonSubmit);

        // Check permissions at startup
        checkAndRequestPermissions();

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasPermissions()) {
                    String userId = editTextUserId.getText().toString().trim();
                    String remoteId = editTextRemoteId.getText().toString().trim();

                    Intent intent = new Intent(MainActivity.this, CallActivity.class);
                    intent.putExtra("USER_ID", userId);
                    intent.putExtra("REMOTE_ID", remoteId);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "You have to allow all permissions to proceed", Toast.LENGTH_LONG).show();
                    // Re-request permissions if needed
                    checkAndRequestPermissions();
                }
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPermissions() {
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
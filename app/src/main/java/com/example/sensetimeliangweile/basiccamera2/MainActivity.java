package com.example.sensetimeliangweile.basiccamera2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.sensetimeliangweile.basiccamera2.fragments.Camera2Fragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_FOR_CAMERA = 100;

    private String[] permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private boolean checkPermissionsAndRequestIfNeed() {
        boolean result = true;
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissionsToBeRequested = new ArrayList<>();
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToBeRequested.add(permission);
                    result = false;
                }
            }
            if (!permissionsToBeRequested.isEmpty()) {
                requestPermissions(permissionsToBeRequested.toArray(new String[0]), REQUEST_CODE_FOR_CAMERA);
            }
        }

        return result;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean res = checkPermissionsAndRequestIfNeed();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (res)
            getSupportFragmentManager().beginTransaction().replace(R.id.container, Camera2Fragment.newInstance()).commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_FOR_CAMERA) {
            if (grantResults != null && grantResults.length > 0) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) return;
                }

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, Camera2Fragment.newInstance())
                        .commit();
            }
        }
    }

}

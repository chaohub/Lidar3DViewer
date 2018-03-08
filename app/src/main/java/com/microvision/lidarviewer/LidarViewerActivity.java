/*
 * Copyright 2018 MicroVision, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microvision.lidarviewer;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.Manifest;
import android.content.pm.PackageManager;

/**
 * Created by Chao Chen on 11/22/17.
 */

public class LidarViewerActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private LidarSurfaceView lidarSurfaceView;
    private LidarCameraPreview lidarCameraPreview;
    private final int REQUEST_CAMERA = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestCameraPermission();
    }

    void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            // Camera permission has not been granted yet. Request it directly.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA);
        }
    }

    void startCamera() {
        lidarSurfaceView = new LidarSurfaceView(this);
        SurfaceHolder camHolder = lidarSurfaceView.getHolder();
        lidarCameraPreview = new LidarCameraPreview(lidarSurfaceView);
        camHolder.addCallback(lidarCameraPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setContentView(lidarSurfaceView);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            // Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission has been granted, preview can be displayed
                startCamera();
            } else {
                finish();
            }
        }
    }
}

package com.microvision.lidarviewer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;

public class LidarViewerActivity extends AppCompatActivity {

    private LidarSurfaceView lidarSurfaceView;
    private LidarCameraPreview lidarCameraPreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        lidarSurfaceView = new LidarSurfaceView(this);
        SurfaceHolder camHolder = lidarSurfaceView.getHolder();
        lidarCameraPreview = new LidarCameraPreview(lidarSurfaceView);
        camHolder.addCallback(lidarCameraPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        setContentView(lidarSurfaceView);
    }
}

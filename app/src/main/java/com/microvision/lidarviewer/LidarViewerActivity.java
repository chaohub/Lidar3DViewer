package com.microvision.lidarviewer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;

public class LidarViewerActivity extends AppCompatActivity {

    private LidarSurfaceView mGLView;
    private LidarCameraPreview camPreview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLView = new LidarSurfaceView(this);
        SurfaceHolder camHolder = mGLView.getHolder();
        camPreview = new LidarCameraPreview(mGLView);
        camHolder.addCallback(camPreview);
        camHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        setContentView(mGLView);
    }
}

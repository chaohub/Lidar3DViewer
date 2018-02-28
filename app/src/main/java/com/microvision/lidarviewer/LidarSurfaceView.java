package com.microvision.lidarviewer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

/**
 * Created by Chao Chen on 11/22/17.
 */

class LidarSurfaceView extends GLSurfaceView {

    private final LidarRenderer lidarRenderer;

    public LidarSurfaceView(Context context){
        super(context);

        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        lidarRenderer = new LidarRenderer();

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(lidarRenderer);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float x1 = (x/getWidth() - 0.5f) * 8;
                float y1 = (y/getHeight() - 0.5f) * 8;
                lidarRenderer.setCameraAngle(x1, y1);
                requestRender();
        }
        return true;
    }

    public Mesh getMesh() {
        return lidarRenderer.getMesh();
    }
}
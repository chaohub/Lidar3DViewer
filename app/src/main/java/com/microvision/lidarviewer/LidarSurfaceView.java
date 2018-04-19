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
                float x1 = (x/getWidth() - 0.5f) * 16;
                float y1 = (y/getHeight() - 0.5f) * 16;
                lidarRenderer.setCameraAngle(x1, y1);
                requestRender();
        }
        return true;
    }

    public Mesh getMesh() {
        return lidarRenderer.getMesh();
    }
}
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

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by Chao Chen on 11/22/17.
 */

public class LidarRenderer implements GLSurfaceView.Renderer {
    private long lastTime;
    private int frameCount;
    private Mesh mesh = null;
    private float x;
    private float y;
    private float z = 4.0f;
    private float d = 4.0f;
    // mVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mVPMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private float[] rotationMatrix = new float[16];
    public volatile float angle;

    public LidarRenderer() {
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        // initialize a mesh
        mesh = new Mesh(120, 720);
        lastTime = SystemClock.uptimeMillis();
        frameCount = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = 0.35f / 0.20f;
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 2.4f, 7);
    }

    public void onDrawFrame(GL10 unused) {
        float[] finalMatrix = new float[16];
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setRotateM(rotationMatrix, 0, angle, 0, 0, -1.0f);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, x, y, -z, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mVPMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        // Create a rotation transformation for the triangle
        float angle = 0;
        Matrix.setRotateM(rotationMatrix, 0, -angle, 0, 0, -1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(finalMatrix, 0, mVPMatrix, 0, rotationMatrix, 0);

        // Draw triangle
        if (mesh != null) {
            mesh.draw(finalMatrix);
        }
        fpsCounter();
    }

    /**
     * Log viewer fps every second
     */
    public void fpsCounter() {
        int ret = frameCount;
        long curTime = SystemClock.uptimeMillis();
        if (curTime - lastTime < 1000) {
            frameCount++;
        }
        else {
            Log.i("LidarRenderer", "ImageProcessing finish" + frameCount);
            lastTime = curTime;
            frameCount = 0;
        }
    }

    public static int loadShader(int type, String shaderCode){
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void setCameraAngle(float x, float y) {
        this.x = x - 0.5f;
        this.y = y - 0.5f;
        this.z = (float)Math.sqrt(d*d - x*x -y*y);
    }

    public Mesh getMesh() {
        return mesh;
    }
}
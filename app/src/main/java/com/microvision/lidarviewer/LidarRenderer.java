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
    long lastTime;
    int frameCount;
    private Mesh mMesh = null;
    private float x;
    private float y;
    private float z = 4.0f;
    private float d = 4.0f;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private float[] mRotationMatrix = new float[16];
    private float[] mRotationMatrix2 = new float[16];
    public volatile float mAngle;

    public LidarRenderer() {
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glEnable(GLES20.GL_BLEND);
        // initialize a mesh
        mMesh = new Mesh(120, 720);
        lastTime = SystemClock.uptimeMillis();
        frameCount = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float ratio = 0.35f / 0.20f;
        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 2.4f, 7);
    }

    public void onDrawFrame(GL10 unused) {
        float[] scratch = new float[16];
        float[] scratch2 = new float[16];
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, x, y, -z, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        // Create a rotation transformation for the triangle
        long time = SystemClock.uptimeMillis() % 4000L;
        float angle = 0; //0.090f * ((int) time);
        //Matrix.setRotateM(mRotationMatrix, 0, angle, 0, 0, -1.0f);
        Matrix.setRotateM(mRotationMatrix2, 0, -angle, 0, 0, -1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        //Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
        Matrix.multiplyMM(scratch2, 0, mMVPMatrix, 0, mRotationMatrix2, 0);

        // Draw triangle
        if (mMesh != null) {
            mMesh.draw(scratch2);
        }
        fpsCounter();
    }

    public int fpsCounter() {
        // Caluculate FPS
        int ret = frameCount;
        long curTime = SystemClock.uptimeMillis();
        if (curTime - lastTime < 1000) {
            frameCount++;
        }
        else {
            lastTime = curTime;
            frameCount = 0;
        }
        Log.i("LidarViewer", "ImageProcessing finish");
        return ret;
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
        return mMesh;
    }
}
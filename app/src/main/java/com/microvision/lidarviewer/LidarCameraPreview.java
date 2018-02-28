package com.microvision.lidarviewer;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

/**
 * Created by Chao Chen on 12/11/17.
 */

public class LidarCameraPreview implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private Camera mCamera = null;
    private ShortBuffer frameData = null;
    private int imageFormat;
    private int PreviewSizeWidth;
    private int PreviewSizeHeight;
    private boolean bProcessing = false;
    private Mesh mMesh = null;
    private LidarSurfaceView mGLSurfaceView;

    Handler mHandler = new Handler(Looper.getMainLooper());

    public LidarCameraPreview(LidarSurfaceView glSurfaceView)
    {
        mGLSurfaceView = glSurfaceView;
    }

    @Override
    public void onPreviewFrame(byte[] arg0, Camera arg1)
    {
        // At preview mode, the frame data will push to here.
        if ( !bProcessing )
        {
            frameData = ByteBuffer.wrap(arg0).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            mHandler.post(DoImageProcessing);
        }
    }

    public void onPause()
    {
        mCamera.stopPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3)
    {
        Camera.Parameters parameters=mCamera.getParameters();
        List<Camera.Size> sizes= parameters.getSupportedPreviewSizes();
        PreviewSizeWidth = sizes.get(0).width;
        PreviewSizeHeight =  sizes.get(0).height;
        //List<Integer> formats = parameters.getSupportedPreviewFormats();
        imageFormat = ImageFormat.YUY2;

        parameters.setPreviewSize(PreviewSizeWidth, PreviewSizeHeight);
        parameters.setPreviewFormat(imageFormat);
        mCamera.setParameters(parameters);
        mCamera.startPreview();

        mMesh = mGLSurfaceView.getMesh();
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0)
    {
        mCamera = Camera.open();
        // some platform will show black screen without the following code
        /*try
        {
            // If did not set the SurfaceHolder, the preview area will be black.
            mCamera.setPreviewDisplay(arg0);
        }
        catch (IOException e)
        {
            mCamera.release();
            mCamera = null;
        }
        */
        mCamera.setPreviewCallback(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0)
    {
        mCamera.setPreviewCallback(null);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    private Runnable DoImageProcessing = new Runnable()
    {
        public void run()
        {
            bProcessing = true;

            if (mMesh == null) {
                mMesh = mGLSurfaceView.getMesh();
            }
            else {
                mMesh.cameraFrame(frameData);
            }
            bProcessing = false;
        }
    };
}

package com.microvision.lidarviewer;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by Chao Chen on 11/22/17.
 */

public class Mesh {
    private final String vertexShaderCode =
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "attribute vec4 a_Color;" +
                    "varying vec4 v_Color;" +
                    "void main() {" +
                    // The matrix must be included as a modifier of gl_Position.
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "  v_Color = a_Color;" +
                    "}";
    private final String fragmentShaderCode =
            "precision mediump float;" +
                    //"attribute vec4 dummy;" +
                    //"uniform vec4 vColor;" +
                    "varying vec4 v_Color;" +
                    "void main() {" +
                    "  gl_FragColor = v_Color;" +
                    "}";
    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float meshCoords[];
    private short drawOrder[]; // order to draw vertices
    private final int vertexStride = 7 * 4; // 4 bytes per float
    private int width;
    private int height;
    static final float FOV_W = 0.35842f;
    static final float FOV_H = 0.20764f;
    static final float START_X = -FOV_W /2;
    static final float START_Y = -FOV_H/2; //0.18031f;
    static final float PD_OFFSET_LEFT = -0.03f;
    static final float TOF_UNIT = 0.001f;   // 1 depth unit represent 1mm
    private static final short TOF_OFFSET = 0x0;
    static float scaleMap[];
    static short depthImage[];
    int grow = 500;
    int step = 10;
    private static int MAX_CALI_SAMPLES = 100;
    private int calibrated_samples = 0;
    private int tof[];
    private final float AMP_FACTOR = 1.0f/4096;

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    public Mesh(int width, int height) {
        this.width = width;
        this.height = height;
        scaleMap = new float[width * height * 3];
        depthImage = new short[width * height * 4];
        meshCoords = new float[width * height * 7];
        tof = new int[width * height];

        genTriangles();
        initScaleMap();

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 4 bytes per float)
                meshCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(meshCoords);
        vertexBuffer.position(0);
        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = LidarRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = LidarRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
        // Update vertex coordinates
        vertexBuffer.position(0);
        vertexBuffer.put(meshCoords);

        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        vertexBuffer.position(0);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetAttribLocation(mProgram, "a_Color");
        GLES20.glEnableVertexAttribArray(mColorHandle);
        vertexBuffer.position(3);
        // Prepare the vertex color data
        GLES20.glVertexAttribPointer(
                mColorHandle, 4,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //LidarRenderer.checkGlError("glGetUniformLocation");
        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        //LidarRenderer.checkGlError("glUniformMatrix4fv");
        // Draw triangles
        //GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, width*height);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mColorHandle);
    }

    /**
     * Initialize the scaling factor for every pixel. The scaling factor will be used to adjust incoming lidar depth data.
     */
    public void initScaleMap() {
        double step_x = FOV_W /(width-1);
        double step_y = FOV_H /(height-1);
        double x, y;
        double z = -0.22181f;
        double dist;
        int id = 0;

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                x = START_X + step_x*j;
                y = START_Y + step_y*i;
                /* ideal scalemap */
                if (calibrated_samples < MAX_CALI_SAMPLES) {
                    dist = Math.sqrt(x * x + y * y + z * z) + Math.sqrt((x - PD_OFFSET_LEFT) * (x - PD_OFFSET_LEFT) + y * y + z * z);
                    scaleMap[id++] = (float) (TOF_UNIT * x / dist);
                    scaleMap[id++] = (float) (TOF_UNIT * y / dist);
                    scaleMap[id++] = (float) (TOF_UNIT * z / dist);
                }
                else {
                    /* calibrated scalemap */
                    tof[id/3] = tof[id/3] / MAX_CALI_SAMPLES;
                    int diff = tof[id/3] - TOF_OFFSET;
                    scaleMap[id++] = (float)(x/diff);
                    scaleMap[id++] = (float)(y/diff);
                    scaleMap[id++] = (float)(z/diff);
                }
            }
        }
    }

    /**
     * Generate test image for debugging
     */
    void genTestImage() {
        int id = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                depthImage[id++] = (short)(Math.abs(((float)height/2 - i)/height * ((float)width/2 -j)/width)*grow);
                depthImage[id++] = (short)(Math.abs(((float)height/2 - i)/height * ((float)width/2 -j)/width)*1024);
                id += 2;
            }
        }
        grow += step;
        if (grow > 1500) step = -10;
        else if (grow < 500) step = 10;

        tof2Xyz(ShortBuffer.wrap(depthImage));
    }

    /**
     * Create triangles arrays for solid surface rendering. This is not needed for point rendering
     */
    void genTriangles() {
        int id = 0;
        drawOrder = new short[(width -1) * (height-1) *2 * 3];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                if (i != 0 && j != 0) {
                    drawOrder[id++] = (short) (i * width + j);
                    drawOrder[id++] = (short) ((i - 1) * width + j - 1);
                    drawOrder[id++] = (short) (i * width + j - 1);
                    drawOrder[id++] = (short) (i * width + j);
                    drawOrder[id++] = (short) ((i - 1) * width + j);
                    drawOrder[id++] = (short) ((i - 1) * width + j - 1);
                }
            }
        }
    }

    /**
     * Convert lidar depth to x, y, z position
     * @param buf - The short buffer containing lidar data
     * 4 Bytes per pixel for two camera channel, 2 bytes for depth, 2 bytes for amplitude
     */
    public void tof2Xyz(ShortBuffer buf) {
        int idc = 0;
        int idd = 0;
        int idm = 0;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                short diff = (short)(buf.get(idd++) - TOF_OFFSET);
                if (diff < 100) {
                    diff = (short)(tof[idc/3]);
                }

                meshCoords[idm++] = scaleMap[idc++] * diff * 4;     //x
                meshCoords[idm++] = scaleMap[idc++] * diff * 4;     //y
                meshCoords[idm++] = scaleMap[idc++] * diff * 4;     //z
                short amp = buf.get(idd++);
                meshCoords[idm++] = amp * AMP_FACTOR;       // R
                meshCoords[idm++] = amp * AMP_FACTOR;       // G
                meshCoords[idm++] = amp * AMP_FACTOR;       // B
                meshCoords[idm++] = 1.0f;                   // A - opaque
            }
        }
    }

    /**
     * Process the camera frames.
     *
     * @param buf - The short buffer containing lidar data
     * 4 Bytes per pixel for two camera channel, 2 bytes for depth, 2 bytes for amplitude
     */
    public void cameraFrame(ShortBuffer buf) {
        if (calibrate(buf)) {
            tof2Xyz(buf);
        }
    }

    private boolean calibrate(ShortBuffer buf) {
        if (calibrated_samples < MAX_CALI_SAMPLES) {
            // Accumulate samples
            int len = tof.length;
            for (int i = 0; i < len; i++) {
                tof[i] += buf.get(i*2);
            }

            calibrated_samples++;
            // Set the calibration matrix based on acumulated tof values
            if (calibrated_samples == MAX_CALI_SAMPLES) {
                // Set calibration matrix
                initScaleMap();
            }
            return false;
        }
        else {
            return true;
        }
    }
}

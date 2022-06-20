package com.sliit.poseapp;

import android.graphics.Bitmap;

public interface ImageProcessor {
    void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay);

    /** Processes ByteBuffer image data, e.g. used for Camera1 live preview case. */


    /** Stops the underlying machine learning model and release resources. */
    void stop();
}

package com.sliit.poseapp.movenet;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.sliit.poseapp.FileWriter;
import com.sliit.poseapp.GraphicOverlay;
import com.sliit.poseapp.InferenceInfoGraphic;
import com.sliit.poseapp.ml.FaceDetectionFullRange;
import com.sliit.poseapp.ml.Head;
import com.sliit.poseapp.ml.Mobilenet;
import com.sliit.poseapp.ml.MovenetLightning;
import com.sliit.poseapp.ml.Opt;
import com.sliit.poseapp.ml.Pruqnt;
import com.sliit.poseapp.ml.Quant;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.model.Model;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class MoveNetProcessorBase {
    private static final String TAG = "VisionProcessorBase";
    private Model.Options options;
    //private Head model;
    //private Opt model;
    private Mobilenet model;
    //Pruqnt model;
    //private Quant model;
    private Context context;
    public boolean isComplete;
    private int[] shape;
    private int frameWidth, frameHeight;
    private final Timer fpsTimer = new Timer();
    private int framesPerSecond = 0;
    private final ActivityManager activityManager;
    private int numRuns = 0;
    private long totalFrameMs = 0;
    private long maxFrameMs = 0;
    private long minFrameMs = Long.MAX_VALUE;
    private long totalDetectorMs = 0;
    private long maxDetectorMs = 0;
    private long minDetectorMs = Long.MAX_VALUE;

    private int frameProcessedInOneSecondInterval = 0;

    private FileWriter fileWriter;

    public MoveNetProcessorBase(Context context, Model.Options options) {

        try {
            model=Mobilenet.newInstance(context,options);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.options = options;
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.context = context;
        fileWriter=new FileWriter(context,1);
        fpsTimer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        framesPerSecond = frameProcessedInOneSecondInterval;
                        frameProcessedInOneSecondInterval = 0;
                    }
                },
                /* delay= */ 0,
                /* period= */ 1000);

    }



    public void processBitmap(Bitmap bitmap, GraphicOverlay graphicOverlay) {
        graphicOverlay.clear();
        String x;
        String y;
        long frameStartMs= SystemClock.elapsedRealtime();
        frameWidth = bitmap.getWidth();
        frameHeight = bitmap.getHeight();

        isComplete = false;
        org.tensorflow.lite.support.image.ImageProcessor moveImageProcessor = new org.tensorflow.lite.support.image.ImageProcessor.Builder()
                .add(new ResizeOp(64, 64, ResizeOp.ResizeMethod.BILINEAR))
                .build();
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        tensorImage = moveImageProcessor.process(tensorImage);




            //model=;

            // Creates inputs for reference.
            final long detectorStartMs=SystemClock.elapsedRealtime();
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 3},  DataType.FLOAT32);
            inputFeature0.loadBuffer(tensorImage.getBuffer());


        Mobilenet.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
            shape = outputFeature0.getShape();
            float[] data = outputFeature0.getFloatArray();
            Log.d("shape", String.valueOf(data.length));
            Log.d("out1", String.valueOf(data[0]));
            Log.d("out2", String.valueOf(data[1]));
            Log.d("out3", String.valueOf(data[2]));
//            for (int i = 0; i < 17; i++) {
//                x= String.valueOf((float)data[i * 3 + 1] * frameWidth);
//                y= String.valueOf((float)data[i * 3 + 0] * frameHeight);
//                //Log.d("x", String.valueOf(data[i * 3 + 1] * frameWidth) + "," + String.valueOf(data[i * 3 + 0] * frameHeight));
//                fileWriter.writeText(x.getBytes(StandardCharsets.UTF_8));
//                fileWriter.writeText(',');
//                fileWriter.writeText(y.getBytes(StandardCharsets.UTF_8));
//                fileWriter.writeText(' ');
//            }

//            fileWriter.writeText('\n');
            //model.close();

            isComplete = true;

            long endMs = SystemClock.elapsedRealtime();
            long currentDetectorLatencyMs = endMs - detectorStartMs;
            long currentFrameLatencyMs = endMs - frameStartMs;

            if (numRuns >= 500) {
                resetLatencyStats();
            }
            numRuns++;
            frameProcessedInOneSecondInterval++;
            totalFrameMs += currentFrameLatencyMs;
            maxFrameMs = max(currentFrameLatencyMs, maxFrameMs);
            minFrameMs = min(currentFrameLatencyMs, minFrameMs);
            totalDetectorMs += currentDetectorLatencyMs;
            maxDetectorMs = max(currentDetectorLatencyMs, maxDetectorMs);
            minDetectorMs = min(currentDetectorLatencyMs, minDetectorMs);
            if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Frames per second: " + framesPerSecond);
                Log.d(TAG, "Num of Runs: " + numRuns);
                Log.d(
                        TAG,
                        "Frame latency: max="
                                + maxFrameMs
                                + ", min="
                                + minFrameMs
                                + ", avg="
                                + totalFrameMs / numRuns);
                Log.d(
                        TAG,
                        "Detector latency: max="
                                + maxDetectorMs
                                + ", min="
                                + minDetectorMs
                                + ", avg="
                                + totalDetectorMs / numRuns);

                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                activityManager.getMemoryInfo(mi);
                long availableMegs = mi.availMem / 0x100000L;
                Log.d(TAG, "Memory available in system: " + availableMegs + " MB");


            }
            graphicOverlay.add(
                    new InferenceInfoGraphic(
                            graphicOverlay,
                            currentFrameLatencyMs,
                            currentDetectorLatencyMs,
                            framesPerSecond));
            graphicOverlay.postInvalidate();
        }



    private void resetLatencyStats() {
        numRuns = 0;
        totalFrameMs = 0;
        maxFrameMs = 0;
        minFrameMs = Long.MAX_VALUE;
        totalDetectorMs = 0;
        maxDetectorMs = 0;
        minDetectorMs = Long.MAX_VALUE;
    }

}

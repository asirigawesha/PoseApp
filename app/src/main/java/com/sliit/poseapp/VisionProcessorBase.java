package com.sliit.poseapp;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.android.gms.tasks.Tasks;
import com.google.android.odml.image.BitmapMlImageBuilder;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.common.MlKitException;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public abstract class VisionProcessorBase<T> implements ImageProcessor {
    private OnProcessingCompleteListener onProcessingCompleteListener;
    private final ScopedExecutor executor;
    private final ActivityManager activityManager;
    private final Timer fpsTimer = new Timer();

    // Frame count that have been processed so far in an one second interval to calculate FPS.
    private int frameProcessedInOneSecondInterval = 0;
    private int framesPerSecond = 0;


    // Used to calculate latency, running in the same thread, no sync needed.
    private int numRuns = 0;
    private long totalFrameMs = 0;
    private long maxFrameMs = 0;
    private long minFrameMs = Long.MAX_VALUE;
    private long totalDetectorMs = 0;
    private long maxDetectorMs = 0;
    private long minDetectorMs = Long.MAX_VALUE;

    private static final String TAG = "VisionProcessorBase";

    private boolean isShutdown;
    @GuardedBy("this")
    private ByteBuffer latestImage;

    @GuardedBy("this")
    private FrameMetadata latestImageMetaData;
    // To keep the images and metadata in process.
    @GuardedBy("this")
    private ByteBuffer processingImage;

    @GuardedBy("this")
    private FrameMetadata processingMetaData;

    protected VisionProcessorBase(Context context) {
        activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        executor =  new ScopedExecutor(TaskExecutors.MAIN_THREAD);
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

    public void setOnProcessingCompleteListener(OnProcessingCompleteListener onProcessingCompleteListener) {
        this.onProcessingCompleteListener = onProcessingCompleteListener;
    }
    public interface OnProcessingCompleteListener{
        void onProcessingComplete();
    }

    @Override
    public void processBitmap(Bitmap bitmap,final GraphicOverlay graphicOverlay) {
        long frameStartMs= SystemClock.elapsedRealtime();
        if(isMlImageEnabled(graphicOverlay.getContext())){
            MlImage mlImage=new BitmapMlImageBuilder(bitmap).build();
            requestDetectInImage(
                    mlImage,
                    graphicOverlay,
                    null,
                   true,
                    frameStartMs);
            mlImage.close();
            return;
        }
    }



    protected boolean isMlImageEnabled(Context context) {
        return false;
    }

    private Task<T> requestDetectInImage(
       final MlImage image,
       final GraphicOverlay graphicOverlay,
       @Nullable final Bitmap origianlCameraImage,
       boolean shouldShowFps,
       long frameStartMs){
        return setUpListener(
                detectInImage(image),graphicOverlay,origianlCameraImage,shouldShowFps,frameStartMs);
    }


    protected Task<T> detectInImage(MlImage image) {
        return Tasks.forException(
                new MlKitException(
                        "MlImage is currently not demonstrated for this feature",
                        MlKitException.INVALID_ARGUMENT));
    }


    private Task<T> setUpListener(
           Task<T> task,
           final GraphicOverlay graphicOverlay,
           @Nullable final Bitmap originalCameraImage,
           boolean shouldShowFps,
           long frameStartMs){
        final long detectorStartMs=SystemClock.elapsedRealtime();
        return task.addOnSuccessListener(
                executor,
                results->{
                    long endMs = SystemClock.elapsedRealtime();
                    long currentFrameLatencyMs = endMs - frameStartMs;
                    long currentDetectorLatencyMs = endMs - detectorStartMs;
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
                    // Only log inference info once per second. When frameProcessedInOneSecondInterval is
                    // equal to 1, it means this is the first frame processed during the current second.
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
                    graphicOverlay.clear();
                    if (originalCameraImage != null) {
                        graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
                    }
                    VisionProcessorBase.this.onSuccess(results, graphicOverlay);
//
//                        graphicOverlay.add(
//                                new InferenceInfoGraphic(
//                                        graphicOverlay,
//                                        currentFrameLatencyMs,
//                                        currentDetectorLatencyMs,
//                                        shouldShowFps ? framesPerSecond : null));

                    graphicOverlay.postInvalidate();
                }).addOnFailureListener(
                executor,
                e -> {
                    graphicOverlay.clear();
                    graphicOverlay.postInvalidate();
                    String error = "Failed to process. Error: " + e.getLocalizedMessage();
                    Toast.makeText(
                            graphicOverlay.getContext(),
                            error + "\nCause: " + e.getCause(),
                            Toast.LENGTH_SHORT)
                            .show();
                    Log.d(TAG, error);
                    e.printStackTrace();
                    VisionProcessorBase.this.onFailure(e);
                }
        ).addOnCompleteListener(executor,results->{
            if(onProcessingCompleteListener!=null){
                onProcessingCompleteListener.onProcessingComplete();
            }
        });

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

    @Override
    public void stop() {
        executor.shutdown();
        isShutdown=true;
        resetLatencyStats();
        fpsTimer.cancel();
    }
    protected abstract void onSuccess(@NonNull T results, @NonNull GraphicOverlay graphicOverlay);

    protected abstract void onFailure(@NonNull Exception e);




}


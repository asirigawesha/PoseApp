package com.sliit.poseapp.posedetector;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.odml.image.MlImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.sliit.poseapp.FileWriter;
import com.sliit.poseapp.GraphicOverlay;
import com.sliit.poseapp.VisionProcessorBase;
import com.sliit.poseapp.posedetector.classification.PoseClassifierProcessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PoseDetectorProcessor extends VisionProcessorBase<PoseDetectorProcessor.PoseEstimation> {
    private static final String TAG = "PoseDetectorProcessor";

    private PoseClassifierProcessor poseClassifierProcessor;
    private final PoseDetector detector;
    private final boolean showInFrameLikelihood;
    private final boolean visualizeZ;
    private final boolean rescaleZForVisualization;
    private final boolean runClassification;
    private final boolean isStreamMode;
    private final Context context;
    private final Executor classificationExecutor;
    private FileWriter fileWriter;

    public PoseDetectorProcessor(
            Context context,
            PoseDetectorOptionsBase options,
            boolean showInFrameLikelihood,
            boolean visualizeZ,
            boolean rescaleZForVisualization,
            boolean runClassification,
            boolean isStreamMode) {

        super(context);
        this.showInFrameLikelihood = showInFrameLikelihood;
        this.visualizeZ = visualizeZ;
        this.rescaleZForVisualization = rescaleZForVisualization;
        detector = PoseDetection.getClient(options);
        this.runClassification = runClassification;
        this.isStreamMode = isStreamMode;
        this.context = context;
        classificationExecutor = Executors.newSingleThreadExecutor();
        fileWriter=new FileWriter(context,1);
    }

    @Override
    protected Task<PoseEstimation> detectInImage(MlImage image) {
        return detector
                .process(image)
                .continueWith(
                        classificationExecutor,
                        task -> {
                            Pose pose = task.getResult();
                            List<String> classificationResult = new ArrayList<>();
                            if (runClassification) {
                                if (poseClassifierProcessor == null) {
                                    poseClassifierProcessor = new PoseClassifierProcessor(context, isStreamMode);
                                }
                                classificationResult = poseClassifierProcessor.getPoseResult(pose);
                            }
                            return new PoseEstimation(pose, classificationResult);
                        }
                );
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    protected static class PoseEstimation {
        private final Pose pose;
        private final List<String> classificationResult;

        public PoseEstimation(Pose pose, List<String> classificationResult) {
            this.pose = pose;
            this.classificationResult = classificationResult;
        }

        public Pose getPose() {
            return pose;
        }

        public List<String> getClassificationResult() {
            return classificationResult;
        }

    }

    @Override
    protected void onSuccess(@NonNull PoseEstimation results, @NonNull GraphicOverlay graphicOverlay) {
        List<PoseLandmark> landmarks = results.pose.getAllPoseLandmarks();




            for (PoseLandmark s :landmarks){

                String x3D= String.valueOf(s.getPosition3D().getX());
                String y3D= String.valueOf(s.getPosition3D().getY());
                String z3D= String.valueOf(s.getPosition3D().getZ());

               //String x= String.valueOf(s.getPosition().x);
               // String y= String.valueOf(s.getPosition().y);

               // Log.d("X",x);
                //Log.d("Y",y);
               // Log.d("X3",x3D);
             //   Log.d("Y3",y3D);
             //   Log.d("Z3",z3D);
                fileWriter.writeText(x3D.getBytes(StandardCharsets.UTF_8));
                fileWriter.writeText(',');
                fileWriter.writeText(y3D.getBytes(StandardCharsets.UTF_8));
                fileWriter.writeText(',');
                fileWriter.writeText(z3D.getBytes(StandardCharsets.UTF_8));
                fileWriter.writeText(' ');
            }
           fileWriter.writeText('\n');

            //Toast.makeText(context, "Saved to " + context.getFilesDir() + "/" + FILE_NAME,Toast.LENGTH_SHORT).show();

        graphicOverlay.add(
                new PoseGraphic(
                        graphicOverlay,
                        results.pose,
                        showInFrameLikelihood,
                        visualizeZ,
                        rescaleZForVisualization,
                        results.classificationResult));
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Pose detection failed!", e);
    }

    @Override
    protected boolean isMlImageEnabled(Context context) {
        return true;
    }
}

package com.sliit.poseapp;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;


public class FirstFragment extends Fragment implements View.OnClickListener,ImageAnalysis.Analyzer {
    Button buttonPose1;
    Button buttonVideo;
    PreviewView previewView;
    ImageCapture imageCapture;
    VideoCapture videoCapture;
    ImageAnalysis imageAnalysis;
    PoseDetectorOptions options;

    private ListenableFuture<ProcessCameraProvider> cameraProviderListenableFuture;

    public FirstFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        previewView = view.findViewById(R.id.previewView);
        buttonPose1 = (Button) view.findViewById(R.id.ButtonPoseModel1);
        buttonVideo = (Button) view.findViewById(R.id.ButtonVideo);
        buttonPose1.setOnClickListener(this);
        buttonVideo.setOnClickListener(this);
        cameraProviderListenableFuture = ProcessCameraProvider.getInstance(getActivity());
        cameraProviderListenableFuture.addListener(() -> {
            try {

                ProcessCameraProvider cameraProvider = cameraProviderListenableFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());


        return view;


    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(getActivity());
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        //preview
        Preview preview = new Preview.Builder().build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        //capture
        imageCapture = new ImageCapture.Builder()

                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        //vide
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();
        //analyis
        imageAnalysis = new ImageAnalysis.Builder()

                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalysis.setAnalyzer(getExecutor(), this);

        cameraProvider.bindToLifecycle((LifecycleOwner) getActivity(), cameraSelector, imageAnalysis, preview);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ButtonPoseModel1:
                //capturePhoto();
                break;
            case R.id.ButtonVideo:
                if (buttonVideo.getText() == "Start Video") {
                    buttonVideo.setText("Stop video");
                    //recordVideo();
                } else {
                    buttonVideo.setText("Start Video");
                    videoCapture.stopRecording();
                }
                break;
        }
    }

    @SuppressLint("RestrictedApi")
    private void recordVideo() {

        if (videoCapture != null) {
            //File videoDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/poseapp/");
            File videoDir = new File(getContext().getExternalCacheDir().getAbsolutePath());

            if (!videoDir.exists()) {
                videoDir.mkdirs();
            }
            Date date = new Date();
            String timestamp = String.valueOf(date.getTime());
            String videoPath = videoDir.getAbsolutePath() + "/" + timestamp + ".mp4";
            File vidFile = new File(videoPath);

            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(vidFile).build(),
                    getExecutor(),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Toast.makeText(getActivity(), "saved successfully", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(getActivity(), "Failed to save", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }

    }

    private void capturePhoto() {
        //File photodir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/poseapp");
        File file = new File(getContext().getExternalCacheDir() + File.separator, +System.currentTimeMillis() + ".JPEG");

//        if(!photodir.exists()){
//            photodir.mkdirs();
//
//
//        }
        Log.d("storage", file.toString());
//        Date date = new Date();
//        String timestamp =String.valueOf(date.getTime());
//        String phoFilePath=timestamp+".jpg";
//        File photoFile = new File(photodir,phoFilePath);

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(file).build(),
                getExecutor(),

                new ImageCapture.OnImageSavedCallback() {

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.i("save", String.valueOf(Uri.fromFile(file)));

                        Toast.makeText(getActivity(), "saved successfully", Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.i("error", exception.toString());
                        Toast.makeText(getActivity(), "failed to save", Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }

    @Override
    public void analyze(@NonNull ImageProxy image) {

        //processing

        @SuppressLint("UnsafeOptInUsageError") Image mediaImage = image.getImage();
        if (mediaImage != null) {
            InputImage imagePose =
                    InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());

            options= new PoseDetectorOptions.Builder()
                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                    .build();
            PoseDetector poseDetector = PoseDetection.getClient(options);
            poseDetector.process(imagePose)
                    .addOnSuccessListener(
                            new OnSuccessListener<Pose>() {
                                @Override
                                public void onSuccess(Pose pose) {
                                    processPose(pose);
                                    image.close();

                                }
                            })
                    .addOnFailureListener(
                            new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(getActivity(), "Failed to process", Toast.LENGTH_SHORT).show();
                                }
                            });

        }




    }

    private void processPose(Pose pose) {
        try{
            PoseLandmark leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER);
            PoseLandmark rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);

            PointF leftShoulderP = leftShoulder.getPosition();
            float lShoulderX = leftShoulderP.x;
            float lShoulderY = leftShoulderP.y;
            PointF rightSoulderP = rightShoulder.getPosition();
            float rShoulderX = rightSoulderP.x;
            float rShoulderY = rightSoulderP.y;
            Log.d("leftx", String.valueOf(lShoulderX));

            Paint paint = new Paint();
            paint.setColor(Color.GREEN);
            float strokeWidth = 4.0f;
            paint.setStrokeWidth(strokeWidth);

        }
        catch (Exception e){

        }
    }


}
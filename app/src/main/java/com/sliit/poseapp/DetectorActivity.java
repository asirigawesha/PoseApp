package com.sliit.poseapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.sliit.poseapp.detector.DetectorProcessorBase;
import com.sliit.poseapp.movenet.MoveNetProcessorBase;

import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.support.model.Model;

public abstract class DetectorActivity extends AppCompatActivity {
    private SimpleExoPlayer player;
    private PlayerView playerView;
    private static final int REQUEST_CHOOSE_VIDEO = 1001;
    private Bitmap lastFrame;
    private DetectorProcessorBase imageProcessor;
    private boolean processing;
    private boolean pending;
    private GraphicOverlay graphicOverlay;
    private int frameWidth, frameHeight;
    private FileWriter fileWriter;
    CompatibilityList compatList = new CompatibilityList();
    Model.Options options;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_move_net);
        player=createPlayer();
        playerView=findViewById(R.id.player_view);
        playerView.setPlayer(player);
        FrameLayout contentFrame = playerView.findViewById(R.id.exo_content_frame);

        View videoFrameView = createVideoFrameView();
        if (videoFrameView != null) {
            contentFrame.addView(videoFrameView);
        }

        graphicOverlay = new GraphicOverlay(this, null);
        contentFrame.addView(graphicOverlay);
        findViewById(R.id.choose_btn).setOnClickListener(v -> {
            startChooseVideoIntentForResult();
        });
    }

    private void startChooseVideoIntentForResult() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "select video"), REQUEST_CHOOSE_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_VIDEO && resultCode == RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            fileWriter=new FileWriter(this,2);
            fileWriter.closeWriter();
            setupPlayer(data.getData());
        }
    }

    private void setupPlayer(Uri uri) {
        MediaItem mediaItem = MediaItem.fromUri(uri);
        playerView.hideController();
        player.stop();
        player.setMediaItem(mediaItem);
        player.prepare();

        player.play();

        createImageProcessor();

        if (lastFrame != null) processFrame(lastFrame);
    }

    private void createImageProcessor() {
        //stopImageprocessor();
        if(compatList.isDelegateSupportedOnThisDevice()){
            // if the device has a supported GPU, add the GPU delegate


            options = new Model.Options.Builder().setDevice(Model.Device.CPU).build();

            //Log.d("TAG", options.toString());
        } else {
            // if the GPU is not supported, run on 4 threads
            options = new Model.Options.Builder().setNumThreads(4).build();
        }
        imageProcessor=new DetectorProcessorBase(this,options);

    }
    protected Size getSizeForDesiredSize(int width, int height, int desiredSize) {
        int w, h;
        if (width > height) {
            w = desiredSize;
            h = Math.round((height / (float) width) * w);
        } else {
            h = desiredSize;
            w = Math.round((width / (float) height) * h);
        }
        return new Size(w, h);
    }

    protected void processFrame(Bitmap frame) {

        lastFrame = frame;
        if (imageProcessor != null) {
            pending = processing;
            if (!processing) {
                processing = true;
                if (frameWidth != frame.getWidth() || frameHeight != frame.getHeight()) {
                    frameWidth = frame.getWidth();
                    frameHeight = frame.getHeight();
                  graphicOverlay.setImageSourceInfo(frameWidth, frameHeight, false);
                }

             imageProcessor.processBitmap(frame,graphicOverlay);
             if(imageProcessor.isComplete){
                 processing = false;
                 onProcessComplete(frame);
                 if (pending) {
                     processFrame(lastFrame);
                 }
             }

            }

        }

//        if(!player.isPlaying()){
//            Log.d("eds","ed");
//        }

    }

    private void stopImageprocessor() {

        if (imageProcessor != null) {
            //imageProcessor.stop();
            imageProcessor = null;
            processing = false;
            pending = false;
        }
    }
    private void onProcessComplete(Bitmap frame) {

    }
    @Override
    protected void onPause() {
        super.onPause();
        player.pause();
        stopImageprocessor();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.stop();
        player.release();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //player.play();
        //createImageProcessor();

        //if (lastFrame != null) processFrame(lastFrame);

    }
    @NonNull
    protected abstract SimpleExoPlayer createPlayer() ;

    @Nullable
    protected abstract View createVideoFrameView();

}
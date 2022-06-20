package com.sliit.poseapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;
import com.sliit.poseapp.posedetector.PoseDetectorProcessor;

public abstract class PoseActivity extends AppCompatActivity {

    private SimpleExoPlayer player;
    private PlayerView playerView;
    private static final int REQUEST_CHOOSE_VIDEO = 1001;
    private Bitmap lastFrame;
    private VisionProcessorBase imageProcessor;
    private boolean processing;
    private boolean pending;
    private int frameWidth, frameHeight;
    private GraphicOverlay graphicOverlay;
    PoseDetectorOptions poseDetectorOptions;
    private FileWriter fileWriter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pose);
        player = createPlayer();
        playerView = findViewById(R.id.player_view);
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

    protected abstract @NonNull
    SimpleExoPlayer createPlayer();

    protected abstract @Nullable
    View createVideoFrameView();


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

    private void setupPlayer(Uri uri) {
        MediaItem mediaItem = MediaItem.fromUri(uri);
        playerView.hideController();
        player.stop();
        player.setMediaItem(mediaItem);
        player.prepare();

        player.play();


        createImageProcessor();

        if (lastFrame != null) processFrame(lastFrame);

        player.addListener(new Player.Listener(){
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if(playbackState==player.STATE_ENDED){
                   // Log.d("eds","ed");
                   // String tes=fileWriter.readText();

                }
            }
        });


    }

    private void createImageProcessor() {
        stopImageprocessor();
        imageProcessor = new PoseDetectorProcessor(
                this,
                poseDetectorOptions = new PoseDetectorOptions.Builder()
                        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                        .build(),
                true,
                true,
                true,
                false,
                true
        );
    }

    private void stopImageprocessor() {

        if (imageProcessor != null) {
            imageProcessor.stop();
            imageProcessor = null;
            processing = false;
            pending = false;
        }
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
        createImageProcessor();

        if (lastFrame != null) processFrame(lastFrame);

    }

    protected void processFrame(Bitmap frame) {
        lastFrame = frame;
        if (imageProcessor != null) {
            pending = processing;
            if (!processing) {
                processing = true;
                player.pause();
                if (frameWidth != frame.getWidth() || frameHeight != frame.getHeight()) {
                    frameWidth = frame.getWidth();
                    frameHeight = frame.getHeight();
                    graphicOverlay.setImageSourceInfo(frameWidth, frameHeight, false);
                }
                imageProcessor.setOnProcessingCompleteListener(new VisionProcessorBase.OnProcessingCompleteListener() {
                    @Override
                    public void onProcessingComplete() {
                        processing = false;
                        player.play();
                        onProcessComplete(frame);
                        if (pending) {
                            processFrame(lastFrame);
                        }
                    }
                });
                imageProcessor.processBitmap(frame, graphicOverlay);


            }

        }

//        if(!player.isPlaying()){
//            Log.d("eds","ed");
//        }

    }

    private void onProcessComplete(Bitmap frame) {

    }
}
package com.sliit.poseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.google.android.exoplayer2.SimpleExoPlayer;

public class VideoTextureViewActivity extends PoseActivity implements TextureView.SurfaceTextureListener {

    private SimpleExoPlayer player;
    private TextureView textureView;
    private Surface playerSurface;
    private SurfaceTexture surfaceTexture;

    @Override
    protected SimpleExoPlayer createPlayer(){
        player=new SimpleExoPlayer.Builder(this).build();
        return player;
    }
    @Override
    protected View createVideoFrameView(){
        textureView=new TextureView(this);
        textureView.setSurfaceTextureListener(this);
        return textureView;
    }
    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int i, int i1) {
        surfaceTexture =surface;
        playerSurface=new Surface(surface);
        player.setVideoSurface(playerSurface);
    }


    @Override
    public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
        player.setVideoSurface(null);
        playerSurface.release();
        playerSurface=null;
        surfaceTexture.release();
        surfaceTexture=null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
        Size size = getSizeForDesiredSize(textureView.getWidth(), textureView.getHeight(), 500);
        processFrame(textureView.getBitmap(size.getWidth(), size.getHeight()));
    }


}
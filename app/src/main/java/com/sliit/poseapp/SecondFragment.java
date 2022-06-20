package com.sliit.poseapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class SecondFragment extends Fragment implements View.OnClickListener{


    Button poseButton;
    Button moveNetButton;
    Button detectButton;


    public SecondFragment() {
        // Required empty public constructor
    }




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_second, container, false);
        poseButton=(Button) view.findViewById(R.id.buttonPose);
        moveNetButton=(Button) view.findViewById(R.id.buttonMovenet);
        detectButton=(Button) view.findViewById(R.id.buttonDetect);
        poseButton.setOnClickListener(this);
        moveNetButton.setOnClickListener(this);
        detectButton.setOnClickListener(this);
        return view;

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonPose:

                Intent videoTextureActivity = new Intent(getContext(), VideoTextureViewActivity.class);
                startActivity(videoTextureActivity);
                break;

            case R.id.buttonMovenet:
                Intent MoveNetActivity=new Intent(getContext(), TextureMoveNetActivity.class);
                startActivity(MoveNetActivity);
                break;
            case R.id.buttonDetect:
                Intent DetecorActitivy=new Intent(getContext(), TextureDetectorActivity.class);
                startActivity(DetecorActitivy);
                break;


        }
    }
}
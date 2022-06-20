package com.sliit.poseapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;


public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    BottomNavigationView bottomNavigationView;
    FirstFragment firstFragment=new FirstFragment();
    SecondFragment secondFragment=new SecondFragment();
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_WRITE_CODE = 200;
    private static final int STORAGE_READ_CODE = 300;
    private static final int AUDIO_RECORD = 400;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView=findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.page_2);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        }
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_WRITE_CODE);
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_READ_CODE);
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_RECORD);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==MY_CAMERA_REQUEST_CODE){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"camera permission granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this,"Camera permission denied",Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode==STORAGE_WRITE_CODE){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"write permission granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this,"write permission denied",Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode==STORAGE_READ_CODE){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"read permission granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this,"read permission denied",Toast.LENGTH_SHORT).show();
            }
        }
        if(requestCode==AUDIO_RECORD){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"audio permission granted", Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(this,"audio permission denied",Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        int id=item.getItemId();


            if(id== R.id.page_1) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, firstFragment).commit();
                return true;
            }
            else if(id==R.id.page_2) {
                getSupportFragmentManager().beginTransaction().replace(R.id.flFragment, secondFragment).commit();
                return true;
            }

        return false;
    }
}
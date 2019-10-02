package com.airhero;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.Button;

import com.airhero.R;


public class Activity_Choose extends AppCompatActivity {
    private Button roverButton;
    private Button handheldButton;
    private Button userUploadButton;
    private Button dataButton;

    private boolean visible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose);
        dataButton = findViewById(R.id.dataButton);
        dataButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v){
                if (visible) {
                    roverButton.setVisibility(View.INVISIBLE);
                    handheldButton.setVisibility(View.INVISIBLE);
                    visible = false;
                } else {
                    roverButton.setVisibility(View.VISIBLE);
                    handheldButton.setVisibility(View.VISIBLE);
                    visible = true;
                }
            }
        });
        handheldButton = (Button) findViewById(R.id.handheldButton);
        handheldButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                openActivityHandheld();
            }
        });
        roverButton=(Button) findViewById(R.id.roverButton);
        roverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivityRover();

            }
        });
        userUploadButton = (Button) findViewById(R.id.userUploadButton);
        userUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivityUserUpload();

            }
        });
    }

    public void openActivityHandheld(){
        Intent intent = new Intent(this, Activity_Handheld.class);
        startActivity(intent);
    }
    public void openActivityRover() {
        Intent intent = new Intent(this, Activity_Rover.class);
        startActivity(intent);
    }
    public void openActivityUserUpload(){
        Intent intent = new Intent(this, Activity_User_Upload.class);
        startActivity(intent);
    }
}

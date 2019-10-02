package com.airhero;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.airhero.R;

public class Activity_Rover extends AppCompatActivity {
    private Button continueRoverButton;
    private Button info;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rover);

        continueRoverButton = (Button) findViewById(R.id.continueRoverButton);
        continueRoverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivityVOCDataset();
            }
        });
        info = (Button) findViewById(R.id.info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivityInstruction();
            }

        });
    }
    public void openActivityVOCDataset() {
        Intent intent = new Intent(this, Activity_Collect_Rover.class);
        startActivity(intent);
    }
    public void openActivityInstruction(){
        Intent intent = new Intent(this, Activity_Instruction.class);
        startActivity(intent);
    }

}

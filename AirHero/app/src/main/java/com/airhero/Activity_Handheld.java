package com.airhero;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.airhero.R;

public class Activity_Handheld extends AppCompatActivity {
    private Button continueHandheldButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handheld);

        continueHandheldButton = (Button) findViewById(R.id.continueHandheldButton);
        continueHandheldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivityVOCDataset();
            }
        });
    }
    public void openActivityVOCDataset(){
        Intent intent= new Intent(this, Activity_Collect_Handheld.class);
        startActivity(intent);
    }
}

package com.airhero;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.airhero.R;

import java.util.Arrays;
import java.util.List;

public class Activity_Report extends AppCompatActivity {

    String serverGetImage = "";
    float vocThreshold = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        Intent fromCollection = getIntent();
        String vocReadings = fromCollection.getExtras().getString("vocReadings");
        List<String> vocReadingList = Arrays.asList(vocReadings.substring(1, vocReadings.length() - 1).split(", "));
        String imageUri = fromCollection.getExtras().getString("images");
        List<String> imageList = Arrays.asList(imageUri.substring(1, imageUri.length() - 1).split(", "));

        int length = vocReadingList.size();

        LinearLayout layout = (LinearLayout) findViewById(R.id.linearReportPictures);

        for (int i = 0; i < length; i++){
            RelativeLayout smallLayout = new RelativeLayout(this);
            // Add images
            ImageView imageView = new ImageView(this);
            imageView.setId(i);
            imageView.setPadding(2, 2, 2, 2);
            imageView.setImageURI(Uri.parse(imageList.get(i)));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            smallLayout.addView(imageView);

            // Add IDs
            TextView textView = new TextView(this);
            textView.setId(i);
            textView.setPadding(2, 2, 2, 2);
            if (Float.parseFloat(vocReadingList.get(i)) >= vocThreshold) {
                textView.setText("VOC Level: Harmful");
            } else {
                textView.setText("VOC Level: Safe");
            }
            smallLayout.addView(textView);
            layout.addView(smallLayout);
        }

        Button finish = (Button) findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Thank you for your help in data collection!", Toast.LENGTH_SHORT).show();
                openMainActivity();
            }
        });
    }

    public void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
}

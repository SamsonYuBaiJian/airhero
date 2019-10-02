package com.airhero;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.airhero.R;

import org.json.JSONArray;
import org.json.JSONException;

// TODO: Reset AsyncTask when Activity changes

public class Activity_Collect_Rover<ArrayAdapter> extends AppCompatActivity {
    // GUI components
    private TextView mServerStatus;
    private TextView mReadBuffer;
    private ProgressBar progressBar;
    private String vocReading;
    private String vocReadingTemp;

    private Button reportButtonRover;

    private final String TAG = MainActivity.class.getSimpleName();

    // TODO: Image download
    private static final int PERMISSION_CODE = 1000;
    private String serverGet = "http://ec2-13-234-38-156.ap-south-1.compute.amazonaws.com/sih/public/fetchhand";

    private float vocThreshold = 7;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_rover);

        mServerStatus = (TextView)findViewById(R.id.serverStatus);
        mReadBuffer = (TextView) findViewById(R.id.readBuffer2);
        progressBar = (ProgressBar) findViewById(R.id.progressBar2);
        int darkGray = Color.parseColor("#000000");
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(darkGray));

        reportButtonRover = (Button) findViewById(R.id.reportButtonRover);
        reportButtonRover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityReport();
            }
        });

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        // Receive VOC readings from server
        vocAsyncTask vocAsyncTask = new vocAsyncTask();
        vocAsyncTask.execute();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public void openActivityReport() {
        Intent intent = new Intent(this, Activity_Report.class);
        startActivity(intent);
    }

    private class vocAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Work on UI thread here
            while(true) {
                RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                // Prepare the request
                JsonArrayRequest getRequest = new JsonArrayRequest(Request.Method.GET, serverGet, null,
                        new Response.Listener<JSONArray>() {
                            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                            @Override
                            public void onResponse(JSONArray response) {
                                // Display response
                                try {
                                    Log.d("VOC Response", response.getJSONObject(0).toString());
                                    vocReading = response.getJSONObject(0).getString("voc");
                                    String finalString = response.getJSONObject(0).getString("voc") + " PPM";
                                    mReadBuffer.setText(finalString);
                                    if (Float.valueOf(response.getJSONObject(0).getString("voc")) >= vocThreshold){
                                        int red = Color.parseColor("#FF0000");
                                        progressBar.setIndeterminateTintList(ColorStateList.valueOf(red));
                                    }else{
                                        int green = Color.parseColor("#00ff00");
                                        progressBar.setIndeterminateTintList(ColorStateList.valueOf(green));
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Error.Response", error.toString());
                            }
                        }
                ) {
                    @Override
                    public String getBodyContentType() {
                        return "application/json; charset=utf-8";
                    }
                };
                requestQueue.add(getRequest);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Void result) {
        }

        @Override
        protected void onPreExecute() {}

        @Override
        protected void onProgressUpdate(Void... values) {}
    }

    // Handling permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //this method is called, when user presses Allow or Deny from Permission Request Popup
        switch (requestCode){
            case PERMISSION_CODE:{
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED){
                    //permission from popup was granted
                }
                else {
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}



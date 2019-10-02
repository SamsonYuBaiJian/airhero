package com.airhero;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.airhero.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// TODO: Reset AsyncTask when Activity changes

public class Activity_Collect_Handheld<ArrayAdapter> extends AppCompatActivity {
    // Pop up
    Dialog myDialog;

    // Buttons
    private Button takeImageButton;
    private Button reportButtonHandheld;

    // GUI components
    private TextView mServerStatus;
    private TextView mReadBuffer;
    private ProgressBar progressBar;
    private String vocReading;
    private String vocReadingTemp;

    private final String TAG = MainActivity.class.getSimpleName();

    // Image upload
    protected static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 0;
    private Uri imageUri;
    ImageButton imageButton;
    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    private int imagesTaken = 0;
    private String serverPost = "http://ec2-13-234-38-156.ap-south-1.compute.amazonaws.com/sih/public/phone";
    private String serverGet = "http://ec2-13-234-38-156.ap-south-1.compute.amazonaws.com/sih/public/fetchhand";
    private String serverPostImages = "http://ec2-13-234-38-156.ap-south-1.compute.amazonaws.com/sih/public/file";

    // Save VOC readings
    List<String> vocReadings = new ArrayList<String>();
    List<String> images = new ArrayList<String>();
    List<String> tempImages = new ArrayList<String>();

    private float vocThreshold = 7;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_handheld);

        mServerStatus = (TextView)findViewById(R.id.serverStatus);
        mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        int darkGray = Color.parseColor("#000000");
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(darkGray));
        mServerStatus.setText("Connecting...");

        // Ask for location permission if not already allowed
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        // Receive VOC readings from server
        vocAsyncTask vocAsyncTask = new vocAsyncTask();
        vocAsyncTask.execute();

        myDialog = new Dialog(this);

        // Set button listeners
        takeImageButton = (Button) findViewById(R.id.takeImageButton);
        takeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImage(v);
            }
        });
        reportButtonHandheld = (Button) findViewById(R.id.reportButtonHandheld);
        reportButtonHandheld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openActivityReport();
            }
        });

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    public void openActivityReport() {
        Intent intent = new Intent(this, Activity_Report.class);

        // Pass VOC reading IDs to Activity_Report
        Bundle extras = new Bundle();
        extras.putString("vocReadings", vocReadings.toString());
        extras.putString("images", images.toString());
        intent.putExtras(extras);

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
                                    mServerStatus.setText("Connected!");
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

    private Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                if (imagesTaken < 4){
                    try {
                        // Resize image to make it smaller
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        bitmap = Bitmap.createScaledBitmap(bitmap,(int)(bitmap.getWidth()*0.3), (int)(bitmap.getHeight()*0.3), true);
                        imageUri = getImageUri(this,bitmap);
                        tempImages.add(imageUri.toString());

                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, Integer.toString(imagesTaken + 1));
                        values.put(MediaStore.Images.Media.DESCRIPTION, "Picture " + Integer.toString(imagesTaken + 1));
                        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        // Camera intent
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                        imagesTaken+=1;
                        startActivityForResult(cameraIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
                    } catch (IOException e) {
                        e.printStackTrace();
                        imagesTaken = 0;
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Picture was not taken", Toast.LENGTH_SHORT).show();
                imagesTaken = 0;
            }
        }
    }

    public void uploadImage(View V){
        TextView closeImage;
        Button upload;

        myDialog.setContentView(R.layout.upload_image);
        closeImage =(TextView) myDialog.findViewById(R.id.closeImage);
        closeImage.setText("X");
        closeImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imagesTaken = 0;
                myDialog.dismiss();
            }
        });

        imageButton = (ImageButton) myDialog.findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Make sure user can take photos only when server is sending in VOC data is on (to match it with VOC readings)
                // Check if app is getting VOC data from server
                if (mServerStatus.getText() != "Connecting..."){
                    // Get snapshot of VOC readings
                    vocReadingTemp = vocReading;

                    // Use camera to take image and show image
                    //if system os is >= marshmallow, request runtime permission
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        if (checkSelfPermission(Manifest.permission.CAMERA) ==
                                PackageManager.PERMISSION_DENIED ||
                                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                        PackageManager.PERMISSION_DENIED){
                            //permission not enabled, request it
                            String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                            //show popup to request permissions
                            requestPermissions(permission, PERMISSION_CODE);
                        }
                        else {
                            //permission already granted
                            imagesTaken = 0;
                            openCamera();
                        }
                    }
                    else {
                        //system os < marshmallow
                        imagesTaken = 0;
                        openCamera();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please make sure the server is working", Toast.LENGTH_LONG).show();
                }
            }
        });

        upload = (Button) myDialog.findViewById(R.id.upload);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if four images are taken
                if (imagesTaken == 4){
                    // POST VOC reading and features to online database
                    try {
                        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

                        JSONObject postData = new JSONObject();

                        postData.put("voc", vocReadingTemp);

                        // Add to all readings to pass to Activity_Report
                        for (int i = 0; i < 3;i++) {
                            vocReadings.add(vocReadingTemp);
                            images.add(tempImages.get(i));
                        }

                        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, serverPost, postData,new Response.Listener<JSONObject>(){

                            @Override
                            public void onResponse(JSONObject response) {
                                // display response
                                Log.d("Response", response.toString());
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Error.Response", error.toString());
                            }
                        }) {
                            @Override
                            public String getBodyContentType() {
                                return "application/json; charset=utf-8";
                            }
                        };

                        requestQueue.add(jsonRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // POST four images to online database
                    int serverResponseCode;
                    try {
                        int submitted = 0;
                        String boundary = "*****";
                        String twoHyphens = "--";
                        String lineEnd = "\r\n";
                        URL url = new URL(serverPostImages);

                        for (int i = 0; i < 3; i++){
                            // open a URL connection to the Servlet
                            FileInputStream fileInputStream = new FileInputStream(new File(getPath(Uri.parse(tempImages.get(i)))));

                            // Open a HTTP  connection to  the URL
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setDoInput(true); // Allow Inputs
                            conn.setDoOutput(true); // Allow Outputs
                            conn.setUseCaches(false); // Don't use a Cached Copy
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Connection", "Keep-Alive");
                            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                            conn.setRequestProperty("file", getPath(Uri.parse(tempImages.get(i))));

                            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());

                            dos.writeBytes(twoHyphens + boundary + lineEnd);
                            dos.writeBytes("Content-Disposition: form-data; name=file;filename=" + getPath(Uri.parse(tempImages.get(i))) + lineEnd);
                            dos.writeBytes(lineEnd);

                            // create a buffer of  maximum size
                            int bytesAvailable = fileInputStream.available();
                            int maxBufferSize = 1 * 1024 * 1024;
                            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            byte[] buffer = new byte[bufferSize];

                            // read file and write it into form...
                            int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                            while (bytesRead > 0) {

                                dos.write(buffer, 0, bufferSize);
                                bytesAvailable = fileInputStream.available();
                                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                            }

                            // Send multipart form data necesssary after file data...
                            dos.writeBytes(lineEnd);
                            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                            // Responses from the server (code and message)
                            serverResponseCode = conn.getResponseCode();
                            String serverResponseMessage = conn.getResponseMessage();

                            Log.i("uploadImages", "HTTP Response for image uploads is : "
                                    + serverResponseMessage + ": " + serverResponseCode);

                            if(serverResponseCode == 200) {
                                submitted++;
                            }
                            // Close the streams
                            fileInputStream.close();
                            dos.flush();
                            dos.close();
                        }

                        if (submitted == 3){
                            // All four images are submitted
                            imagesTaken = 0;
                            tempImages = new ArrayList<String>();
                            myDialog.dismiss();
                            Toast.makeText(getApplicationContext(), "Submitted", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Got Exception: see logcat ",
                                Toast.LENGTH_SHORT).show();
                        Log.e("Upload image exception", "Exception: "
                                + e.getMessage(), e);
                    }
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please take four images of your surroundings", Toast.LENGTH_SHORT).show();
                }
            }
        });

        myDialog.show();
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        startManagingCursor(cursor);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
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
                    openCamera();
                }
                else {
                    //permission from popup was denied
                    Toast.makeText(this, "Permission denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void openCamera() {
        tempImages = new ArrayList<String>();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "1");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Picture 1");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        // Camera intent
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        imagesTaken++;
        startActivityForResult(cameraIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
    }
}


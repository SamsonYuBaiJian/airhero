package com.airhero;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.airhero.R;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Activity_User_Upload extends AppCompatActivity {
    private Button predict;

    ImageButton imageButton;
    String serverPostModel = "";
    private static final int PERMISSION_CODE = 1000;

    // Save VOC readings
    List<String> vocReadings = new ArrayList<String>();
    List<String> images = new ArrayList<String>();
    List<String> tempImages = new ArrayList<String>();

    // Image upload
    protected static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 0;
    private Uri imageUri;
    int imagesTaken = 0;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_upload);

        imageButton = (ImageButton) findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Use camera to take image and show image
                //if system os is >= marshmallow, request runtime permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if (checkSelfPermission(android.Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED){
                        //permission not enabled, request it
                        String[] permission = {android.Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
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
            }
        });

        predict = (Button) findViewById(R.id.predict);
        predict.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Check if four images are taken
                if (imagesTaken == 4){
                    // Add to all readings to pass to Activity_Report
                    for (int i = 0; i < 3;i++) {
                        images.add(tempImages.get(i));
                    }

                    // POST four images to online database
                    int serverResponseCode;
                    try {
                        int submitted = 0;
                        String boundary = "*****";
                        String twoHyphens = "--";
                        String lineEnd = "\r\n";
                        URL url = new URL(serverPostModel);

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
                            Toast.makeText(getApplicationContext(), "Submitted", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Got Exception: see logcat ",
                                Toast.LENGTH_SHORT).show();
                        Log.e("Upload image exception", "Exception: "
                                + e.getMessage(), e);
                    }

                    // Shift to report
                    openActivityReport();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Please take four images of your surroundings", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void openActivityReport(){
        Intent intent= new Intent(this, Activity_Report.class);
        startActivity(intent);
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

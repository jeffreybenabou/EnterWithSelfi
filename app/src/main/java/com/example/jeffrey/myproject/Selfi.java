package com.example.jeffrey.myproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.ComparedFace;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.FaceMatch;
import com.amazonaws.services.rekognition.model.Image;
import com.apollographql.apollo.api.internal.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.example.jeffrey.myproject.MainActivity.uri;

public class Selfi extends AppCompatActivity {

    private Bitmap selfi,id;
    private Uri image;
    private Image idImage,selfiImage;
    private ProgressBar progressBar;
    private AmazonRekognition amazonRekognition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selfi);
    }

    private Image setTheImage(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/6, bitmap.getHeight()/6, false);
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);
        ByteBuffer imageBytes = ByteBuffer.wrap(stream.toByteArray());
        image.withBytes(imageBytes);
        return image;
    }

    private void cameraIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            if (photoFile != null) {
                image = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, image);
                startActivityForResult(takePictureIntent, 0);
            }
        }
    }

    private void setBothImages(){
        selfiImage=setTheImage(selfi);
        idImage=setTheImage(id);
        checkIfTheSelfiFitUser();
    }



    public void onClick(View view){
        takeASelfi();
    }
    private void takeASelfi() {
        progressBar = findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.VISIBLE);
        cameraIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 0) {
            checkTheImageSize();
            setBothImages();
        }
    }

    private void checkTheImageSize() {
        try {
            selfi = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image);
            if (selfi.getRowBytes() * selfi.getHeight() > 5000000)
                selfi = MainActivity.getResizedBitmap(selfi);

            id = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
            if (id.getRowBytes() * id.getHeight() > 5000000)
                id = MainActivity.getResizedBitmap(id);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void checkIfTheSelfiFitUser() {
        new Thread(new Runnable() {
            @Override
            public void run() {


                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "us-east-1:0aa1be33-7099-4b66-b392-3e2a3309a59b",
                        Regions.US_EAST_1
                );

                CompareFacesRequest compareFacesRequest = new CompareFacesRequest();
                compareFacesRequest.setSourceImage(idImage);
                compareFacesRequest.setTargetImage(selfiImage);


                amazonRekognition = new AmazonRekognitionClient(credentialsProvider);
                try {
                    if (amazonRekognition.compareFaces(compareFacesRequest).getFaceMatches().get(0).getSimilarity() > 95) {
                        popToast("הפנים מתאימות אל תעודת הזהות ברוך הבא!");
                        startActivity(new Intent(Selfi.this, AfterLogin.class));
                    } else
                        popToast("הפנים לא מתאימות אל תעודת הזהות,נסה שוב.");

                } catch (IndexOutOfBoundsException e) {
                    e.fillInStackTrace();
                    e.printStackTrace();
                    popToast("לא זוהו פנים ,יש להכניס תמונה תקינה של הפנים");


                } catch (Exception e) {
                    e.printStackTrace();

                    popToast("קרתה שגיאה לא ידועה");


                }
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });

            }
        }).start();


    }



    private void popToast(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Selfi.this,text,Toast.LENGTH_LONG).show();
            }
        });

    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );


        return image;
    }





















}

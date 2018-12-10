package com.example.jeffrey.myproject;

import android.content.Intent;
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
    Uri image;
    private Image idImage,selfiImage;
    private     String mCurrentPhotoPath;
    private ProgressBar progressBar;

    AmazonRekognition amazonRekognition;


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
         progressBar=findViewById(R.id.progressBar2);
        progressBar.setVisibility(View.VISIBLE);
        cameraIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK&&requestCode==0)
        {
            try {


                selfi = MediaStore.Images.Media.getBitmap(this.getContentResolver(), image);


                id=MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
                setBothImages();
            } catch (Exception e) {
                e.printStackTrace();
            }




        }
    }

    private void checkIfTheSelfiFitUser() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                CompareFacesRequest comparedFace=new CompareFacesRequest();

                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                        getApplicationContext(),
                        "us-east-1:0aa1be33-7099-4b66-b392-3e2a3309a59b", // Identity pool ID
                        Regions.US_EAST_1 // Region
                );

                CompareFacesRequest compareFacesRequest=new CompareFacesRequest();
                compareFacesRequest.setSourceImage(idImage);
                compareFacesRequest.setTargetImage(selfiImage);



                amazonRekognition = new AmazonRekognitionClient(credentialsProvider);
                try {
                    if( amazonRekognition.compareFaces(compareFacesRequest).getFaceMatches().get(0).getSimilarity()>95)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(Selfi.this,"הפנים מתאימות אל תעודת הזהות ברוך הבא !",Toast.LENGTH_LONG).show();

                            }
                        });
                    else
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Selfi.this,"הפנים לא מתאימות אל תעודת הזהות",Toast.LENGTH_LONG).show();


                        }
                    });

                }catch (IndexOutOfBoundsException e)
                {
                    e.fillInStackTrace();
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Selfi.this,"לא זוהו פנים ,יש להכניס תמונה תקינה של הפנים",Toast.LENGTH_LONG).show();

                        }
                    });

                }catch (Exception e)
                {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Selfi.this,"קרתה שגיאה לא ידועה",Toast.LENGTH_LONG).show();

                        }
                    });
                }
                progressBar.setVisibility(View.INVISIBLE);

            }
        }).start();



    }




    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}

package com.example.jeffrey.myproject;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ImageButton imageButton;
    private Bitmap bitmap;
    private boolean id=false;
    private com.amazonaws.services.rekognition.model.Image getAmazonRekognitionImage=new com.amazonaws.services.rekognition.model.Image();
    private  DetectLabelsResult labelsResult;
    public static Uri uri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageButton=findViewById(R.id.image_to_use);
        requstPermision();




    }

    private void requstPermision() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{(Manifest.permission.READ_EXTERNAL_STORAGE),Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        0);


            }
        } else {
            if(getSharedPreferences("uri",MODE_PRIVATE).getBoolean("exist",false))
            {
                uri = Uri.parse(getSharedPreferences("uri",MODE_PRIVATE).getString("location","f"));


                startTheNextActivity();
            }



        }
    }

    private void startTheNextActivity() {
        startActivity(new Intent(this,Selfi.class));
        finish();
    }

    private void startFaceRecognition() {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        new Thread(new Runnable() {
            @Override
            public void run() {


                
                setTheImage();
                detectLabel();
                checkConfidenceForIdAndFace();


                if ( id) {
                    galleryAddPic();
                    popToast("תמונה תקינה");
                    saveTheUri();
                    changeTheProgressBarAndImage(true);
                    try {

                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    startTheNextActivity();

                } else {
                    popToast("יש לבחור תמונה תקינה של תעדות הזהות");
                    changeTheProgressBarAndImage(false);
                }


            }
        }).start();
    }

    private void popToast(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,s , Toast.LENGTH_LONG).show();

            }
        });

    }

    private void changeTheProgressBarAndImage(final boolean ok) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView imageView=findViewById(R.id.ok_not_ok_image);
                ProgressBar progressBar=findViewById(R.id.progressBar);

                imageView.setVisibility(View.VISIBLE);

                progressBar.setVisibility(View.INVISIBLE);

                if(ok)
                    imageView.setImageDrawable(getDrawable(R.drawable.ic_check_black_24dp));
                else
                    imageView.setImageDrawable(getDrawable(R.drawable.ic_error_outline_black_24dp));
            }
        });

        
    }

    private void checkConfidenceForIdAndFace() {
        try
        {

            for (int i = 0; i < labelsResult.getLabels().size(); i++) {
                if (labelsResult.getLabels().get(i).getName().contains("Id Cards")||labelsResult.getLabels().get(i).getName().contains("Driving License") && labelsResult.getLabels().get(i).getConfidence() > 85)
                    id = true;
            }
        }catch (Exception e)
        {

        }

    }

    private void detectLabel() {
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                "us-east-1:0aa1be33-7099-4b66-b392-3e2a3309a59b", // Identity pool ID
                Regions.US_EAST_1 // Region
        );


        AmazonRekognitionClient amazonRekognitionClient = new AmazonRekognitionClient(credentialsProvider);
        DetectLabelsRequest labelsRequest = new DetectLabelsRequest()
                .withImage(getAmazonRekognitionImage);
        labelsResult= amazonRekognitionClient.detectLabels(labelsRequest);
        labelsResult.getLabels();



    }

    public static Bitmap getResizedBitmap(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = 1000;
            height = (int) (width / bitmapRatio);
        } else {
            height = 1000;
            width = (int) (height * bitmapRatio);

        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    private void setTheImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/6, bitmap.getHeight()/6, false);
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);

        ByteBuffer imageBytes = ByteBuffer.wrap(stream.toByteArray());
        getAmazonRekognitionImage.withBytes(imageBytes);
    }

    public void onClick(View view)
    {

        cameraIntent();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK&&requestCode==0)
        {






            try {



                 bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                 if(bitmap.getRowBytes() * bitmap.getHeight()>5000000)
                bitmap=getResizedBitmap(bitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageButton.setImageBitmap(bitmap);


                    startFaceRecognition();
                }
            });

        }
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
                uri = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(takePictureIntent, 0);
            }
        }
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


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(uri.getPath());
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void saveTheUri() {
        SharedPreferences sharedPreferences=getSharedPreferences("uri",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("location",uri.toString());
        editor.putBoolean("exist",true);
        editor.apply();
    }
}

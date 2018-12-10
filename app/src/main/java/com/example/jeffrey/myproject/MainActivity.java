package com.example.jeffrey.myproject;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.AlphabeticIndex;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.LabelDetection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;

import static android.provider.MediaStore.ACTION_IMAGE_CAPTURE;

public class MainActivity extends AppCompatActivity {

    private ImageButton imageButton;

    private Bitmap bitmap;
    private boolean face=false,id=false;


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

    private void startFaceREconization(){
        new Thread(new Runnable() {
            @Override
            public void run() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ProgressBar progressBar=findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
                setTheImage();
                detectLabel();


               try
               {
                   for (int i = 0; i < labelsResult.getLabels().size(); i++) {
                       if (labelsResult.getLabels().get(i).getName().contains("Id Cards") && labelsResult.getLabels().get(i).getConfidence() > 90)
                           id = true;
                       else if (labelsResult.getLabels().get(i).getName().contains("Human") && labelsResult.getLabels().get(i).getConfidence() > 90)
                           face = true;

                   }
               }catch (Exception e)
               {

               }


               runOnUiThread(new Runnable() {
                   @Override
                   public void run() {
                       if(face&&id)
                       {
                           Toast.makeText(MainActivity.this,"תמונה תקינה",Toast.LENGTH_LONG).show();
                           saveTheUri();
                           new Thread(new Runnable() {
                               @Override
                               public void run() {
                                   try {
                                       runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               ImageView imageView=findViewById(R.id.ok_not_ok_image);
                                               imageView.setVisibility(View.VISIBLE);
                                               ProgressBar progressBar=findViewById(R.id.progressBar);
                                               imageView.setImageDrawable(getDrawable(R.drawable.ic_check_black_24dp));
                                               progressBar.setVisibility(View.INVISIBLE);
                                           }
                                       });

                                       Thread.sleep(2000);
                                   } catch (InterruptedException e) {
                                       e.printStackTrace();
                                   }
                                   startTheNextActivity();
                               }
                           }).start();

                       }else
                       {
                           Toast.makeText(MainActivity.this,"יש לבחור תמונה תקינה של תעדות הזהות",Toast.LENGTH_LONG).show();
                           ImageView imageView=findViewById(R.id.ok_not_ok_image);
                           imageView.setVisibility(View.VISIBLE);
                           imageView.setImageDrawable(getDrawable(R.drawable.ic_error_outline_black_24dp));
                           ProgressBar progressBar=findViewById(R.id.progressBar);
                           progressBar.setVisibility(View.INVISIBLE);
                       }

                   }
               });




            }
        }).start();
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


    private void setTheImage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth()/6, bitmap.getHeight()/6, false);
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);

        ByteBuffer imageBytes = ByteBuffer.wrap(stream.toByteArray());
        getAmazonRekognitionImage.withBytes(imageBytes);
    }

    public void onClick(View view)
    {


        Intent  pickImage = new Intent(Intent.ACTION_PICK);
        pickImage.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        pickImage.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        pickImage.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickImage.setType("image/*");


        try
        {

            startActivityForResult(pickImage, 0);

        }catch (Exception e)
        {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK&&requestCode==0)
        {
            uri=data.getData();




            try {

                 bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());

            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    imageButton.setImageBitmap(bitmap);


                    startFaceREconization();
                }
            });

        }
    }

    private void saveTheUri() {
        SharedPreferences sharedPreferences=getSharedPreferences("uri",MODE_PRIVATE);
        SharedPreferences.Editor editor=sharedPreferences.edit();
        editor.putString("location",uri.toString());
        editor.putBoolean("exist",true);
        editor.apply();
    }
}

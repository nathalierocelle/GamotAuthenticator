package com.example.gamotauthenticator;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.gamotauthenticator.ml.Inception;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CameraPage extends AppCompatActivity {

    Button feedbackButton;
    Button camerabutton;
    TextView result, confidence, disclaimer;
    ImageView imageView;
    int imageSize = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_page);

        feedbackButton = (Button) findViewById(R.id.feedbackbutton);
        camerabutton = (Button) findViewById(R.id.camerabutton);
        result = findViewById(R.id.result);
        confidence = findViewById(R.id.confidence);
        disclaimer = findViewById(R.id.disclaimer);
        imageView = findViewById(R.id.imageView);

        feedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRatingPage();
            }
        });

        camerabutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch camera if we have permission
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 1);
                } else {
                    //Request camera permission if we don't have it.
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

    }

    @SuppressLint("DefaultLocale")
    public void openRatingPage(){
        Intent intent = new Intent(this, RatingPage.class);
        startActivity(intent);
    }

    public void classifyImage(Bitmap image){
        try {
            Inception model = Inception.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int [] intValues = new int[imageSize*imageSize];
            image.getPixels(intValues,0, image.getWidth(), 0,0, image.getWidth(), image.getHeight());
            int pixel = 0;
            for(int i=0; i<imageSize; i++){
                for(int j=0; j<imageSize; j++){
                    int val = intValues[pixel++];
                    byteBuffer.putFloat((val >> 16) & 0xFF) ;
                    byteBuffer.putFloat((val >> 8) & 0xFF);
                    byteBuffer.putFloat(val & 0xFF);
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Inception.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            // Releases model resources if no longer used.

            float[] confidences = outputFeature0.getFloatArray();
            int maxPos = 0;
            float maxConfidence = 0;

            for(int i=0; i<confidences.length; i++){
                if(confidences[i] > maxConfidence){
                    maxConfidence = confidences[i];
                    maxPos = i;
//                    if(maxPos%2 != 0){
//                        maxConfidence = 1-confidences[i];
//                    } else {
//                        maxConfidence = confidences[i];
//                    }
                }
            }
//            String[] classes = {"bioflu_real","bioflu_fake","biogesic_real","biogesic_fake","neozep_real","neozep_fake"};
 //           result.setText(classes[maxPos]);
//            String s = "";
//            for(int i=0; i<classes.length; i++){
//                s += String.format("%s: %.1f%%\n", classes[i], confidences[i]*100);
//            }
            String authentic_disclaimer_message = "Ang kalidad ng produkto ay garantisado";
            String fake_disclaimer_message = "Hindi namin masigurado ang kalidad ng produktong ito. Nasa inyo ang pag papasya. " +
                    "Maaring kumunsulta sa eksperto o pinakamalapit na sentrong pangkalusugan (Health Center).";
            String s = String.format("%.1f%%",  maxConfidence*100);
            String message = "";
            if(maxPos%2!=0){
                message = fake_disclaimer_message;
            } else {
                message = authentic_disclaimer_message;
            }
            //confidence.setText(s);
            disclaimer.setText(message);

            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            Bitmap image = (Bitmap) data.getExtras().get("data");
            int dimension = Math.min(image.getWidth(), image.getHeight());
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

            imageView.setImageBitmap(image);
            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
            classifyImage(image);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
package com.example.cre;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 200;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 300;

    private ImageView imageView;
    private Bitmap capturedPhoto;
    private TextView extractedTextView;

    private static final String API_KEY = "AIzaSyBOYyXYt_9_X--zSfzhBb6a2S3bCRMuBvI";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        extractedTextView = findViewById(R.id.extractedTextView);


        // Request camera permission if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Camera permission is already granted, capture the photo
            capturePhoto();
        }
    }

    private void capturePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
    }

    public void onCapturePhotoClick(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_CAPTURE_REQUEST_CODE);
    }

    public void onConvertToPdfClick(View view) {
        if (capturedPhoto != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String fileName = "captured_photo.pdf";
                savePhotoAsPdf(capturedPhoto, fileName);
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                } else {
                    String fileName = "captured_photo.pdf";
                    savePhotoAsPdf(capturedPhoto, fileName);
                }
            }
        } else {
            Toast.makeText(this, "No receipt entered", Toast.LENGTH_SHORT).show();
        }
    }


    private void savePhotoAsPdf(Bitmap photo, String fileName) {
        try {
            // Create a new PDF document
            PdfDocument document = new PdfDocument();

            // Create a page with the photo as an image
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(photo.getWidth(), photo.getHeight(), 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(photo, 0, 0, null);
            document.finishPage(page);

            // Save the PDF document to the app's external files directory
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            document.writeTo(outputStream);
            document.close();

            Toast.makeText(this, "Photo saved as PDF: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save photo as PDF", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                capturedPhoto = (Bitmap) extras.get("data");
                imageView.setImageBitmap(capturedPhoto);
                
                performGoogleVisionAPICall(capturedPhoto);
            }
        }
    }

    private void performGoogleVisionAPICall(Bitmap photo) {
        // Configure the API request
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();
        Frame frame = new Frame.Builder().setBitmap(photo).build();

        // Perform the API call
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(frame);

        // Process the API response
        StringBuilder extractedText = new StringBuilder();
        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.valueAt(i);
            extractedText.append(textBlock.getValue());
            extractedText.append("\n");
        }

        extractedTextView.setMovementMethod(new ScrollingMovementMethod());
        // Update the TextView with the extracted text
        extractedTextView.setText(extractedText.toString());

        // Show a toast message with the extracted text
        Toast.makeText(this, "완료", Toast.LENGTH_SHORT).show();
    }

    public void onOpenPdfButtonClick(View view) {
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/captured_photo.pdf";
        openPdfFile(filePath);
    }

    private void openPdfFile(String filePath) {
        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(this, "com.example.cre.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/captured_photo.pdf";
                savePhotoAsPdf(capturedPhoto, filePath);
            } else {
                Toast.makeText(this, "파일 권한 거부", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "카메라 권한 부여", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "카메라 권한 거부", Toast.LENGTH_SHORT).show();
            }
        }

    }
}

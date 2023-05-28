package com.example.cre;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 200;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 300;

    private static final int READ_MEDIA_IMAGES_PERMISSION_REQUEST_CODE = 400;


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

    public void onSaveToGalleryClick(View view) {
        if (capturedPhoto != null) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
            Bitmap photo = bitmapDrawable.getBitmap();
            savePhotoToGallery(photo);
        } else {
            Toast.makeText(this, "No photo captured", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePhotoToGallery(Bitmap photo) {
        String imageFileName = "captured_photo.jpg";

        OutputStream fos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM);

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            try {
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                if (fos != null) {
                    photo.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.close();
                    Toast.makeText(this, "영수증이 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            File imageFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), imageFileName);
            try {
                fos = new FileOutputStream(imageFile);
                photo.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();

                // Update gallery
                MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);

                Toast.makeText(this, "Photo saved to gallery", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to save photo to gallery", Toast.LENGTH_SHORT).show();
            }
        }
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

        if (requestCode == READ_MEDIA_IMAGES_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get the selected image URI
            Uri imageUri = data.getData();

            // Do something with the selected image URI, such as displaying it in an ImageView
            imageView.setImageURI(imageUri);
        }
    }

    private void performGoogleVisionAPICall(Bitmap photo) {
        // Configure the text recognizer
        KoreanTextRecognizerOptions options = new KoreanTextRecognizerOptions.Builder().build();
        TextRecognizer recognizer = TextRecognition.getClient(options);

        // Create an ML Kit InputImage from the Bitmap
        InputImage image = InputImage.fromBitmap(photo, 0);

        // Process the image using the text recognizer
        recognizer.process(image)
                .addOnSuccessListener(text -> {
                    // Process the text recognition result
                    StringBuilder extractedText = new StringBuilder();
                    for (Text.TextBlock textBlock : text.getTextBlocks()) {
                        extractedText.append(textBlock.getText());
                        extractedText.append("\n");
                    }

                    extractedTextView.setMovementMethod(new ScrollingMovementMethod());
                    extractedTextView.setText(extractedText.toString());

                    Toast.makeText(this, "완료", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Handle any errors
                    Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }



    public void onViewPhotosClick(View view) {
        // Check if the READ_EXTERNAL_STORAGE permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_MEDIA_IMAGES_PERMISSION_REQUEST_CODE);
        } else {
            // Permission granted, show the photos
            openImageFolder();
        }
    }

    private void openImageFolder() {
        Uri imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Intent intent = new Intent(Intent.ACTION_VIEW, imageUri);
        intent.setDataAndType(imageUri, "image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No file manager app found", Toast.LENGTH_SHORT).show();
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

package com.example.cre;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import android.text.TextUtils;

public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int CAMERA_CAPTURE_REQUEST_CODE = 200;
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 300;

    private static final int READ_MEDIA_IMAGES_PERMISSION_REQUEST_CODE = 400;

    private ImageView imageView;
    private Bitmap capturedPhoto;
    private Bitmap originalPhoto;

    private TextView extractedTextView;

    private static final String API_KEY = "AIzaSyBOYyXYt_9_X--zSfzhBb6a2S3bCRMuBvI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        extractedTextView = findViewById(R.id.extractedTextView);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
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
        if (originalPhoto != null) {
            savePhotoToGallery(originalPhoto);
        } else {
            Toast.makeText(this, "촬영된 영수증이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePhotoToGallery(Bitmap photo) {
        String baseFileName = "receipt";
        String imageFileName = baseFileName + "_" + System.currentTimeMillis() + ".jpg";

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

                MediaScannerConnection.scanFile(this, new String[]{imageFile.getAbsolutePath()}, null, null);

                Toast.makeText(this, "갤러리에 영수증 사진이 보관되었습니다.", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onConvertToPdfClick(View view) {
        String extractedText = extractedTextView.getText().toString();
        if (!TextUtils.isEmpty(extractedText)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String fileName = "receipt";
                saveTextAsPdf(extractedText, fileName);
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                } else {
                    String fileName = "receipt";
                    saveTextAsPdf(extractedText, fileName);
                }
            }
        } else {
            Toast.makeText(this, "입력된 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private int pdfFileCounter;

    private void saveTextAsPdf(String text, String baseFileName) {
        String fileName = baseFileName + pdfFileCounter + ".pdf";
        pdfFileCounter++;

        try {
            PdfDocument document = new PdfDocument();

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(600, 800, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(12f);
            float x = 10f;
            float y = 25f;
            for (String line : text.split("\n")) {
                canvas.drawText(line, x, y, paint);
                y += paint.descent() - paint.ascent();
            }
            document.finishPage(page);

            File file = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
            FileOutputStream outputStream = new FileOutputStream(file);
            document.writeTo(outputStream);
            document.close();

            Toast.makeText(this, "PDF가 저장되었습니다.: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF 저장에 실패하였습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_CAPTURE_REQUEST_CODE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                capturedPhoto = (Bitmap) extras.get("data");
                originalPhoto = capturedPhoto.copy(capturedPhoto.getConfig(), true);
                imageView.setImageBitmap(originalPhoto);

                performGoogleVisionAPICall(capturedPhoto);
            }
        }

        if (requestCode == READ_MEDIA_IMAGES_PERMISSION_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            imageView.setImageURI(imageUri);
        }
    }

    private void performGoogleVisionAPICall(Bitmap photo) {
        KoreanTextRecognizerOptions options = new KoreanTextRecognizerOptions.Builder().build();
        TextRecognizer recognizer = TextRecognition.getClient(options);

        InputImage image = InputImage.fromBitmap(photo, 0);

        recognizer.process(image)
                .addOnSuccessListener(text -> {
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
                    Toast.makeText(this, "이미지를 불러오는데, 실패하였습니다 : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void onDeleteButtonClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }

    public void onViewPhotosClick(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    READ_MEDIA_IMAGES_PERMISSION_REQUEST_CODE);
        } else {
            openPdfFile();
        }
    }

    private void openPdfFile() {
        File pdfDirectory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);

        if (pdfDirectory != null && pdfDirectory.exists()) {
            File[] files = pdfDirectory.listFiles();

            if (files != null && files.length > 0) {
                String[] fileNames = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    fileNames[i] = files[i].getName();
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select a PDF file")
                        .setItems(fileNames, (dialog, which) -> {
                            File selectedFile = files[which];

                            Uri fileUri = FileProvider.getUriForFile(this,
                                    BuildConfig.APPLICATION_ID + ".fileprovider",
                                    selectedFile);

                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(fileUri, "application/pdf");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            if (intent.resolveActivity(getPackageManager()) != null) {
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "pdf파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
            } else {
                Toast.makeText(this, "pdf파일을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "pdf 저장소가 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String fileName = "extracted_text.pdf";
                saveTextAsPdf(extractedTextView.getText().toString(), fileName);
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
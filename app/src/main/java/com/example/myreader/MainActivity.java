package com.example.myreader;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity{
    private ActivityResultLauncher<Intent> filePickerLauncher;
    ImageView myView;
    ParcelFileDescriptor pfd;
    //FileDescriptor fileDescriptor;
    PdfRenderer renderer;
    //PdfRenderer.Page page;
    Bitmap bitmap;
    //Context context;
    private PdfRenderer.Page currentPage;
    int currentPageNum = 0;
    int totalPages = 0;
    Canvas canvas;
    Paint paint = new Paint();
    Button btnOpenFile;
    Button btnNext;
    Button btnPrev;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);//R.layout.activity_main
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */

        myView = findViewById(R.id.pdfView);

        btnOpenFile = findViewById(R.id.btnOpenFile);
        btnOpenFile.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                openFilePicker();
            }
        });

        btnNext = findViewById(R.id.nextBtn);
        btnNext.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(currentPageNum+1<totalPages){
                    currentPageNum++;
                    try{
                        showPage(currentPageNum);
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        btnPrev = findViewById(R.id.prevBtn);
        btnPrev.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(currentPageNum>0){
                    currentPageNum--;
                    try{
                        showPage(currentPageNum);
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        filePickerLauncher = registerForActivityResult(//was before btnOpenFile.setOnClickListener
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    //final int takeFlags = intent.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION);// Check for the freshest data.

                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                        Uri uri = result.getData().getData();
                        //getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        handleFileUri(uri);
                    }
                }
        );
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);//Intent.ACTION_GET_CONTENT
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");//"application/pdf" selects PDFs only //*/*

        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDF"));
    }

    private void handleFileUri(Uri uri){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        try{
            pfd = getContentResolver().openFileDescriptor(uri, "r");
            renderer = new PdfRenderer(pfd);
        }catch(Exception e){
            e.printStackTrace();
        }

        showPage(currentPageNum);
        btnOpenFile.setVisibility(View.GONE);//View.VISIBLE
    }

    private void showPage(int num){
        if(currentPage!=null){
            currentPage.close();
        }
        currentPage = renderer.openPage(num);
        totalPages = renderer.getPageCount();
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        myView.setImageBitmap(bitmap);
        currentPageNum = num;
    }

    @Override
    protected void onStop(){
        super.onStop();
        try{
            closeMyReader();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void closeMyReader() throws IOException{
        if(currentPage!=null){
            currentPage.close();
        }
        if(renderer!=null){
            renderer.close();
        }
        if(pfd!=null){
            pfd.close();
        }
    }
}
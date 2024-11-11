package com.example.myreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener{
    private GestureDetector gestureDetector;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private SubsamplingScaleImageView myView;//ImageView //ZoomImageView
    private Uri uri;
    private ParcelFileDescriptor pfd;
    private PdfRenderer renderer;
    private PdfRenderer.Page currentPage;
    private int currentPageNum = 0;
    private int totalPages = 0;
    private Button btnOpenFile;
    private Button btnNext;
    private Button btnPrev;
    private float scale = 1f;
    private float threshold = 1f;
    private PointF center = new PointF(0,0);//PointF

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        */

        gestureDetector = new GestureDetector(this, this);

        myView = findViewById(R.id.pdfView);
        myView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return myView.onTouchEvent(event);
        });
        myView.setOnStateChangedListener(new SubsamplingScaleImageView.OnStateChangedListener(){
            @Override
            public void onScaleChanged(float newScale, int origin){
                if(newScale>threshold){
                    showPage(currentPageNum, newScale);
                }
            }
            @Override
            public void onCenterChanged(PointF newCenter, int origin){
                //handle panning
            }
        });

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
                        showPage(currentPageNum,1);//scale
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
                        showPage(currentPageNum,1);//scale
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK && result.getData() != null){
                        uri = result.getData().getData();
                        handleFileUri(uri);
                    }
                }
        );
    }

    //GestureDetector.OnGestureListener methods
    @Override
    public boolean onDown(MotionEvent e){
        return false;
    }
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
        final int SWIPE_THRESHOLD = 100;
        final int SWIPE_VELOCITY_THRESHOLD = 100;

        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();

        if(Math.abs(diffX) > Math.abs(diffY)){
            if(Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD){
                if(diffX > 0 && uri!=null){
                    //Swipe Right //prev page
                    if(currentPageNum>0){
                        currentPageNum--;
                        showPage(currentPageNum,1f);//scale
                    }
                }else{
                    //Swipe Left //next page
                    if(currentPageNum+1<totalPages){
                        currentPageNum++;
                        showPage(currentPageNum,1f);//scale
                    }
                }
                return true;
            }
        }
        return false;
    }
    @Override
    public void onShowPress(MotionEvent e){
    }
    @Override
    public boolean onSingleTapUp(MotionEvent e){
        return false;
    }
    @Override
    public void onLongPress(MotionEvent e){
    }
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
        return false;
    }


    private void openFilePicker(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");//"application/pdf" selects PDFs only //*/*
        filePickerLauncher.launch(Intent.createChooser(intent, "Select PDF"));
    }

    private void handleFileUri(Uri uri){
        try{
            pfd = getContentResolver().openFileDescriptor(uri, "r");
            //getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            renderer = new PdfRenderer(pfd);
        }catch(Exception e){
            e.printStackTrace();
        }
        showPage(currentPageNum, 1);//scale
        btnOpenFile.setVisibility(View.GONE);//View.VISIBLE
    }

    private void showPage(int num, float scale){
        if(currentPage!=null){
            currentPage.close();
        }
        currentPage = renderer.openPage(num);
        totalPages = renderer.getPageCount();
        int pageWidth = currentPage.getWidth();//width in points
        int pageHeight = currentPage.getHeight();//height in points
        int density = getResources().getDisplayMetrics().densityDpi;//dots per inch //420
        int bitmapWidth = density / 72 * pageWidth;//dpi / 72 * pageWidth //612w 792h medium //540w 666h
        int bitmapHeight = density / 72 * pageHeight;
        Bitmap bitmap = Bitmap.createBitmap(Math.round(bitmapWidth * scale), Math.round(bitmapHeight * scale), Bitmap.Config.ARGB_8888);//3240 3996
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        myView.setImage(ImageSource.bitmap(bitmap));
        currentPageNum = num;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putInt("currentPageNum", currentPageNum);
        if(uri!=null){
            outState.putParcelable("uri", uri);
        }
        if(myView!=null){
            outState.putFloat("scale", scale);//myView.getScale()
            outState.putParcelable("center", center);//myView.getCenter()
        }
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState){
        super.onRestoreInstanceState(savedInstanceState);
        currentPageNum = savedInstanceState.getInt("currentPageNum", 0);
        uri = savedInstanceState.getParcelable("uri");
        scale = savedInstanceState.getFloat("scale", 1.0f);
        center = savedInstanceState.getParcelable("center");
        if(myView!=null){
            myView.setScaleAndCenter(scale, center);
        }
        if(uri!=null){
            handleFileUri(uri);
        }
    }

    @Override
    protected void onDestroy(){//onStop
        super.onDestroy();
        try{
            if(currentPage!=null){
                currentPage.close();
            }
            if(renderer!=null){
                renderer.close();
            }
            if(pfd!=null){
                pfd.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    @Override
    protected void onStart(){
        super.onStart();
        if(uri!=null){
            handleFileUri(uri);
        }
    }
}

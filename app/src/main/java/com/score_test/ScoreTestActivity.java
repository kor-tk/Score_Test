package com.score_test;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.objdetect.Objdetect.CASCADE_SCALE_IMAGE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ScoreTestActivity extends Activity {

    private static final String TAG = "악보화면";

    private int frame_count = 0;
    private boolean frame_count_flag = false;
    private Point Position = new Point(0, 0);
    private Point Center = new Point(0, 0);
    private Point Center1 = new Point(0, 0);
    private Point Center2 = new Point(0, 0);
    private int PDFPAGE = 0;

    public Point moving_pos = new Point(384, 640);   // Default Screen Size

    private Button l_button;
    private Button r_button;
    private Button turning_Button;
    public ImageView sheet_music;

    RelativeLayout relativelayout;
    RelativeLayout lowerBar;
    private int turning_count = 1;
    private int pdfPage = 0;  // 시작은 인덱스 0
    private int pdfPage_Count = 0;  // 0으로 초기화   -> 밑에서 값 넣어줄것이다
    private ArrayList<Bitmap> pdfBitmap;
    private Point Window_Size;


    private boolean sheet_music_page_turner = false;
    private int sheet_music_page_turner_count = 0;

    private boolean turning_Button_color_check = false;

    boolean isService = false; // 서비스 중인 확인용

    String targetPdf = "";   // 현재 열린 pdf 파일 이름

    // onCreate() 함수는 최초에 한번만 호출된다.   -> 서비스에서 가장 먼저 호출됨.
    // 이 부분은 recreate() 함수 호출하기 전에 미리 pdf Page 저장해두는 부분
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current game state
        savedInstanceState.putInt("PDFPAGE", pdfPage);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Always call the superclass so it can restore the view hierarchy
        super.onRestoreInstanceState(savedInstanceState);

        // Restore state members from saved instance
        PDFPAGE = savedInstanceState.getInt("PDFPAGE");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_test);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            PDFPAGE = savedInstanceState.getInt("PDFPAGE");
        }
        //mRunThread = new RunThread();

        //  ** 작업 중
        Intent pdfIntent = getIntent();   // SelectActivity에서 넘겨준 pdf 경로값을 받아온다.
        String pdfData = pdfIntent.getStringExtra("pdfValue");   // data에 전달
        targetPdf = pdfData;
        Log.i(TAG, "PDF 이름 *** : " + targetPdf);    // 잘 넘어온다.

        File file = new File(targetPdf);
        // targetPdf 는 pdf 파일 이름. 이것을 음.... 가져와야될 것 같은데 SelectActivity에서

        // **

        l_button = (Button) findViewById(R.id.l_button);
        r_button = (Button) findViewById(R.id.r_button);
        turning_Button = (Button) findViewById(R.id.turning_Button);
        //logo = (TextView) findViewById(R.id.logo);
        relativelayout = (RelativeLayout) findViewById(R.id.relativelayout);
        lowerBar = (RelativeLayout) findViewById(R.id.lowerBar);
        sheet_music = (ImageView) findViewById(R.id.sheet_music);

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        Window_Size = new Point();
        Window_Size.x = dm.widthPixels;
        Window_Size.y = dm.heightPixels;
        //Log.i("윈도우 사이즈*****", ""+Window_Size);

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/dx경필명조Bold.ttf");
        turning_Button.setTypeface(typeFace);

        //sheet_music.setImageResource(R.drawable.summer_1);   // imgView 지정

        l_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //sheet_music.setImageResource(R.drawable.summer_1);
                if (pdfPage + 1 > 1)
                    pdfPage--;
                sheet_music.setImageBitmap(pdfBitmap.get(pdfPage));

            }
        });

        r_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //sheet_music.setImageResource(R.drawable.summer_2);
                if (pdfPage_Count > pdfPage + 1)
                    pdfPage++;
                sheet_music.setImageBitmap(pdfBitmap.get(pdfPage));
            }
        });

        turning_Button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                turning_count++;
                if (turning_count % 2 == 0) {
                    //mOpenCvCameraView.setAlpha(1.0f);
                    //sheet_music.setVisibility(View.INVISIBLE);
                } else {
                    //mOpenCvCameraView.setAlpha(0.8f);
                    //sheet_music.setVisibility(View.VISIBLE);
                }
            }
        });

        String path1 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Face_template/face_template.jpg";   // TODO: 현재 템플릿 가져온 상태.

        Bitmap bp1 = BitmapFactory.decodeFile(path1);
        if (bp1.getHeight() == 0) {
            Toast.makeText(ScoreTestActivity.this, "템플릿 불러오기 실패.. 템플릿을 생성해주세요", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            Log.i(TAG, "템플릿 불러오기 성공***");
        }

        //  **  수정중
        pdfBitmap = pdfToBitmap(file);  // pdfBitmap에 Array형식으로 악보가 bitmap 한장 한장 들어감
        sheet_music.setImageBitmap(pdfBitmap.get(pdfPage));
        //  **
    }

    private ArrayList<Bitmap> pdfToBitmap(File pdfFile) {
        ArrayList<Bitmap> bitmaps = new ArrayList<>();
        try {
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY));

            //Bitmap bitmap;
            final int pageCount = renderer.getPageCount();
            pdfPage_Count = pageCount;   // 여기서 pdfPage_Count에 값을 넣어준다.  다른 곳에서 다 쓸 수 있게
            for (int i = 0; i < pageCount; i++) {
                //Bitmap bitmap;

                PdfRenderer.Page page = renderer.openPage(i);

                int width = getResources().getDisplayMetrics().densityDpi / 72 * page.getWidth();
                int height = getResources().getDisplayMetrics().densityDpi / 72 * page.getHeight();
                Log.i(TAG, "width=" + page.getWidth() + "  height=" + page.getHeight()+"\nwidth="+width+"   height="+height);


                if(page.getWidth() > 800) {
                    width = 595;
                    height = 841;
                }
                else {
                    width = page.getWidth();
                    height = page.getHeight();
                }

                Bitmap bitmap = null;
                bitmap = Bitmap.createScaledBitmap(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), 1536, 2048, true);
                /*
                    뭔가 이 윗줄 코드로 하니까 pdf파일은 다 열 수 있게됐다.
                    Bitmap.createBitmap(숫자, 숫자, ㅇㅇㅇ), 숫자, 숫자, treu) 여기서 '숫자' 파트가 중요한것같은데 이건
                    모든 기기에도 가능하게 나중에 수정해보자.
                 */
                //bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);  // **원래 코드
                //bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                bitmaps.add(bitmap);
                bitmap = null;

                // close the page
                page.close();
            }
            // close the renderer
            renderer.close();
        } catch (Exception ex) {
            Log.e(TAG, "pdfTobitmap:", ex);
            ex.printStackTrace();
        }
        return bitmaps;
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause 동작");
        pdfBitmap.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() 호출");
        System.gc();
    }

    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart() 호출");
    }

    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop() 호출");
    }

    public void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart() 호출");
    }

    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "onCameraViewStarted() 호출");
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped() 호출");
    }

    class RunThread extends Thread {    //TODO: Hands-Free 하단부 색깔 지정.
        @Override
        public void run() {

            Log.i(TAG, "Thread 동작중");

            if(turning_Button_color_check == false) {
                (ScoreTestActivity.this).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {

                        turning_Button.setTextColor(0xffffffff);   // 하얀색
                    }
                });
            }
            else {   // true 일 때
                (ScoreTestActivity.this).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {

                        turning_Button.setTextColor(0xFFFF4081);  // 분홍색
                    }
                });
            }

            if (frame_count_flag == true) {  // 프레임 개수 세는 부분
                (ScoreTestActivity.this).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        //face_detect_surface_view.this.recreate();
                        Log.i(TAG, "DisableView 호출***");
                        //ScoreTestActivity.this.mOpenCvCameraView.disableView();

                        Log.i(TAG, "EnableView 호출***");
                        //ScoreTestActivity.this.mOpenCvCameraView.enableView();
                    }
                });
                frame_count_flag = false;
            }

        }
    }
}
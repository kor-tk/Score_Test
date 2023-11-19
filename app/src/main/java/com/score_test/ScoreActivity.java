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

public class ScoreActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "악보화면";
    // private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;

    // private Mat templateR;
    // private Mat templateL;

    // private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private File mCascadeFileEye;
    private CascadeClassifier mJavaDetector;
    private CascadeClassifier mJavaDetectorEye;

    private String[] mDetectorName;

    private CameraBridgeViewBase mOpenCvCameraView;

    double xCenter = -1;
    double yCenter = -1;

    private int frame_count = 0;
    private boolean frame_count_flag = false;
    private Point Position = new Point(0, 0);
    private Point Center = new Point(0, 0);
    private Point Center1 = new Point(0, 0);
    private Point Center2 = new Point(0, 0);
    private RunThread mRunThread;
    private int PDFPAGE = 0;

    // Point default_pos = new Point(0, 0);   // Middle of Eye area
    // Point pre_pos = new Point(0, 0);      // Present of Eye position
    public Point moving_pos = new Point(384, 640);   // Default Screen Size

    private Button l_button;
    private Button r_button;
    private Button turning_Button;
    public ImageView sheet_music;
    // public Boolean frame_flag = false;

    RelativeLayout relativelayout;
    RelativeLayout lowerBar;
    private int turning_count = 1;
    private int pdfPage = 0;  // 시작은 인덱스 0
    private int pdfPage_Count = 0;  // 0으로 초기화   -> 밑에서 값 넣어줄것이다
    private ArrayList<Bitmap> pdfBitmap;
    private Point Window_Size;
    private Point matchLoc_Position;

    private int Success_Count = 0;
    private int Frame_Count = 0;

    private boolean sheet_music_page_turner = false;
    private int sheet_music_page_turner_count = 0;

    private boolean turning_Button_color_check = false;

    boolean isService = false; // 서비스 중인 확인용

    String targetPdf = "";   // 현재 열린 pdf 파일 이름

    private Mat left_template;
    private Mat right_template;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // load cascade file from application resources
                        InputStream ise = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
                        File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFileEye = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");
                        FileOutputStream ose = new FileOutputStream(mCascadeFileEye);

                        while ((bytesRead = ise.read(buffer)) != -1) {
                            ose.write(buffer, 0, bytesRead);
                        }
                        ise.close();
                        ose.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        mJavaDetectorEye = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier for eye");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileEye.getAbsolutePath());

                        cascadeDir.delete();
                        cascadeDirEye.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.setCameraIndex(1);  // 1은 전면 카메라, 0은 후면 카메라
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public ScoreActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
    }

    // TODO: 2023-01-17 현재 C++을 사용하지 않기때문에 주석처리함
    // 이상하게 opencv_java3 -> opencv_java4 로 바꾸니 에러 안뜸
    static {
        System.loadLibrary("opencv_java4");
    }
    
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
        setContentView(R.layout.activity_score);
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

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.score_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setAlpha(0.8f);   // 카메라 투명도 조절  왜  카메라는 되는데 안보이냐 ㅋㅋㅋㅋㅋㅋ   // TODO: 이거 주석처리하면 전면 카메라 영상 보인다
        // 만약에 레이아웃이면  aaaa.getBackground().setAlpha(0~100); 이정도    1.0f 하니까 딱 악보만 잘 보이네 ㅎㅎ

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
                    mOpenCvCameraView.setAlpha(1.0f);
                    //sheet_music.setVisibility(View.INVISIBLE);
                } else {
                    mOpenCvCameraView.setAlpha(0.8f);
                    //sheet_music.setVisibility(View.VISIBLE);
                }
            }
        });

        //this.runOnUiThread(updateUI);
        //this.onRestart();
        //this.recreate();

        //dataPassing(frame_flag);

        //left_template = new Mat(R.drawable.left_template);
        // TODO: 이 부분은 얼굴 템플릿 사진 가져오는 부분.
        //String filepath1 = "C:\\Users\\KimTY\\Desktop\\얼굴 템플릿\\left_template.PNG";
        //String filepath2 = "C:\\Users\\KimTY\\Desktop\\얼굴 템플릿\\right_template.PNG";
        //Bitmap bitmap_temp = BitmapFactory.decodeResource(getResources(), R.drawable.left_template);
        //Bitmap bitmap_temp = BitmapFactory.decodeFile(filepath1);
        //Utils.bitmapToMat(bitmap_temp, left_template);          // 젠장 도대체 어떻게 해야  내가 찍은 사진 파일 불러올 수 있을까. Mat 파일로
        //left_template = Imgcodecs.imread(filepath1);
        //right_template = Imgcodecs.imread(filepath2);

        String path1 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Face_template/face_template.jpg";   // TODO: 현재 템플릿 가져온 상태.
        //String path2 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Face_template/r_t.png";

        Bitmap bp1 = BitmapFactory.decodeFile(path1);
        if (bp1.getHeight() == 0) {
            Toast.makeText(ScoreActivity.this, "템플릿 불러오기 실패.. 템플릿을 생성해주세요", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            Log.i(TAG, "템플릿 불러오기 성공***");
        }
        left_template = new Mat(bp1.getWidth(), bp1.getHeight(), CvType.CV_8U);
        //left_template = new Mat(bp1.getHeight(), bp1.getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(bp1, left_template);
        Imgproc.resize(left_template, left_template, new Size(bp1.getWidth()*0.5, bp1.getHeight()*0.5));
        //TODO:Imgproc.resize() 이 부분으로 원본 사이즈를 조절해서 사용하자!
        Imgproc.cvtColor(left_template, left_template, COLOR_BGR2GRAY);


        /*
        Bitmap bp2 = BitmapFactory.decodeFile(path2);
        right_template = new Mat(bp2.getWidth(), bp2.getHeight(), CvType.CV_8U);
        Utils.bitmapToMat(bp2, right_template);
        Imgproc.cvtColor(right_template, right_template, COLOR_BGR2GRAY);
        */

        //left_template = bytesToMat(bitmapToByteArray(bp));
        //File imgFile = new  File(filepath1);

        /*
        if(imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            left_template = convertBitMap_to_Mat(myBitmap);
            Log.i("비트맵 이미지 생성", "완료");
        }
        else
            Log.i("비트맵 이미지 생성", "실패");
        //left_template = convertBitMap_to_Mat(myBitmap);
        */


        //  **  수정중
        pdfBitmap = pdfToBitmap(file);  // pdfBitmap에 Array형식으로 악보가 bitmap 한장 한장 들어감
        //sheet_music.setImageBitmap(pdfBitmap.get(pdfPage));  // 얘가 원본
        sheet_music.setImageBitmap(pdfBitmap.get(pdfPage));  // 얘가 수정한거
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


                //BitmapFactory.Options options = new BitmapFactory.Options();
                //options.inSampleSize = 4;
                //options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                //options.inMutable = true;

                if(page.getWidth() > 800) {
                    width = 595;
                    height = 841;
                }
                else {
                    width = page.getWidth();
                    height = page.getHeight();
                }

                Bitmap bitmap = null;
                //Bitmap bitmap = Bitmap.createBitmap(1536, 2048, Bitmap.Config.ARGB_8888);
                //bitmap = Bitmap.createScaledBitmap(Bitmap.createBitmap(2380, 3368, Bitmap.Config.ARGB_8888), 1536, 2048, true);  // 1536, 2048 이었다.
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
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (mGray != null)
            mGray.release();
        if (left_template != null)
            left_template.release();
        pdfBitmap.clear();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);  // TODO: OPENCV_VERSION 으로 변경
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() 호출");
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
            mOpenCvCameraView = null;  // 추가
        }
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
        mGray = new Mat();
        //mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "onCameraViewStopped() 호출");
        mGray.release();
        //left_template.release();
        //mRgba.release();
        //mZoomWindow.release();
        //mZoomWindow2.release();
    }

    public Point templateMatching(Mat inFile, Mat templateFile1, int match_method, Point compare_center, Point tl, Point br) {  // TODO: templateMatching()함수.
        // Mat templateFile2 뺐다

        Mat img = inFile.clone();
        Mat templ = templateFile1.clone();
        //Mat templ2 = templateFile2;

        // / Create the result matrix
        int result_cols = img.cols() - templ.cols() + 1;
        //Log.e(TAG,"result_cols : "+result_cols+"\nimg_cols : "+img.cols()+"\ntempl_cols : "+templ.cols());
        int result_rows = img.rows() - templ.rows() + 1;
        //Log.e(TAG,"result_rows : "+result_cols+"\nimg_rows : "+img.rows()+"\ntempl_rows : "+templ.rows());
        Mat result = new Mat(result_cols, result_rows, CvType.CV_8U);
        //Mat result2 = new Mat(img.cols()-templ2.cols()+1, img.rows()-templ2.rows()+1, CvType.CV_8U);
        //Log.e(TAG,"final output mat rows : "+result.rows()+"\nfinal output mat cols : "+result.cols());
        // / Do the Matching and Normalize

        Imgproc.matchTemplate(img, templ, result, match_method);
        //Imgproc.matchTemplate(img, templ2, result2, match_method);

        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());
        //Core.normalize(result2, result2, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        // / Localizing the best match with minMaxLoc
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);
        //Core.MinMaxLocResult mmr2 = Core.minMaxLoc(result2);

        Point matchLoc;
        //Point matchLoc2;
        if (match_method == Imgproc.TM_SQDIFF || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
            //matchLoc2 = mmr2.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
            //matchLoc2 = mmr2.maxLoc;
        }

        //Log.i("matchLoc=", ""+matchLoc);
        //Log.i("템플릿매칭", "매칭성공");

        Log.i(TAG, "$$$$결과$$$$:\nFace compare_center:" + compare_center + ",\nmatchLoc:" + matchLoc);

        Frame_Count++;
        if (tl.x >= matchLoc.x || tl.y >= matchLoc.y || br.x <= matchLoc.x || br.y <= matchLoc.y) {
            //Imgproc.rectangle(mGray, matchLoc, new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows()), new Scalar(255, 255, 255));  //255 255 255가 흰색
            turning_Button_color_check = false;
            Log.i(TAG, "turning_check : false");

            if (img != null)   // 원래 if문 없앴었다
                img.release();
            if (templ != null)
                templ.release();

            return new Point(-1, -1);
        } else {
            turning_Button_color_check = true;
            Imgproc.rectangle(mGray, matchLoc, new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows()), new Scalar(255, 255, 255));  //255 255 255가 흰색
            Success_Count++;
            Log.i(TAG, "Success_Count=" + Success_Count + ",\nFrame_Count="+Frame_Count);
            Log.i(TAG, "turning_check : true");
            if (img != null)
                img.release();
            if (templ != null)
                templ.release();

            return matchLoc;
        }

        /*
        Log.i(TAG, "$$$$결과$$$$:\nFace compare_center:" + compare_center + ",\nmatchLoc:" + matchLoc);
        if (one.x >= matchLoc.x || one.y >= matchLoc.y || two.x <= matchLoc.x || two.y <= matchLoc.y) {
            //Imgproc.rectangle(mGray, matchLoc, new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows()), new Scalar(255, 255, 255));  //255 255 255가 흰색
            turning_Button_color_check = false;
            Log.i(TAG, "turning_check : false");

            if (img != null)   // 원래 if문 없앴었다
                img.release();
            if (templ != null)
                templ.release();

            return new Point(-1, -1);
        } else {
            turning_Button_color_check = true;
            Imgproc.rectangle(mGray, matchLoc, new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows()), new Scalar(255, 255, 255));  //255 255 255가 흰색
            Log.i(TAG, "turning_check : true");
            if (img != null)
                img.release();
            if (templ != null)
                templ.release();

            return matchLoc;
        }
        */
        //if(compare_center.x <= matchLoc.x || compare_center.y <= matchLoc.y)    // ㅋㅋㅋㅋ 이제는 얼굴 범위로 지정해야겠는데? ㅋㅋㅋㅋㅋㅋ 젠장
        //    return;

        // / Show me what you got
        //Imgproc.rectangle(img, matchLoc, new Point(matchLoc.x+templ.cols(), matchLoc.y+templ.rows()), new Scalar(255, 255, 255));  //255 255 255가 흰색
        //Imgproc.rectangle(img, matchLoc2, new Point(matchLoc2.x+templ2.cols(), matchLoc2.y+templ2.rows()), new Scalar(255, 255, 0));
        //return matchLoc;
    }

    // TODO: onCameraFrame() 함수.
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if (frame_count > 130) {
            frame_count_flag = true;
            frame_count = 0;
        }
        Log.i(TAG, "*****FRMAE_COUNT=" + frame_count);

        //mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Core.flip(mGray, mGray, 0);   // PortraitView 사용할 때만


        //if(frame_flag == true)
        //    Log.i("frame_flag 값은:", "true");
        //else
        //    Log.i("frame_flag 값은:", "false");

        //Log.i("left_template.rows=", "'"+left_template.rows()+"'"+"cols="+left_template.cols()+"'");

        /*
        if (mAbsoluteFaceSize == 0)
        {
            int height = mGray.rows();   // mGray.rows() = 1280
            if (Math.round(height * mRelativeFaceSize) > 0)  // 256f   round() 반올림 함수
            {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }
        */

        //if (mZoomWindow2 == null)    //****이거 주석처리해도 되긴 되네?
        //    CreateAuxiliaryMats();

        MatOfRect faces = new MatOfRect();
        /*
        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)                                   // 2
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 0|CASCADE_SCALE_IMAGE, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
        }
        */


        // Objdetect.CASCADE_FIND_BIGGEST_OBJECT | CASCADE_SCALE_IMAGE  은 바꾼거
        mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 0 | CASCADE_SCALE_IMAGE, // TODO: 얼굴 인식 부분.
                new Size(256, 256), new Size());  // 256  256

        Rect[] facesArray = faces.toArray();   // facesArray는 1명 인식되면 length가 1이 된다.  아니면 0

        Point compare_center = new Point(0, 0); // 템플릿 매칭 범위 조절 변수
        Point compare_center1 = new Point(0, 0);
        Point compare_center2 = new Point(0, 0);
        Point tl = new Point(0, 0);
        Point br = new Point(0, 0);

        if (facesArray.length != 0) {
            compare_center = new Point((facesArray[0].x + facesArray[0].width + facesArray[0].x) / 2,
                    (facesArray[0].y + facesArray[0].y + facesArray[0].height) / 2);
            Center.x = compare_center.x;
            Center.y = compare_center.y;
        }
        Imgproc.circle(mGray, Center, 10, new Scalar(255, 0, 0, 255), 3);

        //if(facesArray.length == 0)
        //    frame_flag = false;    // 인식 안됐을 때

        for (int i = 0; i < facesArray.length; i++) {
            //                              tl(),  br()
            //Imgproc.rectangle(mGray, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 2);   // TODO: 얼굴 윤곽 사각형 인식 부분.

            xCenter = (facesArray[i].x + facesArray[i].width + facesArray[i].x) / 2;
            yCenter = (facesArray[i].y + facesArray[i].y + facesArray[i].height) / 2;
            Point center = new Point(xCenter, yCenter);

            tl = new Point(facesArray[i].tl().x, facesArray[i].tl().y+(facesArray[i].br().y - facesArray[i].tl().y)*0.68);
            br = new Point(facesArray[i].br().x, facesArray[i].br().y);

            //TODO: 입술 범위 : tl, br 썼다.

            double d_tl_x = tl.x + (br.x - tl.x)*0.15;
            double d_br_x = br.x - (br.x - tl.x)*0.15;

            tl.x = d_tl_x;
            br.x = d_br_x;

            //Imgproc.rectangle(mGray, tl, br, new Scalar(255, 0, 0, 5), 2);

            //Log.i(TAG, "Face center: " + center.x + ", " + center.y);

            //Imgproc.circle(mGray, center, 10, new Scalar(255, 0, 0, 255), 3);


            //Imgproc.putText(mGray, "x:" + center.x + ",  y:" + center.y + "]",
            //        new Point(center.x + 20, center.y + 20),
            //        Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(255, 255, 255, 255));
            //Imgproc.putText(mGray, "x:" + center.x + ",  y:" + center.y + "]",
            //        new Point(100, 100),
            //        Core.FONT_HERSHEY_SIMPLEX, 0.7, new Scalar(0, 0, 0, 0));


            Rect r = facesArray[i];
            // compute the eye area
            //Rect eyearea = new Rect(r.x + r.width / 8,
            //        (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
            //        (int) (r.height / 3.0));
            // split it

            Rect eyearea_left = new Rect(r.x + r.width / 16,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            Rect eyearea_right = new Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (int) (r.y + (r.height / 4.5)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
            // draw the area - mGray is working grayscale mat, if you want to
            // see area in rgb preview, change mGray to mRgba
            // Rect의 tl()은 오른쪽 위 Point반환,   br()은 좌측 아래 Point반환
            //Imgproc.rectangle(mGray, eyearea_left.tl(), eyearea_left.br(),       // TODO: 여기가 눈 영역 사각형 그리는 부분
            //        new Scalar(255, 0, 0, 5), 2);
            //Log.i("eyearea_left", "tl()="+eyearea_left.tl()+"\n br()="+eyearea_left.br());

            //compare_center1.x = eyearea_left.tl().x + 17;
            //compare_center1.y = eyearea_left.tl().y + 10;
            //compare_center2.x = eyearea_left.br().x - 10;
            //compare_center2.y = eyearea_left.br().y - 15;

            compare_center1.x = eyearea_right.tl().x + 17;
            compare_center1.y = eyearea_right.tl().y + 10;
            compare_center2.x = eyearea_right.br().x - 50;
            compare_center2.y = eyearea_right.br().y - 15;

            Center1.x = compare_center1.x;
            Center1.y = compare_center1.y;
            Center2.x = compare_center2.x;
            Center2.y = compare_center2.y;

            Imgproc.rectangle(mGray, tl, br, new Scalar(255, 0, 0, 5), 2); // TODO: 입 영역 사각형 그리는 부분

            //Imgproc.rectangle(mGray, eyearea_right.tl(), eyearea_right.br(),
            //        new Scalar(255, 0, 0, 5), 2);

            // 기준점 초기화 ( 눈 영역의 가운데 )
            //default_pos.x = (eyearea_left.tl().x + eyearea_left.br().x) / 2;
            //default_pos.y = (eyearea_left.br().y - eyearea_left.tl().y) / 2;

            //Imgproc.circle(mGray, new Point(eyearea_left.x * 1.2, eyearea_left.y * 1.2), 10, new Scalar(0, 0, 255));  // 어떻게 수치로 바꿔서 계산해볼까

            //***************************************여기다  하던부분
            //if (learn_frames < 500) {
            //templateR = eye_tracking(mJavaDetectorEye, eyearea_left, 24);
            //templateL = eye_tracking(mJavaDetectorEye, eyearea_left, 24);     // 이거 없애면  한쪽눈만  뽑아낸다.
            // TODO: templateL 이거 주석 처리했다.

            //} else {
            // Learning finished, use the new templates for template
            // matching
            //match_eye(eyearea_right, teplateR, method);
            //match_eye(eyearea_left, teplateL);
            //}

            // cut eye areas and put them to zoom windows
            //Imgproc.resize(mRgba.submat(eyearea_left), mZoomWindow2, mZoomWindow2.size());
            //Imgproc.resize(mGray.submat(eyearea_right), mZoomWindow, mZoomWindow.size());
        }


        // TODO: 여기가 원래 시작점!.  이제 여기를 몇 프레임 해서 그것을 몇 초처럼 동작시키게 하자.
        //TODO: 어차피 얼굴 인식중이면 프레임 느려져서 괜찮을 것ㄱ같다.
        if(compare_center.x != 0 && sheet_music_page_turner == false) {  // compare_center는 얼굴 인식이 됐을 때, 악보가 아직 안넘어갔을 때 동작**
            matchLoc_Position = templateMatching(mGray, left_template, Imgproc.TM_CCOEFF_NORMED, compare_center, tl, br);   // TODO: templateMatching()함수가 matchTemplate()있는 함수.

            if(turning_Button_color_check == true && compare_center.x > 430 && compare_center.x < 550) {   // 템플릿 매칭 성공했을때, 얼굴이 가운데 쯤에 있을 때
                //if(tl.x < matchLoc_Position.x && tl.y < matchLoc_Position.y && br.x > matchLoc_Position.x && br.y > matchLoc_Position.y) {   // 우측의 수치는 옆 바라볼 때의 좌표

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            public void run() {

                                if(pdfPage_Count > pdfPage + 1)
                                    pdfPage++;
                                sheet_music.setImageBitmap(pdfBitmap.get(pdfPage));

                                Log.i(TAG, "악보 페이지 넘어감***");
                                sheet_music_page_turner = true;   // 얘가 가장 중요
                            }
                        });
                    }
                }).start();
                // TODO: 여기까지 시작점 끝
                //}
            }   // 여기 까지 큰 if문이 수정 중인 if문

        }
        else if(sheet_music_page_turner == true) {  // 악보가 넘어갔을 때**
            matchLoc_Position = templateMatching(mGray, left_template, Imgproc.TM_CCOEFF_NORMED, compare_center, tl, br);   // TODO: templateMatching()함수가 matchTemplate()있는 함수.

            sheet_music_page_turner_count++;  // 한 30 프레임까지만 세어 보자)
            if(sheet_music_page_turner_count >= 16) {
                sheet_music_page_turner = false;  // false로 교체
                sheet_music_page_turner_count = 0;
            }
        }

        //else {  // 이 else문은 굳이 필요 없는거 아닌가?
        //    //turning_Button_color_check = false;
        //    matchLoc_Position = new Point(-1, -1);   // (-1, -1)은 초기화 값으로 냅두자
        //    //turning_Button.setBackgroundColor(0xFFDEEBF7);  // 파랑
        //}

        mRunThread = new RunThread();
        mRunThread.start();

        frame_count++;  // 프레임 카운터로 다스려보자.,

        return mGray;
    }

    class RunThread extends Thread {    //TODO: Hands-Free 하단부 색깔 지정.
        @Override
        public void run() {
            //Position = templateMatching(mGray, left_template,
            //        Imgproc.TM_CCOEFF_NORMED, Center, Center1, Center2);
            //Position = face_detect_surface_view.this.templateMatching(mGray, left_template,
            //        Imgproc.TM_CCOEFF_NORMED, Center, Center1, Center2);

            //Imgproc.rectangle(mGray, Position, new Point(Position.x + left_template.cols(), Position.y + left_template.rows()), new Scalar(255, 255, 255));

            Log.i(TAG, "Thread 동작중");

            if(turning_Button_color_check == false) {
                (ScoreActivity.this).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {

                        turning_Button.setTextColor(0xffffffff);   // 하얀색
                    }
                });
            }
            else {   // true 일 때
                (ScoreActivity.this).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {

                        turning_Button.setTextColor(0xFFFF4081);  // 분홍색
                    }
                });
            }

            if (frame_count_flag == true) {  // 프레임 개수 세는 부분
                (ScoreActivity.this).runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        //face_detect_surface_view.this.recreate();
                        Log.i(TAG, "DisableView 호출***");
                        ScoreActivity.this.mOpenCvCameraView.disableView();

                        Log.i(TAG, "EnableView 호출***");
                        ScoreActivity.this.mOpenCvCameraView.enableView();
                    }
                });
                frame_count_flag = false;
            }

        }
    }
}
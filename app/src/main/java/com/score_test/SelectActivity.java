package com.score_test;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SelectActivity extends AppCompatActivity {

    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;
    private static final String TAG = "SelectActivity";

    private int permission_camera;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 277;

    Button templButton;
    RadioGroup radioGroup;
    RadioButton radio_btn_off;
    RadioButton radio_btn_on;
    Boolean radio_btn_off_flag;

    ListView sheet_music_list;
    IconTextListAdapter adapter;

    // TODO: 23_11_20 테스트 주석 추가 - 2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        //Intent intent = getIntent();

        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/dx경필명조Bold.ttf");   // asset > fonts 폴더 내의 폰트 적용
        //Typeface typeFace2 = Typeface.createFromAsset(getAssets(), "fonts/NanumSquareR.ttf");

        //Sheet_List = (TextView)findViewById(R.id.sheet_music_title);
        //Sheet_List.setTypeface(typeFace);

        permission_camera = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);  // SelectActivity 만들어졌을때 onCreate()에서 한 번만 우선 세팅되어있는 퍼미션 확인

        sheet_music_list = (ListView) findViewById(R.id.sheet_music_list);
        radio_btn_off = (RadioButton) findViewById(R.id.radio_btn_off);
        radio_btn_on = (RadioButton) findViewById(R.id.radio_btn_on);
        radioGroup = (RadioGroup) findViewById(R.id.radio_group);

        adapter = new IconTextListAdapter(this);

        radio_btn_off.setTypeface(typeFace);
        radio_btn_on.setTypeface(typeFace);

        // 라디오 버튼 flag 초기화
        radio_btn_off_flag = radio_btn_off.isChecked();  // 안드로이드스튜디오가 이렇게 코드 단순화시키라네  와 좋네.

        // 2023_07_02 추가. 라디오버튼 체크에 따라 악보 누르면 퍼미션 체크 후 화면 이동, 악보 화면 이동 등 다르게 설정.
        // 그러므로 flag로 관리하자. off 상태 즉. Hands-Free 기능이 꺼져있을 때가 Default 상태로 함.
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.radio_btn_off) {
                    radio_btn_off_flag = true;
                    Log.i(TAG, "라디오버튼 :: off");
                }
                else if(i == R.id.radio_btn_on) {
                    radio_btn_off_flag = false;
                    Log.i(TAG, "라디오버튼 :: on");
                }
            }
        });

        // TODO:2023-01-17 파일 없으면 여기서 Error. 예외처리 해주자 - 완료
        try {
            // 폴더 경로   **
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sheet_Music";
            Log.i(TAG, path);
            File directory = new File(path);
            File[] files = directory.listFiles();

            final List<String> filesNameList = new ArrayList<>();

            for(int i=0; i<files.length; i++) {
                filesNameList.add(files[i].getName());
                Log.d(TAG, filesNameList.get(i));

            }   // 잘 나오네  get(i) 하니까

            for(int i=0; i<files.length; i++) {
                adapter.addItem(new IconTextItem(AppCompatResources.getDrawable(this, R.drawable.music_note), filesNameList.get(i)));
                // 어댑터 적용
            }

            sheet_music_list.setAdapter(adapter);

            // ListView Item들 클릭했을 때
            sheet_music_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                /**
                 * ListView의 Item을 Click 할 때 수행할 동작
                 * @param parent 클릭이 발생한 AdapterView.
                 * @param view 클릭 한 AdapterView 내의 View(Adapter에 의해 제공되는 View).
                 * @param position 클릭 한 Item의 position
                 * @param id 클릭 된 Item의 Id
                 */
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) { // 각 ListView에 해당하는 position 인덱스

                    /**
                     * 23_05_17 SelectActivity 리스트뷰에서 악보들 클릭하면
                     * 마찬가지로 카메라 퍼미션 뜨게 설정
                     */

                    permission_camera = ContextCompat.checkSelfPermission(SelectActivity.this, Manifest.permission.CAMERA);
                    if(permission_camera == PackageManager.PERMISSION_DENIED) {  // 카메라 퍼미션 거부 상태
                        showDialogForPermission("핸즈프리악보를 사용하시려면 카메라 권한을 허용하셔야됩니다");
                    }
                    else {  // 카메라 퍼미션 허용 상태  -> CameraTestActivity로 넘어간다
                        //filesNameList.get(position)  이렇게 하면 누른 것 pdf 이름이 들어가려나?
                        //Intent sheet_music_Intent = new Intent(SelectActivity.this, ScoreActivity.class);  // 2023-01-18 잠시 테스트를 위해 주석
                        //Intent sheet_music_Intent = new Intent(SelectActivity.this, CameraTestActivity.class); // 2023_07_02 마찬가지로 테스트를 위해 주석
                        Intent sheet_music_Intent = new Intent(SelectActivity.this, ScoreTestActivity.class); // 악보 수정이 가능 한 Activity 테스트중
                        sheet_music_Intent.putExtra("pdfValue", path+"/"+filesNameList.get(position));  // pdf 파일 경로 넣자

                        Log.i(TAG, path+"/"+filesNameList.get(position));

                        SelectActivity.this.startActivity(sheet_music_Intent);
                        // 이제  해당 거 누르면 넘어가게 하자
                    }

                }
            });
        } catch(NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, "Sheet_Music 폴더가 없다");
        }

        templButton = (Button) findViewById(R.id.templCaptureBtn);
        templButton.setTypeface(typeFace);
        templButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 버튼 눌렀을 때 다시한번 카메라 퍼미션 확인
                permission_camera = ContextCompat.checkSelfPermission(SelectActivity.this, Manifest.permission.CAMERA);
                if(permission_camera == PackageManager.PERMISSION_DENIED) {  // 카메라 퍼미션 거부 상태
                    showDialogForPermission("템플릿을 수정하시려면 카메라 권한을 허용하셔야됩니다");
                }
                else {  // 카메라 퍼미션 허용 상태  -> TemplateActivity로 넘어간다
                    Intent templIntent = new Intent(SelectActivity.this, TemplateActivity.class);  // 템플릿 지정하는 화면으로 전환
                    SelectActivity.this.startActivity(templIntent);
                }
                
            }
        });
    }

    @Override
    public void onPause()  // 악보 누르면 호출돼
    {
        super.onPause();
        Log.i(TAG, "onPause() 호출");
    }

    @Override
    public void onResume() {  // 얘가 onCreate() 다음으로 젤 먼저 호출
        super.onResume();
        Log.i(TAG, "onResume() 호출");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() 호출");
    }

    public void onStop() {  // 마지막 호출 (악보 눌렀을 때 악보화면 동작 중에)
        super.onStop();
        Log.i(TAG, "onStop() 호출");
    }

    public void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart() 호출");
    }


    // 뒤로가기 두 번 눌렸을 때 앱 종료 함수
    @Override
    public void onBackPressed() {

        long tempTime = System.currentTimeMillis();
        long intervalTime = tempTime - backPressedTime;

        if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime)
        {
            super.onBackPressed();
            finishAffinity();
        }
        else
        {
            backPressedTime = tempTime;
            showToastMessage("한번 더 뒤로가기를 누르면 앱이 종료됩니다", 1200);
        }
    }

    /** 2023_03_09 Toast메시지를 1초만에 없애는 코드 추가 (Handler, postDelayed()함수 사용)  - 오 되네 좋다
     * https://stackoverflow.com/questions/3775074/set-toast-appear-length/9715422#9715422
     * */
    private void showToastMessage(String msg, long delayMillis) {
        final Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT);
        toast.show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toast.cancel();
            }
        }, delayMillis);  // 1000ms : 1s

       // handler.postDelayed(toast::cancel, 1000);  // 이렇게 한 문장으로 단순화 시킬 수 있다. (람다코드) 잘 모름.
                                                            // 후에 리팩토링 할 때 활용하는 것도 괜찮을듯
    }


    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "퍼미션 체크 완료");
            // 퍼미션 허용 상태 체크 완료되면 TemplateActivity로 넘어간다
            Intent templIntent = new Intent(SelectActivity.this, TemplateActivity.class);  // 템플릿 지정하는 화면으로 전환
            SelectActivity.this.startActivity(templIntent);

        }
        else {
            showToastMessage("취소하셨습니다", 800);  // 거부 눌렀을 때 멘트 등장

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SelectActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {  // 사용자가 이 기능을 사용하겠다고 '예'를 눌렀는데 그거에대한 권한요청을 하는데 Deny때문에 뜨는지 않뜨는지 그런..
                // 사용자가 Don't show again 안눌렀을 때
                if(ActivityCompat.shouldShowRequestPermissionRationale(SelectActivity.this, CAMERA)) {
                    requestPermissions(new String[]{CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                }
                // Don't show again 체크되어있을 때
                else {  // 직접 퍼미션 설정 창 띄워준다.
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", SelectActivity.this.getPackageName(),
                            null);
                    intent.setData(uri);
                    SelectActivity.this.startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                showToastMessage("취소하셨습니다", 800);  // 거부 눌렀을 때 멘트 등장
            }
        });
        builder.create().show();
    }
}
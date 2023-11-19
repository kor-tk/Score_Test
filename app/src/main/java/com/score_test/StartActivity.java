package com.score_test;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

public class StartActivity extends AppCompatActivity {

    private Handler handler;
    private boolean PERMISSON_DENY_CHECK_FLAG = false;
    private int permission;
    private int permission2;
    private static final int EXTERNAL_STORAGE_REQUEST_CODE = 1000;
    private static String TAG = "startActivity";

    // TODO: 23_11_20 테스트 주석 추가 - 1
    // TODO: 23_11_20 테스트 주석 추가 - 2

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // 권한ID를 가져옵니다
        permission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        permission2 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);

        // 권한이 열려있는지 확인
        if (permission == PackageManager.PERMISSION_DENIED || permission2 == PackageManager.PERMISSION_DENIED) {
            // 마쉬멜로우 이상버전부터 권한을 물어본다
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                        EXTERNAL_STORAGE_REQUEST_CODE);
            }
            return;
        }
        else {  // 권한 허용 되어있을 때
            doHandler();
        }
    }

    /** 몇 초 멈췄다가 다음 SelectActivity로 넘어가게하는 함수 */
    public void doHandler() {

        handler = new Handler(Looper.getMainLooper()) {    // Handler 생성자에 Looper.getMainLooper()를 넣어서 메인쓰레드에서 동작하게 시킨다
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Intent selectIntent = new Intent(StartActivity.this, SelectActivity.class);  // 1초 후 로그인 화면으로...
                StartActivity.this.startActivity(selectIntent);
                finish();
                //overridePendingTransition(R.anim.loadfadein, R.anim.loadfadeout);   // TODO: 고치자 제대로. 검색해서
                Log.i(TAG,"finish()함수 동작되네");
            }
        };

        handler.sendEmptyMessageDelayed(0, 1000);
    }
    @Override
    public void onPause()  // 악보 누르면 호출돼
    {
        super.onPause();
        Log.i(TAG, "onPause() 호출");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy() 호출");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() 호출");

        /** 2023_03_08 작업
         * 퍼미션 다시보지않음 체크 후 인텐트 갔다온 상태 + 해당 permission들이 모두 허용상태 일 때 동작 */
        if(PERMISSON_DENY_CHECK_FLAG) {  // PERMISSION_DENY_CHECK_FLAG가 true일 때만 동작
            // 권한들을 다시 확인한다
            permission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            permission2 = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE);

            if(PERMISSON_DENY_CHECK_FLAG && permission != PackageManager.PERMISSION_DENIED && permission2 != PackageManager.PERMISSION_DENIED) {
                Log.i(TAG, "PERMISSION Deny flag 동작 완료");
                doHandler();
            }
        }

    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.i(TAG, "onRestart() 호출");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart() 호출");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop() 호출");
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if(requestCode == EXTERNAL_STORAGE_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "퍼미션 체크 완료");

            /** 2023_03_07
             * Handler로 수 초 후에 SelectActivity 호출되게 설정
             * 이 onRequestPermissionsResult() 함수 안에 넣어야 퍼미션 허용된 상태일 때 다음으로 진행할 수 있게 설정 가능
             * TODO:Deny체크하고 거절했을 때 어떻게 할지 추가요함(완료) */
            doHandler();
        }
        else {
            showDialogForPermission("본 기기의 악보를 불러오려면\n사용 권한이 필요합니다.");  // 퍼미션 거부했을 때 등장 멘트
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission(String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
        builder.setTitle("알림");
        builder.setMessage(msg);
        builder.setCancelable(false);
        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // 사용자가 Don't show again 안눌렀을 때
                if(ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this, READ_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);
                }
                // Don't show again 체크되어있을 때
                else {  // 직접 설정 창 띄워준다.  그리고 flag값을 true로 줘서 이 케이스일 때만 따로 동작하게 만든다. onResume()에서 다음화면으로 던져주려고
                    PERMISSON_DENY_CHECK_FLAG = true;
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", StartActivity.this.getPackageName(),
                            null);
                    intent.setData(uri);
                    StartActivity.this.startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finishAffinity();
            }
        });
        builder.create().show();
    }

}
package com.score_test;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TemplateActivity extends AppCompatActivity implements View.OnClickListener {

    private ImageView imgMain;
    private Button btnCamera, btnAlbum;

    private static final int PICK_FROM_CAMERA = 1;
    private static final int PICK_FROM_ALBUM = 2;
    private static final int CROP_FROM_CAMERA = 3;

    private Uri photoUri;
    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA};
    private static final int MULTIPLE_PERMISSIONS = 101;
    private Typeface typeFace;
    private boolean PERMISSON_DENY_CHECK_FLAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_template);
        typeFace = Typeface.createFromAsset(getAssets(), "fonts/dx경필명조Bold.ttf");   // asset > fonts 폴더 내의 폰트 적용
        checkPermissions();
        initView();
    }

    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>();
        for (String pm : permissions) {
            result = ContextCompat.checkSelfPermission(this, pm);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(pm);
            }
        }
        if (!permissionList.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void initView() {
        imgMain = (ImageView) findViewById(R.id.img_test);
        btnCamera = (Button) findViewById(R.id.btn_camera);
        btnAlbum = (Button) findViewById(R.id.btn_album);

        btnCamera.setOnClickListener(this);
        btnAlbum.setOnClickListener(this);

        btnCamera.setTypeface(typeFace);
        btnAlbum.setTypeface(typeFace);

        File img_temp = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Face_template", "/face_template" + ".jpg");
        if(img_temp.exists()) { // 템플릿 이미지가 있으면 먼저 이미지를 띄워놔준다.
            imgMain.setImageURI(Uri.fromFile(img_temp));
        }

    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Toast.makeText(TemplateActivity.this, "이미지 프로세싱 오류.. 다시 시도해주세요", Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(TemplateActivity.this,
                    "com.score_test.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(intent, PICK_FROM_CAMERA);
        }
    }

    private File createImageFile() throws IOException {
        //final String imageFileName = "face_template";
        File storageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Face_template/");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Face_template", "/face_template" + ".jpg");

        return image;
    }

    private void goToAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_FROM_ALBUM);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_camera) {
            takePhoto();
        }
        else if(v.getId() == R.id.btn_album) {
            goToAlbum();
        }
        /*    // 이게 원래 코드. 뭐 경고 떠서 걍 if문으로 바꿔버림.
        switch (v.getId()) {
            case R.id.btn_camera:
                takePhoto();
                break;
            case R.id.btn_album:
                goToAlbum();
                break;
        }
        */
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(this.permissions[0])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showDialogForPermission();
                            }
                        } else if (permissions[i].equals(this.permissions[1])) {
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                showDialogForPermission();

                            }
                        } else if (permissions[i].equals(this.permissions[2])) { // 카메라 퍼미션
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {

                                // Deny 안눌렀을 때
                                if(ActivityCompat.shouldShowRequestPermissionRationale(TemplateActivity.this, CAMERA)) {
                                    PERMISSON_DENY_CHECK_FLAG = false;
                                    showDialogForPermission();
                                }
                                else {  // Deny 눌렀을 때
                                    PERMISSON_DENY_CHECK_FLAG = true;
                                    showDialogForPermission();
                                }

                            }
                        }
                    }
                } else {
                    showDialogForPermission();
                }
                return;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void showDialogForPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(TemplateActivity.this);
        builder.setTitle("알림");
        builder.setMessage("권한 요청에 동의 해주셔야 이용 가능합니다.\n권한 허용에 동의 해주시길 바랍니다.");
        builder.setCancelable(false);
        builder.setPositiveButton("알겠습니다", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // 사용자가 Don't show again 안눌렀을 때

                // Deny 안누르고 그냥 알겠다 눌렀을 때
                if(!PERMISSON_DENY_CHECK_FLAG) {
                    finish();
                }
                // Don't show again 체크되어있을 때
                else {  // 직접 퍼미션 설정 창을 띄워준다.
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", TemplateActivity.this.getPackageName(),
                            null);
                    intent.setData(uri);
                    TemplateActivity.this.startActivity(intent);
                    finish();
                }

            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            showToastMessage("취소하셨습니다", 900);
            return;
        }
        // PICK_FROM_ALBUM
        if (requestCode == PICK_FROM_ALBUM) {
            if (data == null) {
                return;
            }
            photoUri = data.getData();
            cropImage();
        }
        // PICK_FROM_CAMERA
        else if (requestCode == PICK_FROM_CAMERA) {
            cropImage();

            ////////////////////////////////////////////////////////////////

            // 갤러리에 나타나게
            MediaScannerConnection.scanFile(TemplateActivity.this,
                    new String[]{photoUri.getPath()}, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    });
        }
        // CROP_FROM_CAMERA
        else if (requestCode == CROP_FROM_CAMERA) {
            //TODO: 오!!! 되긴되는데  사진 사이즈를 좀 줄여야겠다.
            try {
                // 비트맵 이미지로 가져온다
                File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Face_template", "/face_template" + ".jpg");

                String imagePath = f.getAbsolutePath();
                Bitmap image = BitmapFactory.decodeFile(imagePath);

                // 이미지를 상황에 맞게 회전시킨다
                ExifInterface exif = new ExifInterface(imagePath);
                int exifOrientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                int exifDegree = exifOrientationToDegrees(exifOrientation);
                image = rotate(image, exifDegree);

                // 변환된 이미지 사용
                //imgMain.setImageBitmap(image);
            } catch (Exception e) {
                Toast.makeText(this, "오류발생: " + e.getLocalizedMessage(),
                        Toast.LENGTH_LONG).show();
            }
            /////////////////////////////////////////////////

            imgMain.setImageURI(null);
            imgMain.setImageURI(photoUri);
            revokeUriPermission(photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        }
    }

    public int exifOrientationToDegrees(int exifOrientation)
    {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90)
        {
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180)
        {
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270)
        {
            return 270;
        }
        return 0;
    }
    public Bitmap rotate(Bitmap bitmap, int degrees)
    {
        if(degrees != 0 && bitmap != null)
        {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);

            try
            {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted)
                {
                    bitmap.recycle();
                    bitmap = converted;
                }
            }
            catch(OutOfMemoryError ex)
            {
                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환합니다.
            }
        }
        return bitmap;
    }

    public void cropImage() {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(photoUri, "image/*");
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 0);
        grantUriPermission(list.get(0).activityInfo.packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        int size = list.size();
        if (size == 0) {
            showToastMessage("취소하셨습니다", 900);
            return;
        } else {
            Toast.makeText(this, "용량이 큰 사진의 경우 시간이 걸릴 수 있습니다", Toast.LENGTH_SHORT).show();
            intent.putExtra("crop", "true");
            //intent.putExtra("aspectX", 1);   // 얘네 주석 해제하면 1:1 비율의 정사각형 모양으로 크롭
            //intent.putExtra("aspectY", 1);   //
            intent.putExtra("scale", true);

            File croppedFileName = null;
            try {
                croppedFileName = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //File folder = new File(Environment.getExternalStorageDirectory() + "/NOSTest/");
            File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Face_template/");
            File tempFile = new File(folder.toString(), croppedFileName.getName());

            photoUri = FileProvider.getUriForFile(TemplateActivity.this,
                    "com.score_test.fileprovider", tempFile);
            intent.putExtra("return-data", false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

            Intent i = new Intent(intent);
            ResolveInfo res = list.get(0);
            grantUriPermission(res.activityInfo.packageName, photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            i.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            startActivityForResult(i, CROP_FROM_CAMERA);

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
}
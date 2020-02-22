package com.example.bo.ocrbaidu;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OCR";

    private static final int REQUEST_CODE_GENERAL = 105;
    private static final int REQUEST_CODE_GENERAL_BASIC = 106;
    private static final int REQUEST_CODE_ACCURATE_BASIC = 107;
    private static final int REQUEST_CODE_ACCURATE = 108;
    private static final int REQUEST_CODE_GENERAL_ENHANCED = 109;
    private static final int REQUEST_CODE_GENERAL_WEBIMAGE = 110;
    private static final int REQUEST_CODE_BANKCARD = 111;
    private static final int REQUEST_CODE_VEHICLE_LICENSE = 120;
    private static final int REQUEST_CODE_DRIVING_LICENSE = 121;
    private static final int REQUEST_CODE_LICENSE_PLATE = 122;
    private static final int REQUEST_CODE_BUSINESS_LICENSE = 123;
    private static final int REQUEST_CODE_RECEIPT = 124;

    private static final int REQUEST_CODE_PASSPORT = 125;
    private static final int REQUEST_CODE_NUMBERS = 126;
    private static final int REQUEST_CODE_QRCODE = 127;
    private static final int REQUEST_CODE_BUSINESSCARD = 128;
    private static final int REQUEST_CODE_HANDWRITING = 129;
    private static final int REQUEST_CODE_LOTTERY = 130;
    private static final int REQUEST_CODE_VATINVOICE = 131;
    private static final int REQUEST_CODE_CUSTOM = 132;

    // activity result requestCode
    private static final int CAMERA = 1;
    private static final int PICTURE = 2;

    // permission
    private static final String[] permissions = new String[] {
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET,
        Manifest.permission.CAMERA
    };
    private List<String> permissionList = new ArrayList<>();

    private Button btnOcr;
    private Button btnPicture;
    private Button btnCamera;
    private ImageView imageView;
    private Uri imageUri;
    private boolean hasGotToken = false;
    String imagePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOcr = findViewById(R.id.ocr);
        btnOcr.setOnClickListener(ocrClickListener);
        btnPicture = findViewById(R.id.picture);
        btnPicture.setOnClickListener(pictureClickListener);
        btnCamera = findViewById(R.id.camera);
        btnCamera.setOnClickListener(cameraClickListener);
        imageView = findViewById(R.id.image);

        // permission
        for(String permission:permissions) {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }
        if(permissionList.size() > 0) {
            requestPermissions(permissions, 0);
        }

        // init token
        initAccessTokenWithAkSk();
    }

    private boolean checkTokenStatus() {
       if(!hasGotToken) {
           Toast.makeText(getApplicationContext(), "token does get yet !", Toast.LENGTH_SHORT).show();
       }
       return hasGotToken;
    }

    /**
     * use ak sk to init
     */
    private void initAccessTokenWithAkSk() {
        OCR.getInstance(this).initAccessTokenWithAkSk(new OnResultListener<AccessToken>() {
            @Override
            public void onResult(AccessToken accessToken) {
               String token = accessToken.getAccessToken();
               hasGotToken = true;
            }

            @Override
            public void onError(OCRError ocrError) {
                ocrError.printStackTrace();
                Log.d(TAG, "onError: get token fail ：" + ocrError.getMessage() );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "ak sk get token fail !", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }, getApplicationContext(), "Y78alE5vrsquyOod2qbSSBhA", "WBLuGeVdbe8vP8YzQUVZi87GtN0oNqbU");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean denied = false;
        switch (requestCode) {
            case 0:
                for (int i = 0; i < grantResults.length; i ++) {
                    if(grantResults[i] == -1) {
                        denied = true;
                    }
                }
                if(denied) {
                    Toast.makeText(MainActivity.this, "permission denied", Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }

    private View.OnClickListener ocrClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!checkTokenStatus()) {
                return;
            }
            Log.d(TAG, "ocrClickListener: ");
            recGeneral(imagePath);

        }
    };

    private View.OnClickListener pictureClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!checkTokenStatus()) {
                return;
            }
            Intent intent = new Intent("android.intent.action.GET_CONTENT");
            intent.setType("image/*");
            startActivityForResult(intent, PICTURE);
        }
    };

    private View.OnClickListener cameraClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!checkTokenStatus()) {
                return;
            }

            File image = new File(getExternalCacheDir(), "image.jpg");
            Log.d(TAG, "onClick: cameraClickListener :" + getExternalCacheDir());
            try {
                if (image.exists()) {
                    Log.d(TAG, "onClick: image exist, delete");
                    image.delete();
                }
                image.createNewFile();
                imagePath = image.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (Build.VERSION.SDK_INT >= 24) {
                imageUri = FileProvider.getUriForFile(MainActivity.this, "com.example.bo.ocrbaidu.provider", image);
            }
            else {
                imageUri = Uri.fromFile(image);
            }

            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, CAMERA);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case CAMERA:
                if(resultCode != RESULT_OK) {
                    Log.e(TAG, "onActivityResult result not ok");
                    return;
                }
                Log.d(TAG, "onActivityResult: glide set image");
                Glide.with(this).load(imageUri).into(imageView);
                break;
            case PICTURE:
                if(resultCode == RESULT_OK) {
                    if(Build.VERSION.SDK_INT >= 19) {
                        handleImageOnKitKat(data);
                    }
                    else {
                        handleImageBeforeKitKat(data);
                    }
                }

                break;
            default:
                break;

        }
    }

    private void recGeneral(String filePath) {
        File image = new File(filePath);
        if (!image.exists()) {
            Log.e(TAG, "image does not exist" + image.getAbsolutePath());
            return;
        }
        final StringBuilder sb = new StringBuilder();
        // 通用文字识别参数设置
        GeneralBasicParams params = new GeneralBasicParams();
        params.setDetectDirection(true);
        params.setLanguageType(GeneralBasicParams.CHINESE_ENGLISH);
        params.setDetectLanguage(true);
        //params.setImageFile(new File(image.getAbsolutePath()));
        params.setImageFile(image);

        // 调用通用文字识别服务
        OCR.getInstance(this).recognizeGeneralBasic(params, new OnResultListener<GeneralResult>() {
            @Override
            public void onResult(GeneralResult generalResult) {
                // 调用成功，返回GeneralResult对象
                for (WordSimple wordSimple : generalResult.getWordList()) {
                   // wordSimple不包含位置信息
                    WordSimple word = wordSimple;
                    sb.append( word.getWords());
                    sb.append("\n");
                }
                // json格式返回字符串
                // listener.onResult(result.getJsonRes());
                Log.d(TAG, "onResult: " + sb.toString());
                Intent intent = new Intent(MainActivity.this, OcrActivity.class);
                intent.putExtra("ocr_result", sb.toString());

            }

            @Override
            public void onError(OCRError ocrError) {
                // 调用失败，返回OCRError对象
                Log.d(TAG, "onError: ocr fail : " + ocrError.getMessage());
                Toast.makeText(MainActivity.this, "ocr fail !", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void handleImageOnKitKat(Intent data) {
        Uri uri = data.getData();
        Log.d(TAG, "handleImageOnKitKat: uri : " + uri );
        if(DocumentsContract.isDocumentUri(this, uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            }
            else if("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(contentUri, null);
            }
        }
        else if("content".equalsIgnoreCase(uri.getScheme())) {
            imagePath = getImagePath(uri, null);
        }
        else if("file".equalsIgnoreCase(uri.getScheme())) {
            imagePath = uri.getPath();
        }

        displayImage(imagePath);
    }

    private void handleImageBeforeKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = getImagePath(uri, null);
        displayImage(imagePath);
    }

    private String getImagePath(Uri uri, String selection) {
        String path = null;

        Cursor cursor = getContentResolver().query(uri,null, selection, null, null);
        if(null != cursor) {
            if(((Cursor) cursor).moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    private void displayImage(final String imagePath) {
//        if(imagePath != null) {
//            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
//            Log.d(TAG, "displayImage: org bitmap w = " + bitmap.getWidth() + " h = " + bitmap.getHeight());
//            /* rescaling */
//            Bitmap bmp = ocrService.rescaling(bitmap, 400, 400);
//            /* binarisation */
//            bmp = ocrService.binarisation(bmp,115);
//            /* removeNoise */
//            bmp = ocrService.removeNoise(bmp);
//
//            mBitmap = bmp;
//            Log.d(TAG, "displayImage: resize bitmap w = " + bmp.getWidth() + " h = " + bmp.getHeight());
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
//            byte[] bytes=baos.toByteArray();
//            Glide.with(this).load(bytes).into(imageView);
//
//        }
//        else {
//            Toast.makeText(this, "failed to get image", Toast.LENGTH_SHORT).show();
//        }


        Log.d(TAG, "displayImage: imagePath : " + imagePath);
        Glide.with(this).load(imagePath).into(imageView);
    }

}

package com.example.bo.ocrbaidu;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.AccessToken;
import com.bumptech.glide.Glide;
import com.example.bo.ocrbaidu.listener.OcrListener;
import com.example.bo.ocrbaidu.service.OcrService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OCR";
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

    private DrawerLayout drawerLayout;
    private ImageView imageView;
    private Uri imageUri;
    private boolean hasGotToken = false;
    String m_imagePath = null;

    private OcrService m_ocrService;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            m_ocrService = ((OcrService.OcrBinder) service).getService();
            // set listene
//            m_ocrService.setOcrListener(new OcrListener() {
//                @Override
//                public void onOcrResult(String result) {
//                    Log.d(TAG, "onOcrResult: " + result);
//                    Intent intent = new Intent(MainActivity.this, OcrActivity.class);
//                    intent.putExtra("ocr_result", result);
//                    startActivity(intent);
//                }
//            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private LocalReceiver localReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.image);
        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.cat_footprint_24px);
        }
        //drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.nav_1);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                drawerLayout.closeDrawers();
                return true;
            }
        });
        // floatingActionButton
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.floating_action);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeCamera();
            }
        });
        // permission
        requestPermission();
        // init token
        initAccessTokenWithAkSk();
        // start service
//        Intent intent = new Intent(this, OcrService.class);
//        startService(intent);
        Intent bindIntent = new Intent(this, OcrService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

        //register broadcast
        IntentFilter intentFilter;
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.bo.broadcast.OCR_SUCCESS");
        localReceiver = new LocalReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.album:
                openAlbum();
                break;
            case R.id.ocr:
                if (!checkTokenStatus()) {
                    return false;
                }
                Log.d(TAG, "ocrClickListener: ");
                Intent intent1 = new Intent(MainActivity.this, OcrActivity.class);
                startActivity(intent1);
                m_ocrService.recGeneral(m_imagePath);
                break;
            default:
                break;
        }
        return true;
    }

    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");
        }
    }

    /**
     *  request permission
     */
    private void requestPermission() {
        for(String permission:permissions) {
            if(checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permission);
            }
        }
        if(permissionList.size() > 0) {
            requestPermissions(permissions, 0);
        }
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

    private boolean checkTokenStatus() {
       if(!hasGotToken) {
           Toast.makeText(getApplicationContext(), "token does get yet !", Toast.LENGTH_SHORT).show();
       }
       return hasGotToken;
    }

    private void openAlbum() {
        if (!checkTokenStatus()) {
            return;
        }
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent, PICTURE);
    }

    private void takeCamera(){
        if (!checkTokenStatus()) {
            return;
        }
        File image = new File(getExternalCacheDir(), "image.jpg");
        Log.d(TAG, "onClick: cameraClickListener :" + getExternalCacheDir());
        try {
            if (image.exists()) {
                image.delete();
            }
            image.createNewFile();
            m_imagePath = image.getAbsolutePath();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case CAMERA:
                if(resultCode != RESULT_OK) {
                    Log.e(TAG, "onActivityResult result not ok");
                    return;
                }
                Log.d(TAG, "imageUri : " + imageUri);
//                Glide.with(this).load(imageUri).into(imageView);
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    imageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
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

    private void handleImageOnKitKat(Intent data) {
        Uri uri = data.getData();
        String imagePath = null;

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
        m_imagePath = imagePath;
    }

}

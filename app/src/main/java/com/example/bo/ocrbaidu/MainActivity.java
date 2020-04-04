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
import android.graphics.Path;
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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.bo.ocrbaidu.listener.OcrListener;
import com.example.bo.ocrbaidu.service.OcrService;
import com.example.bo.ocrbaidu.util.FileUtil;

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
    private static final int CROP = 3;
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
    private Uri m_imageUri;
    private Uri m_cropUri;
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
            actionBar.setHomeAsUpIndicator(R.drawable.cat_caregivers_24px);
        }
        //drawer layout
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        navView.setCheckedItem(R.id.files);
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
            case R.id.crop:
                photoCrop(m_imageUri);
                break;
            case R.id.album:
                openAlbum();
                break;
            case R.id.ocr:
                if (!checkTokenStatus()) {
                    return false;
                }
                if(null == m_imagePath) {
                    Toast.makeText(getApplicationContext(), "No picture loaded", Toast.LENGTH_SHORT).show();
                    return false;
                }
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICTURE);
    }

    private void takeCamera(){
        if (!checkTokenStatus()) {
            return;
        }
        File image = new File(getExternalCacheDir(), "image.jpg");
        try {
            if (image.exists()) {
                image.delete();
            }
            image.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        m_imageUri = FileUtil.convertToUri(image);

        Log.d(TAG, "takeCamera uri: " + m_imageUri);

        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        intent.putExtra(MediaStore.EXTRA_OUTPUT, m_imageUri);
        startActivityForResult(intent, CAMERA);
    }

    // TODO: move to util
    private void photoCrop(Uri uri) {
        File file = new File(getExternalCacheDir(), "crop.jpg");
        try {
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*** output uri必须使用Uri.fromFile(file),用FileProvider.getUriForFile
         * 会导致“无法保存经过剪裁的图片问题”，why?
         ***/
        m_cropUri = Uri.fromFile(file);
        Log.d(TAG, "photoCrop path:" + file.getAbsolutePath());
        Log.d(TAG, "photoCrop uri:" + m_cropUri);

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setDataAndType(uri, "image/*");
        // crop = true开启intent中设置显示的view可剪裁
        intent.putExtra("crop", "true");
        intent.putExtra("scale", "true");
        // 如果打开下面代码，截取的是原型区域
        // 高宽的比例
//        intent.putExtra("aspectX", 1);
//        intent.putExtra("aspectY", 1);
//        //建材图片高宽
//        intent.putExtra("outputX", 150);
//        intent.putExtra("outputY", 300);
        intent.putExtra("return-data", false);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, m_cropUri);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, m_cropUri);
        startActivityForResult(intent, CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode != RESULT_OK) {
            Log.e(TAG, "onActivityResult result not ok");
            return;
        }
        switch (requestCode) {
            case CAMERA:
                Log.d(TAG, "camera image url : " + m_imageUri);
                Glide.with(this)
                    .load(m_imageUri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageView);
                break;
            case PICTURE:
                m_imageUri = data.getData();
                Log.d(TAG, "album image url : " + m_imageUri);
                Glide.with(this)
                    .load(m_imageUri)
                    .into(imageView);
                break;
            case CROP:
                Log.d(TAG, "cropped image uri : " + m_cropUri);
                Glide.with(this)
                    .load(m_cropUri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(imageView);
                m_imageUri = m_cropUri;
                break;
            default:
                break;
        }

        m_imagePath = FileUtil.getImagePath(m_imageUri);
        Log.d(TAG, "m_imagePath : " + m_imagePath);
    }


}

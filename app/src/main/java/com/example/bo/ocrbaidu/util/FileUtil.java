package com.example.bo.ocrbaidu.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.example.bo.ocrbaidu.MainActivity;
import com.example.bo.ocrbaidu.MyApplication;

import java.io.File;

public class FileUtil {
    private static final String TAG = "OCR";

    public static String getImagePath(Uri uri) {
        String path = null;

        if (Build.VERSION.SDK_INT >= 19) {
            path = handleImageOnKitKat(uri);
        }
        else {
            path = handleImageBeforeKitKat(uri);
        }
        return path;
    }
    private static String handleImageOnKitKat(Uri uri) {
        String path = null;

        Log.d(TAG, "handleImageOnKitKat: uri : " + uri );
        if(DocumentsContract.isDocumentUri(MyApplication.getInstance(), uri)) {
            String docId = DocumentsContract.getDocumentId(uri);
            if("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];
                String selection = MediaStore.Images.Media._ID + "=" + id;
                path = uri2path(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            }
            else if("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                path = uri2path(contentUri, null);
            }
        }
        else if ("com.example.bo.ocrbaidu.provider".equals(uri.getAuthority())) {
            Log.d(TAG, "get uri auth: " + uri.getAuthority());
            File file = new File(MyApplication.getInstance().getExternalCacheDir(),
                    "image.jpg");
            path = file.getAbsolutePath();
        }
        else if("content".equalsIgnoreCase(uri.getScheme())) {
            path = uri2path(uri, null);
        }
        else if("file".equalsIgnoreCase(uri.getScheme())) {
            path = uri.getPath();
        }
        return path;
    }

    private static String handleImageBeforeKitKat(Uri uri) {
        String path = uri2path(uri, null);
        return path;
    }

    private static String uri2path(Uri uri, String selection) {
        String path = null;

        Cursor cursor = MyApplication.getInstance().getContentResolver().query
                (uri,null, selection, null,
                null);
        if(null != cursor) {
            if(((Cursor) cursor).moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    public static Uri convertToUri(File file) {
        Uri tmp;
        if (Build.VERSION.SDK_INT >= 24) {
            tmp = FileProvider.getUriForFile(MyApplication.getInstance(),
                    "com.example.bo.ocrbaidu.provider", file);
        }
        else {
            tmp = Uri.fromFile(file);
        }
        return  tmp;
    }


}

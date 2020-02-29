package com.example.bo.ocrbaidu.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.baidu.ocr.sdk.OCR;
import com.baidu.ocr.sdk.OnResultListener;
import com.baidu.ocr.sdk.exception.OCRError;
import com.baidu.ocr.sdk.model.GeneralBasicParams;
import com.baidu.ocr.sdk.model.GeneralResult;
import com.baidu.ocr.sdk.model.WordSimple;
import com.example.bo.ocrbaidu.MainActivity;
import com.example.bo.ocrbaidu.OcrActivity;
import com.example.bo.ocrbaidu.listener.OcrListener;

import java.io.File;

public class OcrService extends Service {
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

    private static final String TAG = "OCR";
    OcrBinder m_ocrBinder = new OcrBinder();
    OcrListener m_ocrListener;
    private LocalBroadcastManager localBroadcastManager;

    public OcrService() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    public class OcrBinder extends Binder {
        public OcrService getService() {
            return  OcrService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return m_ocrBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void setOcrListener(OcrListener listener) {
        m_ocrListener = listener;
    }

    public void recGeneral(String filePath) {
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
//                m_ocrListener.onOcrResult(sb.toString());
                Intent intent = new Intent("com.bo.broadcast.OCR_SUCCESS");
                intent.putExtra("ocrResult", sb.toString());
                localBroadcastManager.sendBroadcast(intent);
            }

            @Override
            public void onError(OCRError ocrError) {
                // 调用失败，返回OCRError对象
                Log.d(TAG, "onError: ocr fail : " + ocrError.getMessage());
//                m_ocrListener.onOcrResult(ocrError.getMessage());
                Intent intent = new Intent("com.bo.broadcast.OCR_SUCCESS");
                intent.putExtra("ocrResult", ocrError.getMessage());
                localBroadcastManager.sendBroadcast(intent);
            }
        });
    }

}

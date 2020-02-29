package com.example.bo.ocrbaidu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class OcrActivity extends AppCompatActivity {

    private static final String TAG = "OCR";
    private TextView ocrText;
    private ProgressBar progressBar;
    LocalReciever localReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        ocrText = findViewById(R.id.ocr_text);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        //register broadcast
        IntentFilter intentFilter;
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.bo.broadcast.OCR_SUCCESS");
        localReceiver = new LocalReciever();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter);
    }

    class LocalReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getStringExtra("ocrResult"));
            progressBar.setVisibility(View.GONE);
            ocrText.setText(intent.getStringExtra("ocrResult"));



        }
    }


}

package com.example.bo.ocrbaidu;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class OcrActivity extends AppCompatActivity implements SaveDialogFragment.SaveDialogListener{

    private static final String TAG = "OCR";
    private TextView ocrText;
    private ProgressBar progressBar;
    LocalReciever localReceiver;
    private StringBuilder m_ocrResult = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        ocrText = findViewById(R.id.ocr_text);
        progressBar = findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);
        // toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //register broadcast
        IntentFilter intentFilter;
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.bo.broadcast.OCR_SUCCESS");
        localReceiver = new LocalReciever();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, intentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ocr_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.save:
            SaveDialogFragment saveDialogFragment = new SaveDialogFragment();
            saveDialogFragment.show(getSupportFragmentManager(), "tag");
            break;
        case R.id.copy:
            if (m_ocrResult.length() != 0) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("Lable", m_ocrResult.toString());
                clipboardManager.setPrimaryClip(clipData);
                m_ocrResult.setLength(0);

                Snackbar.make(getWindow().getDecorView(), "Copy to clipboard",
                        Snackbar.LENGTH_SHORT).show();
            }
            else {
                Snackbar.make(getWindow().getDecorView(), "Noting copy to clipboard !",
                        Snackbar.LENGTH_SHORT).show();
            }
            break;
        default:
            break;
        }
        return true;
    }

    class LocalReciever extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: " + intent.getStringExtra("ocrResult"));
            progressBar.setVisibility(View.GONE);
            ocrText.setText(intent.getStringExtra("ocrResult"));
            m_ocrResult.setLength(0);
            m_ocrResult.append(intent.getStringExtra("ocrResult"));
        }
    }

    private boolean save(String inputText, String fileName) {
        boolean res = false;
        FileOutputStream out = null;
        BufferedWriter writer = null;
        try {
            out = openFileOutput(fileName, Context.MODE_PRIVATE);
            writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.write(inputText);
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if (writer != null) {
                    writer.close();
                    res = true;
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        String inputText = ocrText.getText().toString();
        Log.d(TAG, "onDialogPositiveClick: " + inputText);
        SaveDialogFragment saveDialogFragment = (SaveDialogFragment) dialog;
        String fileName = saveDialogFragment.getFileName();
        Log.d(TAG, "onDialogPositiveClick file name = " + fileName);

        if (save(inputText, fileName)) {
            Snackbar.make(getWindow().getDecorView(), "save successfully",
                    Snackbar.LENGTH_SHORT).show();
        }
        else {
            Snackbar.make(getWindow().getDecorView(), "save failed",
                    Snackbar.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        Log.d(TAG, "onDialogNegativeClick: ");
        Snackbar.make(getWindow().getDecorView(), "save failed",
                Snackbar.LENGTH_SHORT).show();
   }
}

package com.example.bo.ocrbaidu;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class OcrActivity extends AppCompatActivity {

    TextView ocrText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        ocrText = findViewById(R.id.ocr_text);

        Intent intent = getIntent();
        String ocrResult = intent.getStringExtra("ocr_result");

        ocrText.setText(ocrResult);
    }
}

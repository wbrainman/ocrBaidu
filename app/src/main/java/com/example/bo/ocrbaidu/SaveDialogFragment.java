package com.example.bo.ocrbaidu;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.text.SimpleDateFormat;
import java.util.Date;


public class SaveDialogFragment extends DialogFragment {

    private static final String TAG = "OCR";
    private String fileName = new String();
    private EditText editText;

    public interface SaveDialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }
    SaveDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.save_dialog, null);
        editText = view.findViewById(R.id.file_name);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fileName = editText.getHint().toString();
                        listener.onDialogPositiveClick(SaveDialogFragment.this);
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onDialogNegativeClick(SaveDialogFragment.this);
                    }
                });

        editText.setHint(getCurrentDate());
        fileName = editText.getHint().toString();

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d(TAG, "onAttach: ");
        try {
            listener = (SaveDialogListener) context;
        }catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() +
                    "must implement SaveDialogListener");
        }



    }

    private String getCurrentDate() {
        Date date = new Date(System.currentTimeMillis());
        Log.d(TAG, "onAttach: date: " + date.toString());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return simpleDateFormat.format(date);
    }

    public String getFileName() {
        return fileName;
    }
}

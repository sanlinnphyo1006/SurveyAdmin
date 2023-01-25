package com.sanlinnphyo.surveyadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    boolean allowToSubmit = false;
    String strTitles = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView txtTotalResponses = findViewById(R.id.txt_total_responses);
        Button btnAllow = findViewById(R.id.btn_allow);
        Button btnExport = findViewById(R.id.btn_export);

        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        btnAllow.setText(R.string.checking_permission);
        Toast.makeText(getApplicationContext(), "ဖောင်တင်ခွင့်ကို စစ်ဆေးနေပါသည်။", Toast.LENGTH_SHORT).show();
        btnAllow.setEnabled(false);

        db.collection("survey").document("status").get().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                allowToSubmit = (Boolean) Objects.requireNonNull(task.getResult().getData()).get("canSubmit");
                btnAllow.setText(Boolean.TRUE.equals(allowToSubmit) ? R.string.disabled_to_submit : R.string.allowed_to_submit);
                btnAllow.setEnabled(true);
                Toast.makeText(getApplicationContext(), allowToSubmit ? "ဖောင်တင်ခွင့်ပြုထားပါသည်။" : "ဖောင်တင်ခွင့်မပြုထားပါ။", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), "Oops got an error. " + task.getException(), Toast.LENGTH_LONG).show();
            }
        });

        db.collection("survey")
                .document("datasDocument")
                .collection("datasCollection")
                .addSnapshotListener((value, error) -> {
                    if(error != null){
                        Toast.makeText(getApplicationContext(), "Oops something went wrong" + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(value != null){
                        txtTotalResponses.setText(String.valueOf(value.getDocuments().size()));
                    }
                });

        btnAllow.setOnClickListener(view -> {
            allowToSubmit = !allowToSubmit;
            btnAllow.setText(Boolean.TRUE.equals(allowToSubmit) ? R.string.disabling_to_submit : R.string.allowing_to_submit);
            btnAllow.setEnabled(false);
            Toast.makeText(getApplicationContext(), allowToSubmit ? "ဖောင်တင်ခြင်းကို ခွင့်ပြုနေပါသည်။" : "ဖောင်တင်ခြင်းကို ပိတ်နေပါသည်။", Toast.LENGTH_SHORT).show();
            db.collection("survey").document("status").update("canSubmit", allowToSubmit).addOnSuccessListener(command -> {
                btnAllow.setText(Boolean.TRUE.equals(allowToSubmit) ? R.string.disabled_to_submit : R.string.allowed_to_submit);
                btnAllow.setEnabled(true);
                Toast.makeText(getApplicationContext(), allowToSubmit ? "ဖောင်တင်ခွင့်ပြုထားပါသည်။" : "ဖောင်တင်ခွင့်မပြုထားပါ။", Toast.LENGTH_SHORT).show();
            });
        });

//        btnExport.setEnabled(Integer.parseInt(txtTotalResponses.getText().toString()) > 0);

        ArrayList<String> titles = new ArrayList<>();

        db.collection("survey")
                .document("totalTitles")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if(snapshot.exists()){
                        for (int i = 0; i < snapshot.getData().size(); i++) {
                            titles.add((String) snapshot.getData().get(String.valueOf(i+1)));
                        }
                    }
                });

        btnExport.setOnClickListener(view -> {

//            db.collection("survey")
//                    .document("datasDocument")
//                    .collection("datasCollection")
//                    .get()
//                    .addOnSuccessListener(queryDocumentSnapshots -> {
//                        for (DocumentSnapshot qds : queryDocumentSnapshots.getDocuments()) {
//
//                        }
//                    });

            // Get Documents folder inside internal storage
            File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            // Then create a file inside Documents folder
            File file = new File(documentsFolder, "survey-data.csv");
            try {
                FileOutputStream fileWriter = new FileOutputStream(file);
                for (int i = 0; i < titles.size(); i++) {
                    strTitles += i == 0 ? titles.get(i) : ", " + titles.get(i);
                }
                Log.e("test", strTitles);
                fileWriter.write(strTitles.getBytes());
                Log.e("test", strTitles.getBytes().toString());
                fileWriter.close();
                Toast.makeText(getApplicationContext(), "Created a file inside " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
            }catch (Exception e){
                e.printStackTrace();
            }
        });
    }
}


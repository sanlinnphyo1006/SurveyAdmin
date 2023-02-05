package com.sanlinnphyo.surveyadmin;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    boolean allowToSubmit = false;
    String content = "";
    String[] titles = {"အစိုးရကျောင်းနှင့် ပုဂ္ဂလိကကျောင်းရွေးချယ်တက်ခြင်းက ကျောင်းသား/သူများအတွက်အရေးကြီးပါသလား။",
                        "ဆရာဆရာမများ၏ သင်ကြားရေးအရည်အချင်းများက ကျောင်းသား/သူများအပေါ်သက်ရောက်မှုရှိသလား။",
                        "ကျောင်း၏ ဥပတိရုပ်ကောင်းမွန်မှု ရန်ပုံငွေတည်ထောင်ထားရှိမှု၊ အကူအညီရရှိမှုသည် အရေးပါပါသလား။"
                        };

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
            btnAllow.setText(Boolean.TRUE.equals(allowToSubmit) ? R.string.disabling_to_submit : R.string.allowing_to_submit);
            btnAllow.setEnabled(false);
            Toast.makeText(getApplicationContext(), allowToSubmit ? "ဖောင်တင်ခြင်းကို ပိတ်နေပါသည်။" : "ဖောင်တင်ခြင်းကို ခွင့်ပြုနေပါသည်။", Toast.LENGTH_SHORT).show();
            allowToSubmit = !allowToSubmit;
            db.collection("survey").document("status").update("canSubmit", allowToSubmit).addOnSuccessListener(command -> {
                btnAllow.setText(Boolean.TRUE.equals(allowToSubmit) ? R.string.disabled_to_submit : R.string.allowed_to_submit);
                btnAllow.setEnabled(true);
                Toast.makeText(getApplicationContext(), allowToSubmit ? "ဖောင်တင်ခွင့်ပြုထားပါသည်။" : "ဖောင်တင်ခွင့်မပြုထားပါ။", Toast.LENGTH_SHORT).show();
            });
        });

        btnExport.setOnClickListener(view -> {
            for (int i = 0; i < titles.length; i++) {
                content = i == 0 ? titles[i] : content + "," + titles[i];
            }

            content += "\n";

            db.collection("survey")
                    .document("datasDocument")
                    .collection("datasCollection")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot snapshot:queryDocumentSnapshots) {

                            if(snapshot.getData().size() > 1){
                                for (int i = 1; i <= 3; i++) {
                                    if(snapshot.getData().get(String.valueOf(i)) != null){
                                        if(i == 1){
                                            content += snapshot.getData().get(String.valueOf(i));
                                        }
                                        else{
                                            content += ", " + snapshot.getData().get(String.valueOf(i));
                                        }
                                    }
                                }
                            }else {
                                if(snapshot.getData().get(String.valueOf(1)) != null){
                                    content += snapshot.getData().get(String.valueOf(1));
                                }
                                else if(snapshot.getData().get(String.valueOf(2)) != null){
                                    content += ", " + snapshot.getData().get(String.valueOf(2));
                                }
                                else{
                                    content += ", , " + snapshot.getData().get(String.valueOf(3));
                                }
                            }
                            content += "\n";
                        }

                        // Get Documents folder inside internal storage
                        File documentsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                        // Then create a file inside Documents folder
                        File file = new File(documentsFolder, "survey-data.csv");
                        try {
                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file.getAbsolutePath()), StandardCharsets.UTF_8));
                            writer.write(content);

                            writer.close();
                            Toast.makeText(getApplicationContext(), "Created a file inside " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    });
        });
    }
}


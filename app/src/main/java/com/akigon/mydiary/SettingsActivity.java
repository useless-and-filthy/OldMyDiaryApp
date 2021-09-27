package com.akigon.mydiary;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.Diary;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.akigon.mydiary.BFunctions.ACTION_AUTO_SYNC;
import static com.akigon.mydiary.BFunctions.ACTION_RESTORED;
import static com.akigon.mydiary.BFunctions.CHOOSE_CSV;
import static com.akigon.mydiary.BFunctions.PICK_IMAGE;
import static com.akigon.mydiary.BFunctions.SP_AUTO_SYNC_KEY;
import static com.akigon.mydiary.BFunctions.SP_LAST_SYNC_STAMP;
import static com.akigon.mydiary.BFunctions.SP_PASS_CODE_KEY;
import static com.akigon.mydiary.BFunctions.SP_SECURITY_QUESTION_KEY;
import static com.akigon.mydiary.BFunctions.feedback_url;
import static com.akigon.mydiary.BFunctions.getDiaryArray;
import static com.akigon.mydiary.BFunctions.open_source_license_url;
import static com.akigon.mydiary.BFunctions.privacy_policy_url;
import static com.akigon.mydiary.BFunctions.terms_condition_url;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "LogoutActivity";

    private Button logout;
    private ProgressDialog progressDialog;
    private TextInputEditText userName;
    private ImageView imageView;
    private AppDatabase noteDatabase;
    private TextView passcodeET;
    private LinearLayout passcodeLL;
    private SwitchCompat autoSyncSwitch;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(FirebaseAuth.getInstance().getCurrentUser().getUid(), Context.MODE_PRIVATE);
        noteDatabase = AppDatabase.getInstance(this);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userName = findViewById(R.id.username);
        userName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptUserName();
            }
        });

        imageView = findViewById(R.id.profilePic);
        passcodeET = findViewById(R.id.passcode_ET);
        passcodeLL=findViewById(R.id.passcode_LL);
        autoSyncSwitch = findViewById(R.id.auto_sync_switch);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, PICK_IMAGE);
            }
        });

        logout = findViewById(R.id.logout);
        progressDialog = new ProgressDialog(SettingsActivity.this);

        autoSyncSwitch.setChecked(sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY, false));

        if (sharedPreferences.getBoolean(SP_AUTO_SYNC_KEY, false)){
            findViewById(R.id.web_link).setVisibility(View.VISIBLE);
        }else {
            findViewById(R.id.web_link).setVisibility(View.GONE);
        }

        logout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        findViewById(R.id.restoreTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] options = {"Internal Storage", "Cloud"};
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Restore Options")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                                    chooseFile.setType("*/*");
                                    chooseFile = Intent.createChooser(chooseFile, "Choose CSV file");
                                    startActivityForResult(chooseFile, CHOOSE_CSV);
                                } else {
                                    saveFromCloud();
                                }
                            }
                        });

                AlertDialog ok = builder.create();
                ok.show();
            }
        });

        findViewById(R.id.backupTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] options = {"Internal Storage", "Cloud"};
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Save Diary to")
                        .setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    backUpToInternalStorage();
                                } else {
                                    backUpToCloud();
                                }
                            }
                        });

                AlertDialog ok = builder.create();
                ok.show();
            }
        });

        passcodeLL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this);
                View view = LayoutInflater.from(SettingsActivity.this).inflate(R.layout.edit_view, null);
                TextInputEditText editText = view.findViewById(R.id.inputET);
                TextInputEditText hintText = view.findViewById(R.id.hintET);

                builder.setTitle("Set Password")
                        .setView(view)
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                updatePassword(editText.getText().toString(), hintText.getText().toString());
                            }
                        })
                        .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();
            }
        });

        autoSyncSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                //it tells what state it has become to
                sharedPreferences.edit().putBoolean(SP_AUTO_SYNC_KEY, isChecked).commit();
                intent.putExtra(ACTION_AUTO_SYNC, ACTION_AUTO_SYNC);
                setResult(RESULT_OK, intent);

                if (isChecked){
                    findViewById(R.id.web_link).setVisibility(View.VISIBLE);
                }else {
                    findViewById(R.id.web_link).setVisibility(View.GONE);
                }

            }
        });

        findViewById(R.id.loadSamplesTV).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                try {
                    InputStream is = getAssets().open("samples.csv");
                    InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
                    CSVReader csvReader = new CSVReader(reader);
                    String[] nextRecord;

                    // we are going to read data line by line
                    int p = 0;
                    long curT = System.currentTimeMillis();
                    while ((nextRecord = csvReader.readNext()) != null) {
                        Diary diary = new Diary(nextRecord[0], curT, Long.parseLong(nextRecord[2]), nextRecord[3], nextRecord[4], nextRecord[5], nextRecord[6], Boolean.parseBoolean(nextRecord[7]));
                        noteDatabase.diaryDao().insert(diary);
                        p++;
                    }
                    intent.putExtra(ACTION_RESTORED, ACTION_RESTORED);
                    setResult(RESULT_OK, intent);
                    showAlert("Restore Completed", "Total " + p + " entries added");
                } catch (Exception e) {
                    e.printStackTrace();
                    showAlert("Restore Completed", "Exception: " + e.getMessage());
                }
            }
        });

        findViewById(R.id.sendFeedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedback_url));
                startActivity(browserIntent);
            }
        });

        findViewById(R.id.open_source_licence).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(open_source_license_url));
                startActivity(browserIntent);
            }
        });
        findViewById(R.id.termsncondition).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(terms_condition_url));
                startActivity(browserIntent);
            }
        });
        findViewById(R.id.privacy_policy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacy_policy_url));
                startActivity(browserIntent);
            }
        });
        setUserData(FirebaseAuth.getInstance().getUid());
        updatePassText();
    }

    private void updatePassText() {
        String s = sharedPreferences.getString(SP_PASS_CODE_KEY, "");
        passcodeET.setText(s);
    }

    private void updatePassword(String s, String s1) {
        if (sharedPreferences.edit().putString(SP_PASS_CODE_KEY, s).commit() && sharedPreferences.edit().putString(SP_SECURITY_QUESTION_KEY, s1).commit()) {
            showAlert("Security Alert", "Passcode saved successfully");
            updatePassText();
        } else {
            showAlert("Security Alert", "Error saving pass code");
        }
    }


    private void setUserData(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("USERS").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Map<String, Object> map = task.getResult().getData();
                    if (map != null && map.containsKey("USERNAME")) {
                        ((TextView) findViewById(R.id.username)).setText(map.get("USERNAME").toString());
                    }
                    if (map != null && map.containsKey("PHOTO_URL")) {
                        Glide.with(SettingsActivity.this).load(map.get("PHOTO_URL").toString()).centerCrop().circleCrop().placeholder(R.mipmap.boy).into((ImageView) findViewById(R.id.profilePic));
                    }
                }
            }
        });
    }

    private void backUpToCloud() {
        progressDialog.show();
        List<Diary> diaries = noteDatabase.diaryDao().getAll();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Get a new write batch
        WriteBatch batch = db.batch();
        CollectionReference userData = db.collection("USERS").document(FirebaseAuth.getInstance().getUid()).collection("JOURNALS");
        for (Diary xx : diaries) {
            batch.set(userData.document(xx.getObjectId()), xx, SetOptions.merge());
        }
        // Commit the batch
        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    showAlert("Backup Complete", "Total " + diaries.size() + " entries uploaded");
                } else {
                    showAlert("Backup Complete", "Total " + diaries.size() + " entries uploaded");
                }
            }
        });
    }

    private void promptUserName() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        EditText editText = new EditText(this);
        builder.setTitle("Set Username")
                .setView(editText)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userName.setText(editText.getText().toString());
                        updateUsername();
                    }
                });
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Glide.with(this).load(data.getData()).centerCrop().placeholder(R.mipmap.boy).into(imageView);
            ProgressDialog progressDoalog = new ProgressDialog(this);
            progressDoalog.setMax(100);
            progressDoalog.setMessage("Uploading File");
            progressDoalog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDoalog.show();
            String realPath = ImageFilePath.getPath(this, data.getData());
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            String uniqueID = UUID.randomUUID().toString() + realPath.substring(realPath.lastIndexOf("."));
            StorageReference imagesRef = storageRef.child(uniqueID);
            imagesRef.putFile(data.getData()).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDoalog.setProgress((int) progress);
                    if (progressDoalog.getProgress() == progressDoalog.getMax()) {
                        progressDoalog.dismiss();
                    }
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    imagesRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            updatePhoto(uri);
                        }
                    });
                }

                ;
            });
        } else if (requestCode == CHOOSE_CSV && resultCode == RESULT_OK && data.getData() != null) {
            restoreFromStorage(data.getData());
        }
    }

    private void restoreFromStorage(Uri csvFile) {
        progressDialog.show();
        try {
            InputStream is = getContentResolver().openInputStream(csvFile);
            InputStreamReader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
            CSVReader csvReader = new CSVReader(reader);
            String[] nextRecord;

            // we are going to read data line by line
            int p = 0;
            long curT = System.currentTimeMillis();
            while ((nextRecord = csvReader.readNext()) != null) {
                Diary diary = new Diary(nextRecord[0], curT, Long.parseLong(nextRecord[2]), nextRecord[3], nextRecord[4], nextRecord[5], nextRecord[6], Boolean.parseBoolean(nextRecord[7]));
                noteDatabase.diaryDao().insert(diary);
                p++;
            }
            intent.putExtra(ACTION_RESTORED, ACTION_RESTORED);
            setResult(RESULT_OK, intent);
            showAlert("Restore Completed", "Total " + p + " entries added");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Restore Completed", "Exception: " + e.getMessage());
        }
    }

    private void updateUsername() {
        progressDialog.show();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("USERNAME", userName.getText().toString());
        FirebaseFirestore.getInstance().collection("USERS").document(user.getUid()).set(map, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "User profile updated", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updatePhoto(Uri uri) {
        progressDialog.show();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Map<String, Object> map = new HashMap<>();
        map.put("PHOTO_URL", uri.toString());
        FirebaseFirestore.getInstance().collection("USERS").document(user.getUid()).set(map, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this, "User profile updated", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showAlert(String title, String message) {
        progressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(SettingsActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog ok = builder.create();
        ok.show();
    }

    private Intent intent = new Intent();

    private void saveFromCloud() {
        progressDialog.show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference userData = db.collection("USERS").document(FirebaseAuth.getInstance().getUid()).collection("JOURNALS");
        userData.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null) {
                    for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                        noteDatabase.diaryDao().insert(snapshot.toObject(Diary.class));
                    }
                    intent.putExtra(ACTION_RESTORED, ACTION_RESTORED);
                    setResult(RESULT_OK, intent);
                    showAlert("Restore Completed", "Total " + querySnapshot.getDocuments().size() + " entries added.");
                } else {
                    showAlert("Restore Completed", "Warning: No data found on server");
                }
            }
        });
    }

    private void backUpToInternalStorage() {
        progressDialog.show();
        String file_main = "mydiary.csv";
        File folder = new File(Environment.getExternalStorageDirectory() + "/My Diary");
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                showAlert("Backup Alert", "Error: Could not create folder");
                return;
            }
        }
        File file = new File(Environment.getExternalStorageDirectory() + "/My Diary/" + file_main);
        if (file.exists()) {
            file.delete();
        }
        try {
            boolean isCreated = file.createNewFile();
            if (!isCreated) {
                showAlert("Backup Alert", "Error: Could not create a new file");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Backup Alert", e.getMessage());
        }
        try {
            List<String[]> diaryList = new ArrayList<>();
            for (Diary xx : noteDatabase.diaryDao().getAll()) {
                diaryList.add(getDiaryArray(xx));
            }
            // create FileWriter object with file as parameter
            FileWriter outputfile = new FileWriter(file);
            // create CSVWriter object filewriter object as parameter
            CSVWriter writer = new CSVWriter(outputfile);
            // create a List which contains String array
            writer.writeAll(diaryList);
            // closing writer connection
            writer.close();
            showAlert("Backup Completed", "Total " + diaryList.size() + " entries saved in a file.");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            showAlert("Backup Alert", e.getMessage());
        }
    }
}

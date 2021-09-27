package com.akigon.mydiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.Diary;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

import static com.akigon.mydiary.BFunctions.getTagsFormatted;
import static com.akigon.mydiary.BFunctions.getTimeString;
import static com.akigon.mydiary.BFunctions.minToRead;

public class ViewDiary extends AppCompatActivity {

    private static final int EDIT_NOTE = 450;
    private RichEditor webview;
    private TextView titleTV, createdOnTV;
    private AppDatabase noteDatabase;
    private Diary diary;
    private ProgressDialog progressDialog;

    private TextView updatedTV, tagsTV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_diary);
        progressDialog = new ProgressDialog(this);
        webview = findViewById(R.id.mywebview);
        titleTV = findViewById(R.id.titleText);
        createdOnTV = findViewById(R.id.createdOnText);
        updatedTV = findViewById(R.id.updatedAtText);
        tagsTV = findViewById(R.id.tagsText);
        noteDatabase = AppDatabase.getInstance(this);
        webview.setEditorFontSize(18);
        webview.editOff();
        displayNote(getIntent().getStringExtra("objectId"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.edit_note:
                Intent intent = new Intent(this, AddEditDiary.class);
                intent.putExtra("objectId", getIntent().getStringExtra("objectId"));
                startActivityForResult(intent, EDIT_NOTE);
                return true;
            case R.id.upload_note:
                uploadNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_NOTE && resultCode == RESULT_OK) {
            String objectId = data.getStringExtra("objectId");
            Intent intent = new Intent();
            intent.putExtra("objectId", objectId);
            setResult(RESULT_OK, intent);
            displayNote(objectId);
            Toast.makeText(ViewDiary.this, "Saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayNote(String objectId) {
        diary = noteDatabase.diaryDao().getDiary(objectId);
        titleTV.setText(diary.getTitle());
        createdOnTV.setText(getTimeString(diary.getCreatedAt()) + "\t • \t" + minToRead(diary.getContent())+" min");
        updatedTV.setText("Updated: " + getTimeString(diary.getUpdatedAt()));
        tagsTV.setText(getTagsFormatted(diary.getTags()));
        webview.setHtml(diary.getContent());
        setUserData(diary.getUid());
    }



    private void uploadNote() {
        progressDialog.show();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("USERS").document(FirebaseAuth.getInstance().getUid()).collection("JOURNALS")
                .document(diary.getObjectId()).set(diary, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    showAlert("Upload complete", "1 Entry uploaded successfully.");
                } else {
                    showAlert("Upload Alert", "Error: " + task.getException().getMessage());
                }
            }
        });
    }

    private void showAlert(String title, String message) {
        progressDialog.dismiss();
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
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

    private void setUserData(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("USERS").document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Map<String, Object> map = task.getResult().getData();
                    if (map != null && map.containsKey("USERNAME")) {
                        ((TextView) findViewById(R.id.usernameText)).setText(map.get("USERNAME").toString());
                    }
                    if (map != null && map.containsKey("PHOTO_URL")) {
                        Glide.with(ViewDiary.this).load(map.get("PHOTO_URL").toString()).centerCrop().circleCrop().placeholder(R.mipmap.boy).into((ImageView) findViewById(R.id.userPfp));
                    }else {
                        Glide.with(ViewDiary.this).load(R.mipmap.boy).centerCrop().circleCrop().into((ImageView) findViewById(R.id.userPfp));
                    }
                }
            }
        });
    }


}
package com.akigon.mydiary;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.DeletedDiary;
import com.akigon.mydiary.db.Diary;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.commons.text.WordUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.akigon.mydiary.BFunctions.EDIT_NOTE;
import static com.akigon.mydiary.BFunctions.PICK_AUDIO;
import static com.akigon.mydiary.BFunctions.PICK_IMAGE;
import static com.akigon.mydiary.BFunctions.PICK_VIDEO;
import static com.akigon.mydiary.BFunctions.SP_LAST_SYNC_STAMP;


public class AddEditDiary extends AppCompatActivity {

    private RichEditor mEditor;
    private EditText titleET;
    private String objectId = UUID.randomUUID().toString();
    private long createdAt = System.currentTimeMillis();
    private AppCompatEditText tagsET;
    private boolean isHidden = false;

    private boolean supscript = false, subscript = false;
    private static final String TAG = "AddEditDiary";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_diary);
        noteDatabase = AppDatabase.getInstance(AddEditDiary.this);
        titleET = findViewById(R.id.diary_title);
        tagsET = findViewById(R.id.diary_tags);
        tagsET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        mEditor = (RichEditor) findViewById(R.id.editor);
        mEditor.setEditorFontSize(18);
        //mEditor.setEditorFontColor(Color.BLACK);
        //mEditor.setPadding(14, 0, 14, 8);
        //mEditor.setBackground("https://raw.githubusercontent.com/wasabeef/art/master/chip.jpg");
        mEditor.setPlaceholder("Insert text here...");
        //mEditor.setInputEnabled(false);

        findViewById(R.id.action_bold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBold();
            }
        });

        findViewById(R.id.action_italic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setItalic();
            }
        });

        findViewById(R.id.action_underline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setUnderline();
            }
        });
        findViewById(R.id.action_strikethrough).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setStrikeThrough();
            }
        });

        findViewById(R.id.action_subscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setSubscript();
            }
        });


        findViewById(R.id.action_superscript).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setSuperscript();
            }
        });


        findViewById(R.id.action_heading1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(1);
            }
        });

        findViewById(R.id.action_heading2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(2);
            }
        });

        findViewById(R.id.action_heading3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(3);
            }
        });

        findViewById(R.id.action_heading4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setHeading(4);
            }
        });

//        findViewById(R.id.action_heading5).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mEditor.setHeading(5);
//            }
//        });


//        findViewById(R.id.action_txt_color).setOnClickListener(new View.OnClickListener() {
//            private boolean isChanged = false;
//
//            @Override
//            public void onClick(View v) {
//                if (isChanged) {
//                    mEditor.setTextColor(Color.BLACK);
//                    Toast.makeText(AddEditDiary.this, "Color: Black", Toast.LENGTH_SHORT).show();
//                } else {
//                    mEditor.setTextColor(Color.WHITE);
//                    Toast.makeText(AddEditDiary.this, "Color: White", Toast.LENGTH_SHORT).show();
//                }
//                isChanged = !isChanged;
//            }
//        });

//        findViewById(R.id.action_bg_color).setOnClickListener(new View.OnClickListener() {
//            private boolean isChanged;
//
//            @Override
//            public void onClick(View v) {
//                mEditor.setTextBackgroundColor(isChanged ? Color.TRANSPARENT : Color.YELLOW);
//                isChanged = !isChanged;
//            }
//        });

        findViewById(R.id.action_indent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setIndent();
            }
        });

        findViewById(R.id.action_outdent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setOutdent();
            }
        });

        findViewById(R.id.action_align_left).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignLeft();
            }
        });

        findViewById(R.id.action_align_center).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignCenter();
            }
        });

        findViewById(R.id.action_align_right).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setAlignRight();
            }
        });


        findViewById(R.id.action_insert_numbers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setNumbers();
            }
        });

        findViewById(R.id.action_insert_bullets).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.setBullets();
            }
        });


        findViewById(R.id.action_insert_link).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AddEditDiary.this);
                View v1 = LayoutInflater.from(AddEditDiary.this).inflate(R.layout.input_link_view, null, false);
                builder.setView(v1);
                builder.setTitle("Enter URL")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String Ltitle = ((TextInputEditText) v1.findViewById(R.id.link_title)).getText().toString().trim();
                                String link = ((TextInputEditText) v1.findViewById(R.id.link)).getText().toString().trim();

                                if (!link.isEmpty()) {
                                    if (Ltitle.isEmpty()) {
                                        mEditor.insertLink(link, link);
                                    } else {
                                        mEditor.insertLink(link, Ltitle);
                                    }
                                } else {
                                    Toast.makeText(AddEditDiary.this, "No Link", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();

            }
        });
        findViewById(R.id.action_insert_checkbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditor.insertTodo();
            }
        });

        findViewById(R.id.action_insert_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showBottomSheet();
            }
        });

        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && !(source.charAt(i) == ';') && !(source.charAt(i) == ' ')) {
                        return "";
                    }
                }
                return null;
            }
        };

        tagsET.setFilters(new InputFilter[]{filter});

//        tagsET.addTextChangedListener(new TextWatcher() {
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                tagsET.setText(s.toString().trim().replaceAll("\\s+", " "));
//            }
//
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count,
//                                          int after) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//
//            }
//        });

        if (getIntent().getStringExtra("objectId") != null) {
            displayNote(getIntent().getStringExtra("objectId"));
        }
    }


    private void showBottomSheet() {
        View view = getLayoutInflater().inflate(R.layout.bottom_media_options, null);
        final Dialog mBottomSheetDialog = new Dialog(this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(view); // your custom view.
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.show();

        view.findViewById(R.id.action_insert_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AddEditDiary.this);
                EditText editText = new EditText(AddEditDiary.this);
                builder.setView(editText);
                builder.setTitle("Enter Image Url")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mEditor.insertImage(editText.getText().toString().trim());
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setNeutralButton("STORAGE", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                getIntent.setType("image/*");
                                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                pickIntent.setType("image/*");
                                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                                startActivityForResult(chooserIntent, PICK_IMAGE);
                            }
                        });
                builder.show();
            }
        });

        view.findViewById(R.id.action_insert_youtube).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AddEditDiary.this);
                EditText editText = new EditText(AddEditDiary.this);
                builder.setView(editText);
                builder.setTitle("Enter Youtube Url")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String base_url = "https://www.youtube.com/embed/" + extractYTId(editText.getText().toString().trim());
                                mEditor.insertYoutubeVideo(base_url);
                            }
                        })
                        .setNegativeButton("Dismiss", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builder.show();

            }
        });

        view.findViewById(R.id.action_insert_video).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TAG", "onSuccess: " + "in pickk video 12");
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("video/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("video/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Video");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, PICK_VIDEO);
            }
        });

        view.findViewById(R.id.action_insert_audio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("audio/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("audio/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Audio");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, PICK_AUDIO);

            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.add_edit_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.save_note:
                saveNote();
                return true;
            case R.id.undo_note:
                mEditor.undo();
                return true;
            case R.id.redo_note:
                mEditor.redo();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void displayNote(String objectId2) {
        Diary diary = noteDatabase.diaryDao().getDiary(objectId2);
        if (diary != null) {
            mEditor.setHtml(diary.getContent());
            titleET.setText(diary.getTitle());
            tagsET.setText(diary.getTags());
            objectId = diary.getObjectId();
            createdAt = diary.getCreatedAt();
            isHidden = diary.isHidden();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE || requestCode == PICK_AUDIO || requestCode == PICK_VIDEO) {
            if (resultCode == RESULT_OK) {
                ProgressDialog progressDoalog = new ProgressDialog(this);
                progressDoalog.setProgress(10);
                progressDoalog.setCancelable(false);
                progressDoalog.setMax(100);
                progressDoalog.setMessage("Uploading File");
                //progressDoalog.setTitle("Uploading");
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
//                                Uri downloadUrl = uri;
                                Log.d("TAG", "onSuccess: " + "file uploaded");
                                if (requestCode == PICK_IMAGE) {
                                    Log.d("TAG", "onSuccess: " + "in pick image");
                                    mEditor.insertImage(uri.toString());
                                } else if (requestCode == PICK_VIDEO) {
                                    Log.d("TAG", "onSuccess: " + "in pick video");
                                    mEditor.insertVideo(uri.toString());
                                } else if (resultCode == PICK_AUDIO) {
                                    mEditor.insertAudio(uri.toString());
                                }
                                //Do what you want with the url
                            }
                        });
                    }

                    ;
                });
            }
        }
    }

    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    public static String extractYTId(String ytUrl) {
        String vId = null;
        Pattern pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(ytUrl);
        if (matcher.matches()) {
            vId = matcher.group(1);
        }
        return vId;
    }

    private void saveNote() {
        Diary note = new Diary(objectId, System.currentTimeMillis(), createdAt, titleET.getText().toString(), mEditor.getHtml(), FirebaseAuth.getInstance().getUid(), getTagsText(), isHidden);
        new InsertTask(this, note).execute();
    }

    private String getTagsText() {
        String s = tagsET.getText().toString().trim();
        if (s.isEmpty()) {
            return s;
        } else {
            String[] tgs = s.split(";");
            StringBuilder m = new StringBuilder();
            for (String x : tgs) {
                x = WordUtils.capitalize(x.trim());
                if (!x.isEmpty()) {
                    m.append(x).append(";");
                }
            }
            return m.toString();
        }
    }

    private AppDatabase noteDatabase;

    private static class InsertTask extends AsyncTask<Void, Void, Boolean> {
        private WeakReference<AddEditDiary> activityReference;
        private Diary note;

        // only retain a weak reference to the activity
        InsertTask(AddEditDiary context, Diary note) {
            activityReference = new WeakReference<>(context);
            this.note = note;
        }

        // doInBackground methods runs on a worker thread
        @Override
        protected Boolean doInBackground(Void... objs) {
            activityReference.get().noteDatabase.diaryDao().insert(note);
            return true;
        }

        // onPostExecute runs on main thread
        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool) {
                activityReference.get().setResult(note.getObjectId());
            }
        }

    }

    private void setResult(String objectId) {
        Intent intent = new Intent();
        intent.putExtra("objectId", objectId);
        setResult(RESULT_OK, intent);
        finish();
    }
}
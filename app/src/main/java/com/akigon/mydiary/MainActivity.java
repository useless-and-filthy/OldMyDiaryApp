package com.akigon.mydiary;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.DeletedDiary;
import com.akigon.mydiary.db.Diary;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


import static com.akigon.mydiary.BFunctions.ACTION_AUTO_SYNC;
import static com.akigon.mydiary.BFunctions.ACTION_RESTORED;
import static com.akigon.mydiary.BFunctions.ADD_NOTE;
import static com.akigon.mydiary.BFunctions.EDIT_NOTE;
import static com.akigon.mydiary.BFunctions.SETTING_SEND;
import static com.akigon.mydiary.BFunctions.SP_LAST_SYNC_STAMP;

public class MainActivity extends AppCompatActivity {


    private TextView searchCount;
    private AppDatabase noteDatabase;
    private NestedScrollView nestedScrollView;
    private RecyclerView recyclerView;
    private ProgressBar progressBarSync;
    private DiaryRecyclerAdapter adapter;
    private List<Diary> list = new ArrayList<>();
    private LinearLayout tagsLayout;
    private SharedPreferences prefs;
    private static final String TAG = "MainActivity";
    private Synchronize synchronize;

    // powerful query
    private String searchText = "";
    private String tagText = "";
    private boolean loadHidden=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noteDatabase = AppDatabase.getInstance(this);

        recyclerView = findViewById(R.id.notice_recycler_view);
        progressBarSync = findViewById(R.id.progressbar_sync);
        searchCount = findViewById(R.id.searchResultCount);
        tagsLayout = findViewById(R.id.tagLL);
        prefs = getSharedPreferences(FirebaseAuth.getInstance().getCurrentUser().getUid(), Context.MODE_PRIVATE);
        nestedScrollView = findViewById(R.id.notice_scroll_view);
        nestedScrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight()) {
                    //when reach last item position
                    int indexStart = list.size();
                    List<Diary> newList = noteDatabase.diaryDao().getDiaries(getLastCreatedAtInList(), getSearchText(), getTagText(),loadHidden);
                    if (newList.isEmpty()) {
                        return;
                    }
                    for (Diary xx : newList) {
                        if (!list.contains(xx)) {
                            list.add(xx);
                        }
                    }
                    adapter.notifyItemRangeInserted(indexStart, list.size());
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 22);
            }
        }

        //setting adapter
        adapter = new DiaryRecyclerAdapter(this, list);
        adapter.setOnItemClickListener(new DiaryRecyclerAdapter.OnItemClickListener() {
            @Override
            public void OnItemLongClick(Diary journal, int position) {
                showBottomSheet(journal, position);
            }

            @Override
            public void OnItemClick(Diary journal) {
                Intent intent = new Intent(MainActivity.this, ViewDiary.class);
                intent.putExtra("objectId", journal.getObjectId());
                startActivityForResult(intent, EDIT_NOTE);
            }
        });

        recyclerView.setAdapter(adapter);
        //synchronize
        synchronize = new Synchronize(this);
        synchronize.startSync();
        newUX();
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateTags();
    }

    private String getSearchText() {
        return "%" + searchText + "%";
    }

    private String getTagText() {
        if (tagText.isEmpty() || tagText.equals("All"))
            return "%" + "" + "%";

        return "%" + tagText + ";%";
    }

    private long getLastCreatedAtInList() {
        if (list.isEmpty()) {
            long ts=System.currentTimeMillis();
            return ts;
        } else {
            return list.get(list.size() - 1).getCreatedAt();
        }
    }

    private void newUX() {
        list.clear();
        List<Diary> notes = noteDatabase.diaryDao().getDiaries(getLastCreatedAtInList(), getSearchText(), getTagText(),loadHidden);
        if (notes.isEmpty()) {
            Toast.makeText(this, "No Data", Toast.LENGTH_LONG).show();
        }
        list.addAll(notes);
        adapter.notifyDataSetChanged();
        updateSubtitle();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchCount.setVisibility(View.GONE);
                    searchText = query;
                    newUX();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String query) {
                    if (query.isEmpty()) {
                        searchCount.setVisibility(View.GONE);
                    } else {
                        searchCount.setVisibility(View.VISIBLE);
                        nestedScrollView.scrollTo(0, 0);
                        searchCount.setText("Found " + noteDatabase.diaryDao().countDiaries("%"+query+"%", getTagText(),loadHidden) + " Records");
                        searchCount.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                onQueryTextSubmit(query);
                            }
                        });
                    }
                    return true;

                }
            });
            searchView.setOnCloseListener(new SearchView.OnCloseListener() {
                @Override
                public boolean onClose() {
                    searchText = "";
                    newUX();
                    return false;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem=menu.findItem(R.id.hidden_diaries);
        if(loadHidden){
            menuItem.setTitle("Show Unhidden Diaries");
        }else {
            menuItem.setTitle("Show Hidden Diaries");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.add_note:
                Intent intent = new Intent(this, AddEditDiary.class);
                startActivityForResult(intent, ADD_NOTE);
                return true;

            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), SETTING_SEND);
                return true;
            case R.id.hidden_diaries:
                if(loadHidden){
                    loadHidden=false;
                    newUX();
                    populateTags();
                }else {
                    loadHidden=true;
                    newUX();
                    populateTags();

                }
                return true;
            case R.id.delelte_all_diaries:
                deleteAllDiaries();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showBottomSheet(final Diary journal, int position) {
        View view = getLayoutInflater().inflate(R.layout.bottom_options_diary, null);
        final Dialog mBottomSheetDialog = new Dialog(MainActivity.this, R.style.MaterialDialogSheet);
        mBottomSheetDialog.setContentView(view); // your custom view.
        mBottomSheetDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mBottomSheetDialog.getWindow().setGravity(Gravity.BOTTOM);
        mBottomSheetDialog.show();

        TextView cancel = view.findViewById(R.id.bsd_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
            }
        });

        TextView delete = view.findViewById(R.id.bsd_delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noteDatabase.diaryDao().delete(journal);
                noteDatabase.diaryDao().insertDeleted(new DeletedDiary(journal.getObjectId(), System.currentTimeMillis()));
                list.remove(journal);
                adapter.notifyItemRemoved(position);
                mBottomSheetDialog.dismiss();
                updateSubtitle();
                populateTags();
                synchronize.initialize(new HashMap<>(), prefs.getLong(SP_LAST_SYNC_STAMP, 0), false);
            }
        });

        TextView copy = view.findViewById(R.id.bsd_copy);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                String journalText = "";
                ClipData clip = ClipData.newPlainText("Journal", journalText);
                clipboard.setPrimaryClip(clip);
                mBottomSheetDialog.dismiss();
            }
        });
//unhide
        TextView hide= view.findViewById(R.id.bsd_hide);
        if (journal.isHidden()){
            hide.setText("Unhide");
        }else {
            hide.setText("Hide");
        }
        hide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                journal.setUpdatedAt(System.currentTimeMillis());
                if (journal.isHidden()){
                    journal.setHidden(false);
                }else {
                    journal.setHidden(true);
                }

                noteDatabase.diaryDao().insert(journal);
                list.remove(journal);
                adapter.notifyItemRemoved(position);
                mBottomSheetDialog.dismiss();
                updateSubtitle();
                populateTags();
                synchronize.initialize(new HashMap<>(), prefs.getLong(SP_LAST_SYNC_STAMP, 0), false);
            }
        });

        TextView edit = view.findViewById(R.id.bsd_edit);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBottomSheetDialog.dismiss();
                Intent intent = new Intent(MainActivity.this, AddEditDiary.class);
                intent.putExtra("objectId", journal.getObjectId());
                startActivityForResult(intent, EDIT_NOTE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_NOTE && resultCode == RESULT_OK) {
            String objectId = data.getStringExtra("objectId");
            Diary diary = noteDatabase.diaryDao().getDiary(objectId);
            list.add(0, diary);
            adapter.notifyItemInserted(0);
            Toast.makeText(MainActivity.this, "Saved", Toast.LENGTH_SHORT).show();
            updateSubtitle();
            synchronize.initialize(new HashMap<>(), prefs.getLong(SP_LAST_SYNC_STAMP, 0), false);
        } else if (requestCode == EDIT_NOTE && resultCode == RESULT_OK) {
            String objectId = data.getStringExtra("objectId");
            int indexD = getDiaryIndexInList(objectId);
            if (indexD != -1) {
                list.remove(indexD);
                Diary diary = noteDatabase.diaryDao().getDiary(objectId);
                list.add(indexD, diary);
                adapter.notifyItemChanged(indexD);
                Toast.makeText(MainActivity.this, "Updated", Toast.LENGTH_SHORT).show();
                synchronize.initialize(new HashMap<>(), prefs.getLong(SP_LAST_SYNC_STAMP, 0), false);
            } else {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SETTING_SEND && resultCode == RESULT_OK) {
            if (data.getStringExtra(ACTION_AUTO_SYNC) != null) {
                Log.d(TAG, "onActivityResult: sync changed from setting");
                synchronize.startSync();
            }
            if (data.getStringExtra(ACTION_RESTORED) != null) {
                Log.d(TAG, "onActivityResult: restored data from setting");
                newUX();
            }
        }
    }

    private void updateSubtitle() {
        Objects.requireNonNull(getSupportActionBar()).setSubtitle(noteDatabase.diaryDao().countDiaries(getSearchText(), getTagText(),loadHidden) + " Entries");
    }

    private int getDiaryIndexInList(String objectId) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getObjectId().equals(objectId)) {
                return i;
            }
        }
        return -1;
    }


    private void populateTags() {
        Log.d(TAG, "populateTags: tagText:" + tagText + ':');
        tagsLayout.removeAllViewsInLayout();
        View.OnClickListener tagClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeStyleOfTags();
                v.setBackgroundResource(R.drawable.bg_round_black);
                ((TextView) v).setTextColor(Color.WHITE);
                if (((TextView) v).getText().toString().equals("All")) {
                    tagText = "";
                } else {
                    tagText = ((TextView) v).getText().toString();
                }
                newUX();
            }
        };

        boolean hasPutEmptyTag = false;
        ArrayList<String> tempList=new ArrayList<>();
        List<String> tags = noteDatabase.diaryDao().getDistinctTags(loadHidden);
        for (String x : tags) {
            String[] splitTags = x.split(";");
            for (String splitTag : splitTags) {
                splitTag = splitTag.trim();
                if (!splitTag.isEmpty() && !tempList.contains(splitTag)) {
                    tempList.add(splitTag);
                } else if (!hasPutEmptyTag) {
                    hasPutEmptyTag = true;
                    TextView v = (TextView) getLayoutInflater().inflate(R.layout.tag_view, tagsLayout, false);
                    if (splitTag.equals(tagText)) {
                        v.setBackgroundResource(R.drawable.bg_round_black);
                        v.setTextColor(Color.WHITE);
                    } else {
                        v.setBackgroundColor(Color.TRANSPARENT);
                        v.setTextColor(Color.BLACK);
                    }
                    v.setText("All");
                    v.setOnClickListener(tagClickListener);
                    tagsLayout.addView(v);
                }
            }
        }
        Collections.sort(tempList);
        for (String splitTag:tempList){
            TextView v = (TextView) getLayoutInflater().inflate(R.layout.tag_view, tagsLayout, false);
            if (splitTag.equals(tagText)) {
                v.setBackgroundResource(R.drawable.bg_round_black);
                v.setTextColor(Color.WHITE);
            } else {
                v.setBackgroundColor(Color.TRANSPARENT);
                v.setTextColor(Color.BLACK);
            }
            v.setText(splitTag);
            v.setOnClickListener(tagClickListener);
            tagsLayout.addView(v);
        }
    }

    private void removeStyleOfTags() {
        for (int i = 0; i < tagsLayout.getChildCount(); i++) {
            TextView tv = (TextView) tagsLayout.getChildAt(i);
            tv.setBackgroundColor(Color.TRANSPARENT);
            tv.setTextColor(Color.BLACK);
        }
    }

    private void deleteAllDiaries() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to all your notes? \nThis action is irreversible. After this, you can restore data from previous backup only.")
                .setPositiveButton("DELETE", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        noteDatabase.diaryDao().deleteAllDiaries();
                        searchText = "";
                        tagText = "";
                        list.clear();
                        adapter.notifyDataSetChanged();
                        updateSubtitle();
                        populateTags();
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

    public void updateListFromSync(Collection<Object> dL) {
        boolean isAdded = false;
        for (Object s : dL) {
            Diary d = (Diary) s;
            if (d.getCreatedAt() > getLastCreatedAtInList()) {
                isAdded = true;
                int indexD = getDiaryIndexInList(d.getObjectId());
                if (indexD != -1) {
                    Log.d(TAG, "updateListFromSync: contains " + d.getTitle());
                    list.remove(indexD);
                    list.add(indexD, d);
                    adapter.notifyItemChanged(indexD);
                } else {
                    list.add(d);
                    Log.d(TAG, "updateListFromSync: added new " + d.getTitle());
                }
            }
        }
        if (isAdded) {
            Collections.sort(list, new Comparator<Diary>() {
                @Override
                public int compare(Diary o1, Diary o2) {
                    return (int) (o2.getCreatedAt() - o1.getCreatedAt());
                }
            });
            adapter.notifyDataSetChanged();
            updateSubtitle();
            populateTags();
        }
    }

    public void showProgressBar() {
        progressBarSync.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        progressBarSync.setVisibility(View.GONE);
    }

    private void showHiddenDiaries(){

    }
}
package com.akigon.mydiary;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.akigon.mydiary.db.AppDatabase;
import com.akigon.mydiary.db.DeletedDiary;
import com.akigon.mydiary.db.Diary;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.akigon.mydiary.BFunctions.SP_AUTO_SYNC_KEY;
import static com.akigon.mydiary.BFunctions.SP_LAST_SYNC_STAMP;

public class Synchronize {
    private static final String TAG = "Synchronize";
    private MainActivity activity;
    private AppDatabase noteDatabase;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userData = db.collection("USERS").document(FirebaseAuth.getInstance().getUid()).collection("JOURNALS");
    private ListenerRegistration listenerRegistration;
    private SharedPreferences prefs;
    private long syncStampMain;

    public Synchronize(MainActivity activity) {
        this.activity = activity;
        this.noteDatabase = AppDatabase.getInstance(activity);
        prefs = activity.getSharedPreferences(FirebaseAuth.getInstance().getCurrentUser().getUid(), Context.MODE_PRIVATE);
    }


    public void startSync() {
        if (prefs.getBoolean(SP_AUTO_SYNC_KEY, false)) {
            activity.showProgressBar();
            syncStampMain = System.currentTimeMillis();
            long syncStamp = prefs.getLong(SP_LAST_SYNC_STAMP, 0);
            Log.d(TAG, "Starting sync for " + syncStamp);
            listenerRegistration = userData.whereGreaterThanOrEqualTo("updatedAt", syncStamp).addSnapshotListener(new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                    if (e != null) {
                        Log.d(TAG, "onEvent: Error in Sync Cloud " + e);
                        return;
                    }
                    if (value != null) {
                        if (value.getDocuments().size() == 0) {
                            Log.d(TAG, "onEvent: received in IF");
                            if (prefs.getLong(SP_LAST_SYNC_STAMP, 0) == 0) {
                                initialize(new HashMap<>(), syncStamp, true);
                            } else {
                                Log.d(TAG, "onEvent: Snapshot size is 0, NO NEED TO UPDATE REGISTRATION");
                                activity.hideProgressBar();
                            }
                        } else {
                            Log.d(TAG, "onEvent: received in else");
                            Map<String, Object> cloudMap = new HashMap<>();
                            for (DocumentSnapshot snapshot : value.getDocuments()) {
                                cloudMap.put(snapshot.getId(), snapshot.toObject(Diary.class));
                            }
                            initialize(cloudMap, syncStamp, true);
                        }
                    } else {
                        Log.d(TAG, "onEvent: null value on cloud");
                    }
                }
            });
        }else {
            Log.d(TAG, "startSync: sync false");
        }
    }

    public void initialize(Map<String, Object> cloudMap, long syncStamp, boolean isCloudEvent) {
        if (prefs.getBoolean(SP_AUTO_SYNC_KEY, false)) {
            activity.showProgressBar();
            //get data from web as per last sync stamp order by modified
            //put in map
            //get data from app as per last sync stamp order by modified
            List<Diary> allDiary = noteDatabase.diaryDao().getModifiedDiaries(syncStamp);

            //put in map
            Map<String, Object> offlineMap = new HashMap<>();
            for (Diary x : allDiary) {
                offlineMap.put(x.getObjectId(), x);
            }
            //merge both map as per latest modification
            //check if value exists in offline data -- because problem is of cloud --
            // if modified in offline but not cloud then we have to upload that
            if (!offlineMap.isEmpty()) {
                List<Object> toPut = new ArrayList<>();
                List<String> toRemove = new ArrayList<>();

                for (Map.Entry<String, Object> set : cloudMap.entrySet()) {
                    if (offlineMap.containsKey(set.getKey())) {
                        if (((Diary) offlineMap.get(set.getKey())).getUpdatedAt() > ((Diary) set.getValue()).getUpdatedAt()) {
                            //last updated in offline
                            toPut.add(offlineMap.get(set.getKey()));
                            //cloudMap.put(set.getKey(), ); // concurrent exception
                            offlineMap.remove(set.getKey());
                        } else {
                            //last updated in online
                            offlineMap.put(set.getKey(), set.getValue());
                            toRemove.add(set.getKey());
                            //cloudMap.remove(set.getKey());
                        }
                    }
                }

                //remove from cloud map
                for (String rT : toRemove) {
                    cloudMap.remove(rT);
                }
                for (Object o : toPut) {
                    Diary d = (Diary) o;
                    cloudMap.put(d.getObjectId(), o);
                }
            }

            //insert cloud map to offline database
            for (Map.Entry<String, Object> set : cloudMap.entrySet()) {
                Diary diary = (Diary) set.getValue();
                if (diary.getTags() == null) {
                    Log.d(TAG, "initialize: null tag");
                }
                noteDatabase.diaryDao().insert(diary);
            }

            //delete from cloud which has been deleted in offline
            List<String> deletedDiaryList = noteDatabase.diaryDao().getDeletedIds();
            WriteBatch batch = db.batch();
            for (String xx : deletedDiaryList) {
                batch.delete(userData.document(xx));
            }
            //upload offline database map to cloud
            for (Map.Entry<String, Object> set : offlineMap.entrySet()) {
                batch.set(userData.document(set.getKey()), set.getValue(), SetOptions.merge());
            }
            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    noteDatabase.diaryDao().deleteAllDeletedDiaries();
                    if (isCloudEvent) {
                        updateLastSync(syncStamp);
                    }
                    activity.hideProgressBar();
                }
            });

            //notify changes to main activity
            activity.updateListFromSync(cloudMap.values());
        }else {
            Log.d(TAG, "startSync: sync false initialise");
        }
    }


    private void updateLastSync(long syncStamp) {
        Log.d(TAG, "updateLastSync: sync completed");

        listenerRegistration.remove();
        Log.d(TAG, "updateLastSync: listener removed for " + syncStamp);

        prefs.edit().putLong(SP_LAST_SYNC_STAMP, syncStampMain).apply();
        Log.d(TAG, "updateLastSync: new sync time put " + syncStampMain);
        startSync();
    }

}

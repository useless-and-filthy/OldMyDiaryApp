package com.akigon.mydiary.db;

import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Calendar;
import java.util.Locale;

import static com.akigon.mydiary.BFunctions.getTimeString;


@Entity
public class Diary {
    @PrimaryKey
    @NonNull
    private String objectId;
    private long updatedAt;
    private long createdAt;
    private String title;
    private String content;
    private String uid;
    private String tags;
    private boolean isHidden=false;

    public Diary() {
    }

    public Diary(@NonNull String objectId, long updatedAt, long createdAt, String title, String content, String uid, String tag,boolean isHidden) {
        this.objectId = objectId;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
        this.title = title;
        this.content = content;
        this.uid = uid;
        this.tags = tag;
        this.isHidden=isHidden;
    }

    @NonNull
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(@NonNull String objectId) {
        this.objectId = objectId;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }
}

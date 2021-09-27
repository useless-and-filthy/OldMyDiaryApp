package com.akigon.mydiary.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DeletedDiary {
    @PrimaryKey @NonNull
    private String objectId;
    private long deletedAt;


    public DeletedDiary() {}

    public DeletedDiary(@NonNull String objectId, long deletedAt) {
        this.objectId = objectId;
        this.deletedAt = deletedAt;
    }

    @NonNull
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public long getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(long deletedAt) {
        this.deletedAt = deletedAt;
    }
}

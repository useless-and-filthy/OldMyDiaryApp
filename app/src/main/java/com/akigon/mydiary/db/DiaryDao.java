package com.akigon.mydiary.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DiaryDao {
    @Query("SELECT * FROM Diary WHERE (createdAt < :startAt) AND (tags LIKE :tag) AND (content LIKE :searchText OR title LIKE :searchText) AND (isHidden=:isHidden) ORDER BY createdAt desc LIMIT(5)")
    List<Diary> getDiaries(long startAt, String searchText, String tag, boolean isHidden);

    @Query("SELECT COUNT(*) FROM Diary WHERE (tags LIKE :tag) AND (content LIKE :searchText OR title LIKE :searchText) AND (isHidden=:isHidden)")
    int countDiaries(String searchText, String tag, boolean isHidden);

    @Query("SELECT * FROM Diary WHERE objectId=:diaryId")
    Diary getDiary(String diaryId);

    @Query("SELECT * FROM Diary ORDER BY createdAt desc")
    List<Diary> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Diary diary);

    @Delete
    void delete(Diary diary);

    @Query("DELETE FROM Diary")
    void deleteAllDiaries();

    @Query("SELECT DISTINCT tags FROM Diary WHERE isHidden=:isHidden")
    List<String> getDistinctTags(boolean isHidden);

    //for deleted diary
    @Query("SELECT objectId FROM DeletedDiary")
    List<String> getDeletedIds();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertDeleted(DeletedDiary... diaries);

    @Query("DELETE FROM DeletedDiary")
    void deleteAllDeletedDiaries();

    //sync
    @Query("SELECT * FROM Diary WHERE updatedAt>=:afterTimestamp")
    List<Diary> getModifiedDiaries(long afterTimestamp);
}

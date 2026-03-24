package com.dailyserviceapp.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.dailyserviceapp.data.local.dao.CustomerDao;
import com.dailyserviceapp.data.local.entity.CustomerEntity;

/**
 * Application local Room Database.
 * Serves as the single source of truth for offline-first architecture.
 */
@Database(entities = {CustomerEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CustomerDao customerDao();
}

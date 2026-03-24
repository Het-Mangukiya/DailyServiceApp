package com.dailyserviceapp.di;

import android.content.Context;

import androidx.room.Room;

import com.dailyserviceapp.data.local.AppDatabase;
import com.dailyserviceapp.data.local.dao.CustomerDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt Module to provide Room Database and DAOs.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    @Provides
    @Singleton
    public AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "dailyservice_db")
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries()
                .build();
    }

    @Provides
    public CustomerDao provideCustomerDao(AppDatabase database) {
        return database.customerDao();
    }
}

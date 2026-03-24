package com.dailyserviceapp.di;

import android.content.Context;

import com.dailyserviceapp.core.offline.OfflineCache;
import com.dailyserviceapp.core.utils.PreferenceManager;

import com.dailyserviceapp.data.local.dao.CustomerDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public PreferenceManager providePreferenceManager(@ApplicationContext Context context) {
        return new PreferenceManager(context);
    }

    @Provides
    @Singleton
    public OfflineCache provideOfflineCache(@ApplicationContext Context context, CustomerDao customerDao) {
        return new OfflineCache(context, customerDao);
    }
}

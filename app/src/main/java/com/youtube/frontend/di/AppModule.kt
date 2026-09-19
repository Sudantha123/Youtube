package com.youtube.frontend.di

import android.content.Context
import androidx.room.Room
import com.youtube.frontend.db.AppDatabase
import com.youtube.frontend.player.PlayerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "youtube_frontend.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun providePlayerManager(@ApplicationContext context: Context): PlayerManager {
        return PlayerManager(context)
    }
}

package com.akhilasdeveloper.marsroverphotos.di

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.room.Room
import com.akhilasdeveloper.marsroverphotos.utilities.Constants
import com.akhilasdeveloper.marsroverphotos.utilities.Constants.MARS_ROVER_DATABASE_NAME
import com.akhilasdeveloper.marsroverphotos.utilities.Utilities
import com.akhilasdeveloper.marsroverphotos.api.MarsRoverPhotosService
import com.akhilasdeveloper.marsroverphotos.db.MarsRoverDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGsonBuilder(): Gson {
        return GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create()
    }

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson): Retrofit.Builder {
        return Retrofit.Builder()
            .baseUrl(Constants.URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
    }

    @Singleton
    @Provides
    fun provideMainService(retrofit: Retrofit.Builder): MarsRoverPhotosService {
        return retrofit
            .build()
            .create(MarsRoverPhotosService::class.java)
    }

    @Singleton
    @Provides
    fun provideUtilities(
        @ApplicationContext context: Context
    ): Utilities {
        return Utilities(context)
    }

    @Singleton
    @Provides
    fun providesInputManager(@ApplicationContext context: Context): InputMethodManager =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @Singleton
    @Provides
    fun provideMarsRoverDatabase(
        @ApplicationContext app: Context
    ) = Room.databaseBuilder(
        app,
        MarsRoverDatabase::class.java,
        MARS_ROVER_DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    @Singleton
    @Provides
    fun provideMarsRoverDao(db: MarsRoverDatabase) = db.getMarsRoverDao()

    @Singleton
    @Provides
    fun provideRemoteKeysDao(db: MarsRoverDatabase) = db.getRemoteKeysDao()

}

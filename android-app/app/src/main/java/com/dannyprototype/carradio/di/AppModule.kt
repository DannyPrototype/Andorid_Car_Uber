package com.dannyprototype.carradio.di

import android.content.Context
import androidx.room.Room
import com.dannyprototype.carradio.data.dao.FuelDao
import com.dannyprototype.carradio.data.dao.TripDao
import com.dannyprototype.carradio.data.db.AppDatabase
import com.dannyprototype.carradio.obd.OBD2Manager
import com.dannyprototype.carradio.obd.OBD2ManagerStub
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
            "car_radio_db"
        ).build()
    }

    @Provides
    fun provideTripDao(db: AppDatabase): TripDao = db.tripDao()

    @Provides
    fun provideFuelDao(db: AppDatabase): FuelDao = db.fuelDao()

    @Provides
    @Singleton
    fun provideOBD2Manager(stub: OBD2ManagerStub): OBD2Manager = stub
}

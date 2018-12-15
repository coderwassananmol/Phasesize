package com.example.gooded.phasesizeapplication;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by root on 15/12/18.
 */
@Dao
public interface PinsDao {

    @Insert
    void insert(Pins pins);

    @Query("SELECT * FROM pins_table")
    LiveData<List<Pins>> getAllPins();
}

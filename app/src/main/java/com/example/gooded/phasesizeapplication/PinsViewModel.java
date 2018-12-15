package com.example.gooded.phasesizeapplication;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

/**
 * Created by root on 15/12/18.
 */

public class PinsViewModel extends AndroidViewModel {
    private PinsRepository pinsRepository;

    private LiveData<List<Pins>> allPins;

    public PinsViewModel (Application application) {
        super(application);
        pinsRepository = new PinsRepository(application);
        allPins = pinsRepository.getAllWords();
    }

    LiveData<List<Pins>> getAllWords() { return allPins; }

    public void insert(Pins pin) {
        pinsRepository.insert(pin);
    }
}

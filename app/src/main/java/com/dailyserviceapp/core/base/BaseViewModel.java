package com.dailyserviceapp.core.base;

import androidx.lifecycle.ViewModel;

public abstract class BaseViewModel extends ViewModel {
    
    /**
     * Called when ViewModel is cleared
     */
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}

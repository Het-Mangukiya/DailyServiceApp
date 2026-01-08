package com.dailyserviceapp.core.base;

import androidx.lifecycle.ViewModel;

/**
 * Base ViewModel for all ViewModels in the DailyDrop application.
 * Extends AndroidX ViewModel to provide lifecycle-aware data management.
 * 
 * <p>This base class can be extended with common ViewModel functionality
 * such as repository access, common data transformations, or shared
 * business logic across multiple ViewModels.</p>
 * 
 * @author DailyDrop Team
 * @version 1.0
 * @since 2026-01-08
 */
public abstract class BaseViewModel extends ViewModel {
    
    /**
     * Called when this ViewModel is no longer used and will be destroyed.
     * Can be overridden by subclasses to clean up resources.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
    }
}

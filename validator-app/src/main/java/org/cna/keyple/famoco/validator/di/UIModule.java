package org.cna.keyple.famoco.validator.di;

import org.cna.keyple.famoco.validator.di.scopes.ActivityScoped;
import org.cna.keyple.famoco.validator.di.scopes.FragmentScoped;
import org.cna.keyple.famoco.validator.ui.activities.MainActivity;
import org.cna.keyple.famoco.validator.ui.fragments.CardReaderFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class UIModule {
    @ActivityScoped
    @ContributesAndroidInjector
    abstract MainActivity mainActivity();

    @FragmentScoped
    @ContributesAndroidInjector
    abstract CardReaderFragment cardReaderFragment();
}

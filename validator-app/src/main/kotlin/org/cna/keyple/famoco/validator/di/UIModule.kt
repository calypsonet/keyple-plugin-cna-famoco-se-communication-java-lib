package org.cna.keyple.famoco.validator.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.cna.keyple.famoco.validator.di.scopes.ActivityScoped
import org.cna.keyple.famoco.validator.di.scopes.FragmentScoped
import org.cna.keyple.famoco.validator.ui.activities.MainActivity
import org.cna.keyple.famoco.validator.ui.fragments.CardReaderFragment

@Module
abstract class UIModule {
    @ActivityScoped
    @ContributesAndroidInjector
    abstract fun mainActivity(): MainActivity

    @FragmentScoped
    @ContributesAndroidInjector
    abstract fun cardReaderFragment(): CardReaderFragment
}
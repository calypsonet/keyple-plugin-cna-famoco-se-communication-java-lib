package org.cna.keyple.famoco.validator

import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import org.cna.keyple.famoco.validator.di.AppComponent
import org.cna.keyple.famoco.validator.di.DaggerAppComponent
import timber.log.Timber
import timber.log.Timber.DebugTree

class Application : DaggerApplication() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
    }

    override fun applicationInjector(): AppComponent? {
        return DaggerAppComponent.builder().application(this).build()
    }
}
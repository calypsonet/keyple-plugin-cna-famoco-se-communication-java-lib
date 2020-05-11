/********************************************************************************
 * Copyright (c) 2020 Calypso Networks Association https://www.calypsonet-asso.org/
 ********************************************************************************/
package org.cna.keyple.famoco.example

import android.app.Application
import timber.log.Timber

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}

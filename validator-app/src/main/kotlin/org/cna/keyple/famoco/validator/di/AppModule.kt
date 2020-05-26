package org.cna.keyple.famoco.validator.di

import android.content.Context
import dagger.Binds
import dagger.Module
import org.cna.keyple.famoco.validator.Application

@Module
abstract class AppModule {
    // expose Application as an injectable context
    @Binds
    abstract fun bindContext(application: Application): Context
}
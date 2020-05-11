package org.cna.keyple.famoco.validator.di;

import android.content.Context;

import org.cna.keyple.famoco.validator.Application;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class AppModule {
    // expose Application as an injectable context
    @Binds
    abstract Context bindContext(Application application);
}
package org.cna.keyple.famoco.validator.di

import dagger.Module
import dagger.Provides
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.cna.keyple.famoco.validator.di.scopes.AppScoped
import org.cna.keyple.famoco.validator.rx.SchedulerProvider

@Module
class SchedulerModule {
    @Provides
    @AppScoped
    fun provideSchedulerProvider(): SchedulerProvider {
        return SchedulerProvider(Schedulers.io(), AndroidSchedulers.mainThread())
    }
}
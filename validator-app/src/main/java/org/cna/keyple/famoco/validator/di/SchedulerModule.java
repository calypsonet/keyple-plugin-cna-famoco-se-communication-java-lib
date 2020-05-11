package org.cna.keyple.famoco.validator.di;

import org.cna.keyple.famoco.validator.di.scopes.AppScoped;
import org.cna.keyple.famoco.validator.rx.SchedulerProvider;

import dagger.Module;
import dagger.Provides;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Module
public class SchedulerModule {
    @Provides
    @AppScoped
    public SchedulerProvider provideSchedulerProvider(){
        return new SchedulerProvider(Schedulers.io(), AndroidSchedulers.mainThread());
    }
}

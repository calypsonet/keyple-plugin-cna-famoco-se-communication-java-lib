package org.cna.keyple.famoco.validator.di;

import org.cna.keyple.famoco.validator.Application;
import org.cna.keyple.famoco.validator.di.scopes.AppScoped;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjector;
import dagger.android.support.AndroidSupportInjectionModule;

@AppScoped
@Component(modules = {
        ViewModelModule.class,
        AppModule.class,
        UIModule.class,
        SchedulerModule.class,
        AndroidSupportInjectionModule.class})
public interface AppComponent extends AndroidInjector<Application> {

    @Component.Builder
    interface Builder {
        @BindsInstance
        AppComponent.Builder application(Application application);

        AppComponent build();
    }
}

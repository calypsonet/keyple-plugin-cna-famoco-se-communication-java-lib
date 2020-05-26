package org.cna.keyple.famoco.validator.di

import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import org.cna.keyple.famoco.validator.Application
import org.cna.keyple.famoco.validator.di.scopes.AppScoped

@AppScoped
@Component(modules = [ViewModelModule::class, AppModule::class, UIModule::class, SchedulerModule::class, AndroidSupportInjectionModule::class])
interface AppComponent : AndroidInjector<Application?> {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}
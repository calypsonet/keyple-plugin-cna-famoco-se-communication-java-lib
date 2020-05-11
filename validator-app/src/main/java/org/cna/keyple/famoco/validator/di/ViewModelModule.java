package org.cna.keyple.famoco.validator.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.cna.keyple.famoco.validator.di.scopes.AppScoped;
import org.cna.keyple.famoco.validator.viewModels.CardReaderViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(CardReaderViewModel.class)
    abstract ViewModel bindCardReaderViewModel(CardReaderViewModel cardReaderViewModel);

    @Binds
    @AppScoped
    abstract ViewModelProvider.Factory bindViewModelFactory(ViewModelFactory factory);
}

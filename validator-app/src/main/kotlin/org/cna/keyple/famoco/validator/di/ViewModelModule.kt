package org.cna.keyple.famoco.validator.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.cna.keyple.famoco.validator.di.scopes.AppScoped
import org.cna.keyple.famoco.validator.viewModels.CardReaderViewModel

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(CardReaderViewModel::class)
    abstract fun bindCardReaderViewModel(cardReaderViewModel: CardReaderViewModel?): ViewModel?

    @Binds
    @AppScoped
    abstract fun bindViewModelFactory(factory: ViewModelFactory?): ViewModelProvider.Factory?
}
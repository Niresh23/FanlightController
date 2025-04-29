package com.niresh23.fanlightcontroller.di

import com.niresh23.fanlightcontroller.ble.BleServiceExecutor
import com.niresh23.fanlightcontroller.ble.IBleServiceExecutor
import com.niresh23.fanlightcontroller.storage.ISettingsLocalStorage
import com.niresh23.fanlightcontroller.storage.SettingsLocalStorageImpl
import com.niresh23.fanlightcontroller.ui.connection.ConnectionViewModel
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val AppModule = module {
    factory<ISettingsLocalStorage> {
        SettingsLocalStorageImpl(androidContext())
    }

    single<IBleServiceExecutor> {
        BleServiceExecutor(androidContext())
    }

    viewModel {
        FanlightViewModel(get(), get())
    }
    viewModel {
        ConnectionViewModel(get(), androidContext())
    }
}
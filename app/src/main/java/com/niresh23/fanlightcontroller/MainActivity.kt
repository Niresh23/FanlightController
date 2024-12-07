package com.niresh23.fanlightcontroller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.niresh23.fanlightcontroller.ui.home.HomeScreen
import com.niresh23.fanlightcontroller.ui.theme.FanlightControllerTheme
import com.niresh23.fanlightcontroller.ui.connection.ConnectionViewModel
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val fanlightViewModel: FanlightViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    fanlightViewModel.eventFlow.collectLatest {
                        connectionViewModel.onEvent(it)
                    }
                }
            }
        }

        setContent {
            FanlightControllerTheme {
                HomeScreen(
                    viewModel = fanlightViewModel,
                    connectionViewModel = connectionViewModel
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onStart() {
        super.onStart()
        fanlightViewModel.onStart()
        Dexter.withContext(this).withPermission(
            Manifest.permission.POST_NOTIFICATIONS
        ).withListener(object : PermissionListener {
            override fun onPermissionGranted(var1: PermissionGrantedResponse?) {

            }

            override fun onPermissionDenied(response: PermissionDeniedResponse?) {

            }

            override fun onPermissionRationaleShouldBeShown(
                request: PermissionRequest?,
                token: PermissionToken?
            ) {

            }
        }).check()
    }

    override fun onStop() {
        super.onStop()
        fanlightViewModel.onStop()
    }
}
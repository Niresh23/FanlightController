package com.niresh23.fanlightcontroller.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.niresh23.fanlightcontroller.R
import com.niresh23.fanlightcontroller.ui.audiovisualizer.VisualizerScreen
import com.niresh23.fanlightcontroller.ui.color.ColorScreen
import com.niresh23.fanlightcontroller.viewmodel.FanlightViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: FanlightViewModel) {
    val startDestination = NavRoute.Color.name
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            text = "Home"
                        )
                        Spacer(modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f))
                        Button(onClick = {
                            viewModel.disconnected()
                        }) {
                            Text(text = stringResource(id = R.string.disconnect_lbl))
                        }
                    }

                }
            )},
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                NavRoute.values().map { it.name }.forEachIndexed { _, item ->
                    NavigationBarItem(
                        icon = { when(item) {
                            NavRoute.Color.name -> {
                                Icon(Icons.Filled.Palette, contentDescription = item)
                            }
                            NavRoute.Visualizer.name -> {
                                Icon(Icons.Filled.GraphicEq, contentDescription = item)
                            }
                        } },
                        label = { Text(item) },
                        selected = currentDestination?.hierarchy?.any { it.route == item } == true,
                        onClick = {
                            navController.navigate(item) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
        }})
    {
        it.calculateTopPadding()
        it.calculateBottomPadding()

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp)
        ) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable(route = NavRoute.Color.name) {
                    ColorScreen(viewModel = viewModel)
                }
                composable(route = NavRoute.Visualizer.name) {
                    VisualizerScreen(viewModel = viewModel)
                }
            }
        }
    }


}
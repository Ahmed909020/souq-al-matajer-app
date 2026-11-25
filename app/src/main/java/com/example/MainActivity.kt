package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.MarketplaceRepository
import com.example.data.NetworkConnectivityObserver
import com.example.data.attemptAutoSignIn
import com.example.data.authStateFlow
import com.example.data.signOutUser
import com.example.ui.MarketplaceViewModel
import com.example.ui.screens.AddStoreScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyStoreScreen
import com.example.ui.screens.StoreDetailScreen
import com.example.ui.screens.StoreTrackingScreen
import com.example.ui.theme.SouqAlMatagerTheme
import com.example.util.MarketplaceNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SouqAlMatagerTheme {
                SouqAppRoot()
            }
        }
    }
}

@Composable
private fun SouqAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope,
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        val uid = user.uid
        val email = user.email ?: ""
        val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: "تاجر / زائر"
        key(uid) {
            val viewModel: MarketplaceViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        MarketplaceViewModel(
                            repository = MarketplaceRepository(db),
                            currentUserId = uid,
                            userEmail = email,
                            userName = displayName,
                            networkObserver = NetworkConnectivityObserver(app),
                            notificationManager = MarketplaceNotificationManager(app),
                        )
                    }
                }
            )
            SouqAuthenticatedShell(
                viewModel = viewModel,
                onSignOut = {
                    signOutUser(credentialManager, scope)
                },
            )
        }
    }
}

@Composable
private fun SouqAuthenticatedShell(
    viewModel: MarketplaceViewModel,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val tabs = remember(viewModel.isAdmin) {
        buildList {
            add(
                BottomNavTab(
                    route = "home",
                    label = "السوق",
                    selectedIcon = Icons.Filled.Storefront,
                    unselectedIcon = Icons.Outlined.Storefront,
                    testTag = "nav_home",
                )
            )
            add(
                BottomNavTab(
                    route = "favorites",
                    label = "المفضلة",
                    selectedIcon = Icons.Filled.Favorite,
                    unselectedIcon = Icons.Outlined.FavoriteBorder,
                    testTag = "nav_favorites",
                )
            )
            add(
                BottomNavTab(
                    route = "add_store",
                    label = "أضف متجرك",
                    selectedIcon = Icons.Filled.AddBusiness,
                    unselectedIcon = Icons.Outlined.AddBusiness,
                    testTag = "nav_add_store",
                )
            )
            add(
                BottomNavTab(
                    route = "my_store",
                    label = "متجري",
                    selectedIcon = Icons.Filled.Inventory2,
                    unselectedIcon = Icons.Outlined.Inventory2,
                    testTag = "nav_my_store",
                )
            )
            if (viewModel.isAdmin) {
                add(
                    BottomNavTab(
                        route = "admin",
                        label = "الإدارة",
                        selectedIcon = Icons.Filled.AdminPanelSettings,
                        unselectedIcon = Icons.Outlined.AdminPanelSettings,
                        testTag = "nav_admin",
                    )
                )
            }
        }
    }

    val showTopLevelNav = currentRoute in tabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                ),
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onStoreClick = { storeId ->
                                    navController.navigate("store/$storeId")
                                },
                                onOpenAdmin = {
                                    navController.navigate("admin") {
                                        launchSingleTop = true
                                    }
                                },
                                onSignOut = onSignOut,
                            )
                        }
                        composable("favorites") {
                            FavoritesScreen(
                                viewModel = viewModel,
                                onStoreClick = { storeId ->
                                    navController.navigate("store/$storeId")
                                },
                                onBrowseMarketplace = {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }
                        composable("add_store") {
                            AddStoreScreen(viewModel = viewModel)
                        }
                        composable("my_store") {
                            MyStoreScreen(
                                viewModel = viewModel,
                                onNavigateToAddStore = {
                                    navController.navigate("add_store") {
                                        launchSingleTop = true
                                    }
                                },
                                onOpenTrackingPage = { storeId ->
                                    navController.navigate("track/$storeId")
                                },
                            )
                        }
                        composable("admin") {
                            AdminDashboardScreen(viewModel = viewModel)
                        }
                        composable(
                            route = "store/{storeId}",
                            arguments = listOf(navArgument("storeId") { type = NavType.StringType }),
                        ) { entry ->
                            val storeId = entry.arguments?.getString("storeId") ?: ""
                            StoreDetailScreen(
                                storeId = storeId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(
                            route = "track/{storeId}",
                            arguments = listOf(navArgument("storeId") { type = NavType.StringType }),
                        ) { entry ->
                            val storeId = entry.arguments?.getString("storeId") ?: ""
                            StoreTrackingScreen(
                                storeId = storeId,
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

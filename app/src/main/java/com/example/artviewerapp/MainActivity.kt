package com.example.artviewerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.artviewerapp.ui.theme.ArtViewerAppTheme
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize database repository
        FavoritesRepository.initialize(this)

        setContent {
            ArtViewerAppTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(navController) }
            composable("search") { backStackEntry ->
                val viewModel: SearchViewModel = androidx.lifecycle.viewmodel.compose.viewModel(backStackEntry)
                SearchScreen(navController, viewModel)
            }
            composable("favorites") { FavoritesScreen(navController) }
            composable("details/{artworkID}") { backStackEntry ->
                val artworkID = backStackEntry.arguments?.getString("artworkID")?.toIntOrNull()
                if (artworkID != null) {
                    ArtworkDetailsScreen(artworkID)
                } else {
                    Text("Invalid artwork ID")
                }
            }
        }
    }
}


@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            selected = navController.currentDestination?.route == "home",
            onClick = { navController.navigate("home") },
            icon = {
                Icon(
                    painter = rememberAsyncImagePainter(R.drawable.ic_home),
                    contentDescription = "Home"
                )
            },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = navController.currentDestination?.route == "search",
            onClick = { navController.navigate("search") },
            icon = {
                Icon(
                    painter = rememberAsyncImagePainter(R.drawable.ic_search),
                    contentDescription = "Search"
                )
            },
            label = { Text("Search") }
        )
        NavigationBarItem(
            selected = navController.currentDestination?.route == "favorites",
            onClick = { navController.navigate("favorites") },
            icon = {
                Icon(
                    painter = rememberAsyncImagePainter(R.drawable.ic_favorite),
                    contentDescription = "Favorites"
                )
            },
            label = { Text("Favorites") }
        )
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    RandomArtworkScreen(navController)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomArtworkScreen(navController: NavHostController) {
    val artworks = remember { mutableStateListOf<ObjectDetailsResponse>() }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val initialArtworks = fetchRandomArtworks(limit = 20)
            artworks.addAll(initialArtworks)
            isLoading = false
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(artworks) { artwork ->
            Row(modifier = Modifier
                .padding(8.dp)
                .clickable { navController.navigate("details/${artwork.objectID}") }) {
                Image(
                    painter = rememberAsyncImagePainter(artwork.primaryImage),
                    contentDescription = artwork.title,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = artwork.title ?: "Untitled",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Рік: ${artwork.accessionYear ?: "Невідомо"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            } else {
                LaunchedEffect(artworks.size) {
                    scope.launch {
                        isLoading = true
                        val newArtworks = fetchRandomArtworks(limit = 10)
                        artworks.addAll(newArtworks)
                        isLoading = false
                    }
                }
            }
        }
    }
}

suspend fun fetchRandomArtworks(limit: Int): List<ObjectDetailsResponse> {
    val randomIDs = List(limit) {Random.nextInt(1, 460000) }
    println("Generated random IDs: $randomIDs")

    return randomIDs.mapNotNull { id ->
        try {
            val artwork = RetrofitInstance.api.getObjectDetailsByObjectID(id)
            if (!artwork.primaryImage.isNullOrEmpty()) {
                artwork
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching details for ID: $id - ${e.message}")
            null
        }
    }
}

@Composable
fun ArtworkDetailsScreen(artworkID: Int) {
    val scope = rememberCoroutineScope()
    var artwork by remember { mutableStateOf<ObjectDetailsResponse?>(null) }
    var isFavorite by remember { mutableStateOf(false) }

    LaunchedEffect(artworkID) {
        scope.launch {
            artwork = RetrofitInstance.api.getObjectDetailsByObjectID(artworkID)
            isFavorite = artwork?.let { FavoritesRepository.isFavorite(it) } ?: false
        }
    }

    artwork?.let {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(it.primaryImage),
                contentDescription = it.title,
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it.title ?: "Untitled", style = MaterialTheme.typography.titleLarge)
            Text(text = "Artist: ${it.artistDisplayName ?: "Unknown"}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Bio: ${it.artistDisplayBio ?: "No biography available"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                scope.launch {
                    if (isFavorite) {
                        FavoritesRepository.removeFromFavorites(it)
                    } else {
                        FavoritesRepository.addToFavorites(it)
                    }
                    isFavorite = !isFavorite
                }
            }) {
                Text(if (isFavorite) "Забрати з Вподобаних" else "Вподобати")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavHostController, viewModel: SearchViewModel) {
    val searchQuery = viewModel.searchQuery.value
    val searchResults = viewModel.searchResults
    val isLoading = viewModel.isLoading.value

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Введіть запит") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.performSearch() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Пошук")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(searchResults) { artwork ->
                Row(modifier = Modifier
                    .padding(8.dp)
                    .clickable { navController.navigate("details/${artwork.objectID}") }) {
                    Image(
                        painter = rememberAsyncImagePainter(artwork.primaryImage),
                        contentDescription = artwork.title,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = artwork.title ?: "Untitled",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Рік: ${artwork.accessionYear ?: "Невідомо"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}



suspend fun fetchArtworksWithQuery(query: String, limit: Int): List<ObjectDetailsResponse> {
    val searchResponse = RetrofitInstance.api.searchArtworks(query)
    val objectIDs = searchResponse.objectIDs.orEmpty().take(limit)

    return objectIDs.mapNotNull { id ->
        try {
            RetrofitInstance.api.getObjectDetailsByObjectID(id)
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
fun FavoritesScreen(navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var favorites by remember { mutableStateOf<List<ObjectDetailsResponse>>(emptyList()) }

    LaunchedEffect(Unit) {
        scope.launch {
            favorites = FavoritesRepository.getFavorites()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(favorites) { artwork ->
            Row(modifier = Modifier
                .padding(8.dp)
                .clickable { navController.navigate("details/${artwork.objectID}") }) {
                Image(
                    painter = rememberAsyncImagePainter(artwork.primaryImage),
                    contentDescription = artwork.title,
                    modifier = Modifier.size(100.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = artwork.title ?: "Untitled",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Рік: ${artwork.accessionYear ?: "Невідомо"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

package com.israel.gamecompletion

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class Game(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val description: String,
    val completed: Boolean,
    val notes: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

fun saveGames(context: Context, games: List<Game>) {
    val sharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = gson.toJson(games)
    sharedPreferences.edit {
        putString("games_list", json)
    }
}

fun loadGames(context: Context): MutableList<Game> {
    val sharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = sharedPreferences.getString("games_list", null)
    val type = object : TypeToken<MutableList<Game>>() {}.type
    return gson.fromJson(json, type) ?: mutableListOf()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val (title, setTitle) = remember { mutableStateOf("") }
    val (description, setDescription) = remember { mutableStateOf("") }
    val (completed, setCompleted) = remember { mutableStateOf(false) }
    val (error, setError) = remember { mutableStateOf<String?>(null) }
    val (showClearDialog, setShowClearDialog) = remember { mutableStateOf(false) }
    val (editingGame, setEditingGame) = remember { mutableStateOf<Game?>(null) }

    val games = remember { mutableStateListOf<Game>() }

    LaunchedEffect(Unit) {
        games.addAll(loadGames(context))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { setShowClearDialog(false) },
            title = { Text("Clear All Games") },
            text = { Text("Are you sure you want to clear all games? This action cannot be undone.") },
            confirmButton = {
                TextButton({
                    games.clear()
                    saveGames(context, games)
                    setShowClearDialog(false)
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton({ setShowClearDialog(false) }) { Text("Cancel") }
            }
        )
    }

    editingGame?.let { game ->
        val (notes, setNotes) = remember { mutableStateOf(game.notes) }
        val (isCompleted, setIsCompleted) = remember { mutableStateOf(game.completed) }

        AlertDialog(
            onDismissRequest = { setEditingGame(null) },
            title = { Text(game.title) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCompleted, onCheckedChange = { setIsCompleted(it) })
                        Text("Completed")
                    }
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { setNotes(it) },
                        label = { Text("Notes") })
                }
            },
            confirmButton = {
                TextButton({
                    val index = games.indexOfFirst { it.id == game.id }
                    if (index != -1) {
                        games[index] = game.copy(completed = isCompleted, notes = notes)
                        saveGames(context, games)
                    }
                    setEditingGame(null)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton({ setEditingGame(null) }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Game Completion") },
                actions = {
                    IconButton({ setShowClearDialog(true) }) {
                        Icon(Icons.Default.Delete, "Clear all games")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.game_icon),
                contentDescription = "Banner",
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.CenterHorizontally)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { setTitle(it) },
                label = { Text("Game Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { setDescription(it) },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = completed,
                    onCheckedChange = { setCompleted(it) }
                )
                Spacer(Modifier.width(8.dp))
                Text("Completed")
            }

            Button(
                onClick = {
                    val t = title.trim()
                    if (t.isBlank()) {
                        setError("Title is required.")
                        return@Button
                    }

                    games.add(Game(title = t, description = description.trim(), completed = completed))
                    saveGames(context, games)

                    setTitle("")
                    setDescription("")
                    setCompleted(false)
                    setError(null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Game")
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            HorizontalDivider()

            Text("My Games", fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(games, key = { it.id }) { g ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { _ ->
                                        setEditingGame(g)
                                    }
                                )
                            },
                        colors = if (g.completed) CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)) else CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(g.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))

                                IconButton({
                                    games.remove(g)
                                    saveGames(context, games)
                                }) {
                                    Icon(Icons.Default.Delete, "Delete game")
                                }
                            }

                            if (g.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(g.description)
                            }
                            if (g.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(g.notes, fontWeight = FontWeight.Light)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(if (g.completed) "Status: Completed" else "Status: Not completed")
                        }
                    }
                }
            }
        }
    }
}

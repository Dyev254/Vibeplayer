
package com.vibeplayer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

data class Song(val id: Long, val title: String, val artist: String, val uri: String)

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()

        setContent {
            var songs by remember { mutableStateOf(emptyList<Song>()) }
            var current by remember { mutableStateOf<Song?>(null) }
            var playing by remember { mutableStateOf(false) }
            var query by remember { mutableStateOf("") }

            fun loadSongs() {
                val list = mutableListOf<Song>()
                val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST
                )
                contentResolver.query(
                    collection, projection, null, null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val title = cursor.getString(titleCol) ?: "Unknown title"
                        val artist = cursor.getString(artistCol) ?: "Unknown artist"
                        val uri = android.content.ContentUris.withAppendedId(collection, id).toString()
                        list.add(Song(id, title, artist, uri))
                    }
                }
                songs = list
            }

            val permission = if (Build.VERSION.SDK_INT >= 33)
                Manifest.permission.READ_MEDIA_AUDIO
            else Manifest.permission.READ_EXTERNAL_STORAGE

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted -> if (granted) loadSongs() }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity, permission
                    ) == PackageManager.PERMISSION_GRANTED
                ) loadSongs()
                else permissionLauncher.launch(permission)
            }

            val filtered = songs.filter {
                it.title.contains(query, true) || it.artist.contains(query, true)
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFBB86FC),
                    background = Color(0xFF100D18),
                    surface = Color(0xFF211A2B)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF100D18))
                        .padding(20.dp)
                ) {
                    Text(
                        "VibePlayer",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBB86FC)
                    )
                    Text("Your music. Your vibe.", color = Color.LightGray)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Search your music") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))
                    Text("YOUR LIBRARY", color = Color.LightGray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (songs.isEmpty()) {
                        Text(
                            "No music found yet. Allow audio access and add MP3 files to your phone.",
                            color = Color.LightGray
                        )
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered) { song ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        current = song
                                        player?.setMediaItem(MediaItem.fromUri(song.uri))
                                        player?.prepare()
                                        player?.play()
                                        playing = true
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("♫", color = Color(0xFFBB86FC), fontSize = 26.sp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, color = Color.White, fontWeight = FontWeight.Medium)
                                    Text(song.artist, color = Color.LightGray, fontSize = 12.sp)
                                }
                                Text("▶", color = Color(0xFFBB86FC))
                            }
                            HorizontalDivider(color = Color.DarkGray)
                        }
                    }

                    current?.let { song ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF211A2B), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(song.artist, color = Color.LightGray, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(onClick = {
                                    if (playing) player?.pause() else player?.play()
                                    playing = !playing
                                }) {
                                    Text(if (playing) "Pause Ⅱ" else "Play ▶")
                                }
                                Spacer(Modifier.width(12.dp))
                                OutlinedButton(onClick = {
                                    player?.seekToNextMediaItem()
                                }) { Text("Next ⏭") }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    suspend fun getAllTracksOnce(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE id IN (:ids)")
    suspend fun getTracksByIds(ids: List<String>): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracksByIds(ids: List<String>)

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album WHERE id = :id")
    suspend fun updateMetadata(id: String, title: String, artist: String, album: String)

    @Query("UPDATE tracks SET lastPlayedAt = :lastPlayedAt, playCount = :playCount WHERE id = :id")
    suspend fun updatePlayStats(id: String, lastPlayedAt: Long, playCount: Int)

    @Query("UPDATE tracks SET plainLyrics = :plainLyrics, syncedLyrics = :syncedLyrics WHERE id = :id")
    suspend fun updateLyrics(id: String, plainLyrics: String?, syncedLyrics: String?)
}

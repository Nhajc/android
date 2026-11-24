package com.nhajc.android.data.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.nhajc.android.data.database.model.ArtistEntity
import com.nhajc.android.data.database.model.PlaylistEntity
import com.nhajc.android.data.database.model.PlaylistSongCrossRef
import com.nhajc.android.data.database.model.QueueItemEntity
import com.nhajc.android.data.database.model.ReleaseArtistCrossRef
import com.nhajc.android.data.database.model.ReleaseEntity
import com.nhajc.android.data.database.model.SongArtistCrossRef
import com.nhajc.android.data.database.model.SongEntity
import com.nhajc.android.data.database.model.UserEntity
import com.nhajc.android.data.database.model.WantedSongView

@Database(
    entities = [
        UserEntity::class,
        ArtistEntity::class,
        ReleaseEntity::class,
        ReleaseArtistCrossRef::class,
        SongEntity::class,
        SongArtistCrossRef::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        QueueItemEntity::class
    ],
    views = [WantedSongView::class],
    version = 1
)
@ColumnTypeConverters(Converters::class)
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    abstract fun artistDao(): ArtistDao

    abstract fun releaseDao(): ReleaseDao

    abstract fun songDao(): SongDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun queueDao(): QueueDao

    abstract fun libraryDao(): LibraryDao
}

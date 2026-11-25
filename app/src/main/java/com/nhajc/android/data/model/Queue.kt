package com.nhajc.android.data.model

data class QueuePosition(val index: Int = 0, val positionMs: Long = 0)

data class SavedQueue(val songs: List<Song>, val position: QueuePosition)

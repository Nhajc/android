package com.nhajc.android.ui.admin

import com.nhajc.android.data.model.AdminError
import com.nhajc.android.data.model.AdminException
import com.nhajc.android.data.model.UploadQueueItem
import com.nhajc.android.data.model.UploadStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Two workers hold their slots through HTTP upload AND server-side processing. */
internal class UploadQueueRunner(private val retryDelayMillis: Long = 2_000) {
    suspend fun <T> run(
        items: List<UploadQueueItem>,
        upload: suspend (UploadQueueItem, (Float?) -> Unit, () -> Unit) -> T,
        update: (UploadQueueItem) -> Unit,
        complete: (
            UploadQueueItem,
            T
        ) -> UploadQueueItem = { item, _ -> item.copy(status = UploadStatus.DONE, progress = 1f) }
    ) = coroutineScope {
        val pending = Channel<UploadQueueItem>(Channel.UNLIMITED)
        items.filter {
            it.status == UploadStatus.WAITING
        }.forEach { pending.trySend(it).getOrThrow() }
        pending.close()
        repeat(2) {
            launch {
                for (original in pending) {
                    var busyRetries = 0
                    while (true) {
                        update(original.copy(status = UploadStatus.UPLOADING, progress = 0f))
                        try {
                            val result = upload(
                                original,
                                { progress ->
                                    update(
                                        original.copy(
                                            status = UploadStatus.UPLOADING,
                                            progress = progress
                                        )
                                    )
                                },
                                {
                                    update(
                                        original.copy(
                                            status = UploadStatus.PROCESSING,
                                            progress = 1f
                                        )
                                    )
                                }
                            )
                            update(complete(original, result))
                            break
                        } catch (
                            exception: CancellationException
                        ) {
                            throw exception
                        } catch (exception: Exception) {
                            val error = (exception as? AdminException)?.error ?: AdminError.SERVER
                            // HTTP 429 guarantees this request was not processed.
                            // Never automatically replay an ambiguous network failure.
                            if (error == AdminError.BUSY && busyRetries++ < 3) {
                                update(original.copy(status = UploadStatus.WAITING))
                                delay(retryDelayMillis)
                            } else {
                                update(original.copy(status = UploadStatus.FAILED, error = error))
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}

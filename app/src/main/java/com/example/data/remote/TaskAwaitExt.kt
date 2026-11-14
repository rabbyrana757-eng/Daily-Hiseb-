package com.example.data.remote

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
  addOnCompleteListener { task ->
    if (task.isCanceled) {
      continuation.cancel()
    } else {
      val exception = task.exception
      if (exception != null) {
        continuation.resumeWithException(exception)
      } else {
        @Suppress("UNCHECKED_CAST")
        continuation.resume(task.result as T)
      }
    }
  }
}

package com.example.data.cloud

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class JarvisCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google sebelum mengakses Firestore.")
    }

    fun observeMessages(userId: String): Flow<List<CloudMessage>> = flow {
        require(userId.isNotBlank()) { "User ID tidak boleh kosong" }
        val path = "users/$userId/messages"
        emitAll(
            db.collection("users").document(userId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeDirectives(userId: String): Flow<List<CloudDirective>> = flow {
        require(userId.isNotBlank()) { "User ID tidak boleh kosong" }
        val path = "users/$userId/directives"
        emitAll(
            db.collection("users").document(userId).collection("directives")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudDirective::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addMessage(
        userId: String,
        role: String,
        text: String,
        actionType: String? = null,
        actionLabel: String? = null,
        actionPayload: String? = null,
        errorFlag: Boolean = false
    ): Result<String> {
        var docPath = "users/$userId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Akun pengguna berubah; menolak penulisan lintas akun." }
            val cleanText = text.trim().take(16000)
            require(cleanText.isNotEmpty()) { "Teks pesan tidak boleh kosong." }
            val safeRole = if (role == "user") "user" else "model"

            val docRef = db.collection("users").document(uid).collection("messages").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "role" to safeRole,
                "text" to cleanText,
                "actionType" to actionType?.take(64)?.takeIf { it.isNotBlank() },
                "actionLabel" to actionLabel?.take(160)?.takeIf { it.isNotBlank() },
                "actionPayload" to actionPayload?.take(4000)?.takeIf { it.isNotBlank() },
                "errorFlag" to errorFlag,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearMessages(userId: String): Result<Unit> {
        val collectionPath = "users/$userId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Akun pengguna berubah; menolak penghapusan." }
            val snapshot = db.collection("users").document(uid).collection("messages").get().await()
            val batch = db.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, collectionPath)
            Result.failure(e)
        }
    }

    suspend fun addDirective(
        userId: String,
        title: String,
        priority: String = "NORMAL"
    ): Result<String> {
        var docPath = "users/$userId/directives"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Akun pengguna berubah; menolak penulisan." }
            val cleanTitle = title.trim().take(240)
            require(cleanTitle.isNotEmpty()) { "Judul direktif tidak boleh kosong." }
            val safePriority = if (priority in listOf("HIGH", "NORMAL", "ROUTINE")) priority else "NORMAL"

            val docRef = db.collection("users").document(uid).collection("directives").document()
            docPath = docRef.path

            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "priority" to safePriority,
                "completed" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleDirective(userId: String, directive: CloudDirective): Result<Unit> {
        var docPath = "users/$userId/directives/${directive.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Akun pengguna berubah; menolak pembaruan." }
            require(directive.id.isNotBlank()) { "ID direktif tidak valid." }

            val docRef = db.collection("users").document(uid)
                .collection("directives").document(directive.id)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "completed" to !directive.completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDirective(userId: String, directiveId: String): Result<Unit> {
        var docPath = "users/$userId/directives/$directiveId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Akun pengguna berubah; menolak penghapusan." }
            require(directiveId.isNotBlank()) { "ID direktif tidak valid." }

            val docRef = db.collection("users").document(uid)
                .collection("directives").document(directiveId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}

package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE projectId = :projectId")
    fun getFilesForProject(projectId: Int): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Int): FileEntity?

    @Query("SELECT * FROM files WHERE projectId = :projectId AND isFolder = 0")
    suspend fun getEditorFiles(projectId: Int): List<FileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Query("DELETE FROM files WHERE id = :id")
    suspend fun deleteFileById(id: Int)

    @Query("UPDATE files SET content = :content WHERE id = :id")
    suspend fun updateFileContent(id: Int, content: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getMessagesForProject(projectId: Int): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    suspend fun clearMessagesForProject(projectId: Int)
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_items ORDER BY timestamp DESC")
    fun getAllKnowledgeItems(): Flow<List<KnowledgeItem>>

    @Query("SELECT * FROM knowledge_items WHERE category = :category ORDER BY timestamp DESC")
    fun getKnowledgeItemsByCategory(category: String): Flow<List<KnowledgeItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeItem(item: KnowledgeItem): Long

    @Query("DELETE FROM knowledge_items WHERE id = :id")
    suspend fun deleteKnowledgeItemById(id: Int)
}

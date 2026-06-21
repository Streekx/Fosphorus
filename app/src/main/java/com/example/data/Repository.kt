package com.example.data

import kotlinx.coroutines.flow.Flow

class IDERepository(private val db: AppDatabase) {
    val projects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val allKnowledge: Flow<List<KnowledgeItem>> = db.knowledgeDao().getAllKnowledgeItems()

    suspend fun getProjectById(id: Int) = db.projectDao().getProjectById(id)
    suspend fun insertProject(project: ProjectEntity): Long = db.projectDao().insertProject(project)
    suspend fun deleteProject(id: Int) = db.projectDao().deleteProjectById(id)

    fun getFilesForProject(projectId: Int): Flow<List<FileEntity>> = db.fileDao().getFilesForProject(projectId)
    suspend fun getFileById(id: Int) = db.fileDao().getFileById(id)
    suspend fun getEditorFiles(projectId: Int) = db.fileDao().getEditorFiles(projectId)
    suspend fun insertFile(file: FileEntity): Long = db.fileDao().insertFile(file)
    suspend fun deleteFile(id: Int) = db.fileDao().deleteFileById(id)
    suspend fun updateFileContent(id: Int, content: String) = db.fileDao().updateFileContent(id, content)

    fun getMessagesForProject(projectId: Int): Flow<List<ChatMessageEntity>> = db.chatDao().getMessagesForProject(projectId)
    suspend fun insertMessage(message: ChatMessageEntity): Long = db.chatDao().insertMessage(message)
    suspend fun clearMessages(projectId: Int) = db.chatDao().clearMessagesForProject(projectId)

    fun getKnowledgeByCategory(category: String): Flow<List<KnowledgeItem>> = db.knowledgeDao().getKnowledgeItemsByCategory(category)
    suspend fun insertKnowledge(item: KnowledgeItem): Long = db.knowledgeDao().insertKnowledgeItem(item)
    suspend fun deleteKnowledge(id: Int) = db.knowledgeDao().deleteKnowledgeItemById(id)
}

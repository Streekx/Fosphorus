package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val language: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val name: String,
    val path: String,
    val content: String,
    val language: String,
    val isFolder: Boolean = false,
    val parentId: Int = 0
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val sender: String, // "user", "assistant", "agent"
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String = "gemini-3.5-flash",
    val status: String = "success" // "sending", "success", "failed"
)

@Entity(tableName = "knowledge_items")
data class KnowledgeItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "prompts", "snippets", "notes", "architecture"
    val content: String,
    val tags: String = "",
    val timestamp: Long = System.currentTimeMillis()
)


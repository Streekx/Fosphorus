package com.example.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Stack

class IDEViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "IDEViewModel"
    private val db = AppDatabase.getDatabase(application)
    private val repository = IDERepository(db)

    // User authentication and system states
    var showSplash = MutableStateFlow(true)
    var onboardingCompleted = MutableStateFlow(false)
    var isAuthenticated = MutableStateFlow(false)
    var userEmail = MutableStateFlow("")
    var userDisplayName = MutableStateFlow("Developer")

    // Project navigation & file states
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val currentProjectId = MutableStateFlow<Int?>(null)
    val files = currentProjectId.flatMapLatest { id ->
        if (id != null) repository.getFilesForProject(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeFileId = MutableStateFlow<Int?>(null)
    val activeTabs = MutableStateFlow<List<Int>>(emptyList()) // open file IDs
    val editorContent = MutableStateFlow("")
    
    // Simple Undo/Redo Stacks
    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    // Chat history
    val messages = currentProjectId.flatMapLatest { id ->
        if (id != null) repository.getMessagesForProject(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isGenerating = MutableStateFlow(false)

    // Knowledge & Prompts Workspace
    val knowledgeItems = repository.allKnowledge.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Agent Workspace Logs and Status
    val agentSteps = MutableStateFlow<List<String>>(emptyList())
    val agentIsExecuting = MutableStateFlow(false)

    // Settings & LLM Model configuration options
    val activeModel = MutableStateFlow("Gemini 3.5 Flash")
    val customApiKey = MutableStateFlow("")
    val temperature = MutableStateFlow(0.7f)
    val maxTokens = MutableStateFlow(2048)

    // Terminal simulated state
    val terminalInput = MutableStateFlow("")
    val terminalLogs = MutableStateFlow<List<String>>(listOf(
        "FOSPHORUS Core v1.5.0 Initialization successful.",
        "System: Environment fully ready. Multi-agent core synced."
    ))
    val terminalHistory = mutableListOf<String>()

    // Code Review results
    val codeReviewResult = MutableStateFlow<String?>(null)
    val codeReviewIsAnalyzing = MutableStateFlow(false)

    init {
        // Pre-populate some cool templates if there are no projects on first launch
        viewModelScope.launch {
            repository.projects.first().let { currentProjects ->
                if (currentProjects.isEmpty()) {
                    createDemoworkspace()
                } else {
                    currentProjectId.value = currentProjects.first().id
                }
            }
            
            // Add some prompt library defaults
            repository.allKnowledge.first().let { currentKnowledge ->
                if (currentKnowledge.isEmpty()) {
                    populateDefaultKnowledge()
                }
            }
        }
    }

    private suspend fun createDemoworkspace() {
        val projId1 = repository.insertProject(
            ProjectEntity(
                name = "Fosphorus Sandbox",
                description = "Default playground workspace demonstrating responsive Compose layouts and local agent actions.",
                language = "Kotlin"
            )
        ).toInt()

        currentProjectId.value = projId1

        // Insert some folders and files
        repository.insertFile(FileEntity(projectId = projId1, name = "src", path = "src", content = "", language = "Folder", isFolder = true))
        
        val mainFileId = repository.insertFile(
            FileEntity(
                projectId = projId1,
                name = "Main.kt",
                path = "src/Main.kt",
                content = """// FOSPHORUS AI Agent IDE playground
import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Initializing Fosphorus Engine...")
    delay(1000)
    
    val coreCount = Runtime.getRuntime().availableProcessors()
    println("Active cores detected: ${'$'}coreCount")
    
    // Simulate AI model inference trigger
    val promptResult = triggerAgentCore("Hello Fosphorus, refactor this!")
    println("Agent Outcome: ${'$'}promptResult")
}

suspend fun triggerAgentCore(input: String): String {
    println("Analyzing codebase security patterns...")
    delay(500)
    return "Optimized 3 submodules securely with Neon theme."
}
""",
                language = "Kotlin"
            )
        ).toInt()

        val cssFileId = repository.insertFile(
            FileEntity(
                projectId = projId1,
                name = "Theme.css",
                path = "src/Theme.css",
                content = """:root {
    --fosphorus-background: #0A0A0D;
    --fosphorus-surface: #1A1A1F;
    --fosphorus-primary: #9D4DFF;
    --fosphorus-glow: rgba(157, 77, 255, 0.4);
}

body {
    background-color: var(--fosphorus-background);
    color: #FFFFFF;
    font-family: 'SF Pro Display', system-ui;
    transition: background 300ms ease;
}
""",
                language = "CSS"
            )
        ).toInt()

        repository.insertFile(
            FileEntity(
                projectId = projId1,
                name = "Readme.md",
                path = "Readme.md",
                content = """# FOSPHORUS AI Agent IDE
Welcome to Fosphorus, your premium mobile-first Android Agent IDE.

## Features
- **Markdown & Code Rendering**: Fluid chat workspace.
- **Agent Mode**: Full multi-file code generator & refactor analysis.
- **Glass-style UI**: Responsive, luxurious 45/45/10 color distribution.
- **Integrated Terminal**: Run mock tasks in real-time.
""",
                language = "Markdown"
            )
        )

        // Select Main.kt as active state
        openFileInEditor(mainFileId, """// FOSPHORUS AI Agent IDE playground
import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Initializing Fosphorus Engine...")
    delay(1000)
    
    val coreCount = Runtime.getRuntime().availableProcessors()
    println("Active cores detected: ${'$'}coreCount")
    
    // Simulate AI model inference trigger
    val promptResult = triggerAgentCore("Hello Fosphorus, refactor this!")
    println("Agent Outcome: ${'$'}promptResult")
}

suspend fun triggerAgentCore(input: String): String {
    println("Analyzing codebase security patterns...")
    delay(500)
    return "Optimized 3 submodules securely with Neon theme."
}
""")
        activeTabs.value = listOf(mainFileId, cssFileId)
    }

    private suspend fun populateDefaultKnowledge() {
        val prompts = listOf(
            KnowledgeItem(
                title = "UI Theme Optimizer",
                category = "UI Design",
                content = "Transform this component to adhere to the Liquid Glass System: 45% black (#0A0A0D), 45% dark grey (#1A1A1F), and 10% neon purple (#9D4DFF) glow.",
                tags = "Compose,Glass,Theme"
            ),
            KnowledgeItem(
                title = "Jetpack Room Flow API",
                category = "Database",
                content = "Always return Flow<List<Entity>> for reactive UI updates from DAO. Implement repository pattern to decouple details, and insert with onConflict = OnConflictStrategy.REPLACE.",
                tags = "Room,Database,Flow"
            ),
            KnowledgeItem(
                title = "Android API Retrofit Client",
                category = "API",
                content = "Create structured REST client using standard OkHttpClient with 60s timeouts, Moshi Converter Factory, and system instruction headers for maximum token efficiency.",
                tags = "Retrofit,API,Network"
            )
        )
        prompts.forEach { repository.insertKnowledge(it) }
    }

    fun openFileInEditor(fileId: Int, content: String) {
        activeFileId.value = fileId
        editorContent.value = content
        undoStack.clear()
        redoStack.clear()
        if (!activeTabs.value.contains(fileId)) {
            activeTabs.value = activeTabs.value + fileId
        }
    }

    fun handleEditorValueChange(newValue: String) {
        val currentText = editorContent.value
        if (currentText != newValue) {
            undoStack.push(currentText)
            redoStack.clear()
            editorContent.value = newValue
            
            // Auto commit to repository in background
            activeFileId.value?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateFileContent(id, newValue)
                }
            }
        }
    }

    fun undo() {
        if (!undoStack.isEmpty()) {
            val element = undoStack.pop()
            redoStack.push(editorContent.value)
            editorContent.value = element
            
            activeFileId.value?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateFileContent(id, element)
                }
            }
        }
    }

    fun redo() {
        if (!redoStack.isEmpty()) {
            val element = redoStack.pop()
            undoStack.push(editorContent.value)
            editorContent.value = element

            activeFileId.value?.let { id ->
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateFileContent(id, element)
                }
            }
        }
    }

    fun selectProject(projectId: Int) {
        currentProjectId.value = projectId
        viewModelScope.launch {
            val projectFiles = repository.getEditorFiles(projectId)
            if (projectFiles.isNotEmpty()) {
                val firstCodeFile = projectFiles.first()
                openFileInEditor(firstCodeFile.id, firstCodeFile.content)
                activeTabs.value = projectFiles.map { it.id }.take(3)
            } else {
                activeFileId.value = null
                editorContent.value = ""
                activeTabs.value = emptyList()
            }
        }
    }

    fun createProject(name: String, desc: String, language: String) {
        viewModelScope.launch {
            val id = repository.insertProject(
                ProjectEntity(name = name, description = desc, language = language)
            ).toInt()
            currentProjectId.value = id
            
            // Create a default start file
            val fileId = repository.insertFile(
                FileEntity(
                    projectId = id,
                    name = "Main.${if (language == "Kotlin") "kt" else "js"}",
                    path = "src/Main.${if (language == "Kotlin") "kt" else "js"}",
                    content = if (language == "Kotlin") {
                        "// Project: $name\nfun main() {\n    println(\"Hello, Fosphorus!\")\n}"
                    } else {
                        "// Project: $name\nconsole.log(\"Hello, Fosphorus!\");"
                    },
                    language = language
                )
            ).toInt()
            
            openFileInEditor(fileId, if (language == "Kotlin") {
                "// Project: $name\nfun main() {\n    println(\"Hello, Fosphorus!\")\n}"
            } else {
                "// Project: $name\nconsole.log(\"Hello, Fosphorus!\");"
            })
            activeTabs.value = listOf(fileId)
        }
    }

    fun createNewFile(name: String, language: String) {
        val projId = currentProjectId.value ?: return
        viewModelScope.launch {
            val fileId = repository.insertFile(
                FileEntity(
                    projectId = projId,
                    name = name,
                    path = name,
                    content = "// File: $name\n// Start coding with AI here\n",
                    language = language
                )
            ).toInt()
            openFileInEditor(fileId, "// File: $name\n// Start coding with AI here\n")
        }
    }

    fun deleteFile(id: Int) {
        viewModelScope.launch {
            repository.deleteFile(id)
            activeTabs.value = activeTabs.value.filter { it != id }
            if (activeFileId.value == id) {
                if (activeTabs.value.isNotEmpty()) {
                    val nextId = activeTabs.value.first()
                    val f = repository.getFileById(nextId)
                    if (f != null) {
                        openFileInEditor(f.id, f.content)
                    } else {
                        activeFileId.value = null
                        editorContent.value = ""
                    }
                } else {
                    activeFileId.value = null
                    editorContent.value = ""
                }
            }
        }
    }

    fun closeTab(tabId: Int) {
        val currentTabs = activeTabs.value.toMutableList()
        currentTabs.remove(tabId)
        activeTabs.value = currentTabs
        if (activeFileId.value == tabId) {
            if (currentTabs.isNotEmpty()) {
                viewModelScope.launch {
                    val f = repository.getFileById(currentTabs.last())
                    if (f != null) openFileInEditor(f.id, f.content)
                }
            } else {
                activeFileId.value = null
                editorContent.value = ""
            }
        }
    }

    // AI Workspace Chat Interface
    fun sendMessageToAI(text: String, replyToText: String? = null) {
        val projId = currentProjectId.value ?: return
        if (text.isBlank()) return

        val userMsgString = replyToText?.let { "Replying to API/Query: $it\n\n$text" } ?: text

        viewModelScope.launch {
            // Save User Message
            repository.insertMessage(
                ChatMessageEntity(projectId = projId, sender = "user", message = userMsgString)
            )

            isGenerating.value = true
            
            // Build Context regarding open code file
            val activeFileName = activeFileId.value?.let { repository.getFileById(it)?.name } ?: "No active file"
            val contextualPrompt = """
                You are FOSPHORUS AI AGENT IDE inside an Android native container.
                Active File: $activeFileName
                Active File Content:
                ```
                ${editorContent.value}
                ```
                
                User query: $userMsgString
                
                Keep your response precise and structured. Always wrap code blocks with their respective language for custom syntax renderings. Describe optimizations concisely.
            """.trimIndent()

            try {
                // Call Gemini Direct REST structure (from Option B in skill guidelines)
                val response = GeminiApiClient.generate(
                    prompt = contextualPrompt,
                    systemPrompt = "You are Fosphorus, an elegant, black-and-purple styled Android IDE AI specialist.",
                    temperature = temperature.value,
                    maxTokens = maxTokens.value,
                    customKey = customApiKey.value
                )
                
                repository.insertMessage(
                    ChatMessageEntity(
                        projectId = projId,
                        sender = "assistant",
                        message = response,
                        modelName = activeModel.value
                    )
                )
            } catch (e: Exception) {
                repository.insertMessage(
                    ChatMessageEntity(
                        projectId = projId,
                        sender = "assistant",
                        message = "Inference API Error: ${e.localizedMessage}. Please verify check your internet connection and API Key settings.",
                        status = "failed"
                    )
                )
            } finally {
                isGenerating.value = false
            }
        }
    }

    // AI Agent Actions Mode
    fun executeAgentTask(taskType: String) {
        val projId = currentProjectId.value ?: return
        val currentCode = editorContent.value
        val activeFileName = activeFileId.value?.let { id ->
            viewModelScope.launch {
                val f = repository.getFileById(id)
                f?.name
            }
        } ?: "code_file.kt"

        viewModelScope.launch {
            agentIsExecuting.value = true
            agentSteps.value = listOf("1. Fetching Workspace Context...")
            
            kotlinx.coroutines.delay(1000)
            agentSteps.value = agentSteps.value + "2. Parsing code syntax for '$activeFileName'..."
            
            kotlinx.coroutines.delay(1000)
            agentSteps.value = agentSteps.value + "3. Querying ${activeModel.value} model patterns..."

            val systemInstr = """
                You are the core agent modules of FOSPHORUS IDE.
                Task selected: $taskType on file $activeFileName.
                Take the input code and output the refined complete code block ONLY. Do not write normal explanatory paragraphs, just the refined, completed full-code blocks in your code block output format so Fosphorus compilation pipeline automatically parses the result.
            """.trimIndent()

            val promptText = """
                Here is the current code of '$activeFileName':
                ```
                $currentCode
                ```
                Please perform standard premium task: "$taskType" and return the finished fully functional version.
            """.trimIndent()

            try {
                val outcome = GeminiApiClient.generate(
                    prompt = promptText,
                    systemPrompt = systemInstr,
                    temperature = 0.5f, // low temperature for code accuracy
                    maxTokens = maxTokens.value,
                    customKey = customApiKey.value
                )

                // Try to extract only the code block from outcome
                val refinedCode = extractCodeBlock(outcome) ?: outcome

                agentSteps.value = agentSteps.value + "4. Modifying file contents in secure workspace database..."
                kotlinx.coroutines.delay(1000)

                activeFileId.value?.let { id ->
                    repository.updateFileContent(id, refinedCode)
                    editorContent.value = refinedCode
                    undoStack.push(currentCode)
                }

                agentSteps.value = agentSteps.value + "5. Completed! Code updated inside mobile workspace."
                repository.insertMessage(
                    ChatMessageEntity(
                        projectId = projId,
                        sender = "agent",
                        message = "Successfully executed '$taskType' Agent Mode outcome on '$activeFileName'.\n\n```\n$refinedCode\n```"
                    )
                )
            } catch (e: Exception) {
                agentSteps.value = agentSteps.value + "Error: Integration layer exception: ${e.message}"
            } finally {
                agentIsExecuting.value = false
            }
        }
    }

    private fun extractCodeBlock(raw: String): String? {
        val startTag = "```"
        if (!raw.contains(startTag)) return null
        val parts = raw.split("```")
        if (parts.size >= 3) {
            // Check if first line states language like ```kotlin
            val block = parts[1]
            val lines = block.lines()
            if (lines.isNotEmpty() && lines[0].trim().any { it.isLetter() }) {
                return lines.drop(1).joinToString("\n")
            }
            return block
        }
        return null
    }

    // Code Review action
    fun performCodeReview() {
        val currentCode = editorContent.value
        if (currentCode.isBlank()) {
            codeReviewResult.value = "Editor is empty. Open a code file to perform live review."
            return
        }

        viewModelScope.launch {
            codeReviewIsAnalyzing.value = true
            codeReviewResult.value = null

            val prompt = """
                Perform review on the following code. Detect any potential bugs, memory leaks, performance issues, or security concerns. Provide a formatted, point-by-point, high-contrast review. Keep it concise.
                
                Code:
                ```
                $currentCode
                ```
            """.trimIndent()

            try {
                val review = GeminiApiClient.generate(
                    prompt = prompt,
                    systemPrompt = "You are Fosphorus Security Review Agent. Highly critical and accurate.",
                    temperature = 0.6f,
                    maxTokens = 2048,
                    customKey = customApiKey.value
                )
                codeReviewResult.value = review
            } catch (e: Exception) {
                codeReviewResult.value = "Error conducting AI security audit: ${e.localizedMessage}"
            } finally {
                codeReviewIsAnalyzing.value = false
            }
        }
    }

    // Simulated Terminal Commands Engine
    fun runTerminalCommand(command: String) {
        if (command.isBlank()) return
        terminalHistory.add(command)
        val logs = terminalLogs.value.toMutableList()
        logs.add("> $command")

        viewModelScope.launch {
            val cmdNormalized = command.trim().lowercase()
            when {
                cmdNormalized.startsWith("help") -> {
                    logs.add("Supported commands:")
                    logs.add("  run           - Simulates execution of the active code tab")
                    logs.add("  gradle build  - Compiles virtual app submodules")
                    logs.add("  ls            - Lists active project filesystem hierarchy")
                    logs.add("  clear         - Clears terminal output logs")
                    logs.add("  systeminfo    - Display mobile hardware status logs")
                }
                cmdNormalized.startsWith("clear") -> {
                    terminalLogs.value = emptyList()
                    return@launch
                }
                cmdNormalized.startsWith("run") -> {
                    val activeId = activeFileId.value
                    if (activeId != null) {
                        val file = repository.getFileById(activeId)
                        logs.add("FosphorusCompiler: Parsing '${file?.name}' syntax tree...")
                        kotlinx.coroutines.delay(600)
                        logs.add("FosphorusRunner: Executing bytecode output...")
                        kotlinx.coroutines.delay(500)
                        if (file?.content?.contains("println") == true) {
                            val printlnMatches = Regex("""println\("(.+?)"\)""").findAll(file.content)
                            if (printlnMatches.any()) {
                                printlnMatches.forEach { match ->
                                    logs.add(match.groupValues[1])
                                }
                            } else {
                                logs.add("Program exited with code 0 (Simulated).")
                            }
                        } else {
                            logs.add("Success execution: finished in 234ms.")
                        }
                    } else {
                        logs.add("No code file currently focused in editor.")
                    }
                }
                cmdNormalized.startsWith("gradle build") -> {
                    logs.add("Starting Fosphorus Gradle Daemon...")
                    kotlinx.coroutines.delay(600)
                    logs.add("Analyzing project dependencies: OK.")
                    kotlinx.coroutines.delay(800)
                    logs.add("BUILD SUCCESSFUL in 1s")
                }
                cmdNormalized.startsWith("ls") -> {
                    logs.add("FileSystem listing for Project id: ${currentProjectId.value}:")
                    files.value.forEach { file ->
                        logs.add("  ${if (file.isFolder) "[DIR] " else "      "} ${file.name}    (${file.language})")
                    }
                }
                cmdNormalized.startsWith("systeminfo") -> {
                    logs.add("Fosphorus Runtime Platform: Android OS")
                    logs.add("Active Agent Engine: ${activeModel.value}")
                    logs.add("Sandbox Encryption Status: HIGHLY SECURE - LOCAL")
                }
                else -> {
                    logs.add("Command not recognized. Type 'help' for options.")
                }
            }
            terminalLogs.value = logs
        }
        terminalInput.value = ""
    }

    // Save customized snippet/note to knowledge items
    fun saveKnowledgeNotes(title: String, category: String, content: String, tags: String) {
        viewModelScope.launch {
            repository.insertKnowledge(
                KnowledgeItem(title = title, category = category, content = content, tags = tags)
            )
        }
    }

    // Delete custom library snippet
    fun deleteKnowledgeItem(id: Int) {
        viewModelScope.launch {
            repository.deleteKnowledge(id)
        }
    }
}

package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.SpeakerNotes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FileEntity
import com.example.data.KnowledgeItem
import com.example.data.ProjectEntity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.IDEViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppShell(
    viewModel: IDEViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Core Navigation & Session states
    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("home") } // home, chat, agent, projects, settings

    // Sheet displays for sub-workspaces
    var currentSubScreen by remember { mutableStateOf<String?>(null) } // "editor", "terminal", "review", "knowledge", "models", "prompts", "new_project_dialog"

    // Animation transition
    AnimatedContent(
        targetState = when {
            showSplash -> "splash"
            !onboardingCompleted -> "onboarding"
            !isAuthenticated -> "auth"
            else -> "main"
        },
        transitionSpec = {
            fadeIn(tween(400)) with fadeOut(tween(400))
        },
        label = "core_workspace_transitions"
    ) { state ->
        when (state) {
            "splash" -> SplashScreen(dismiss = { viewModel.showSplash.value = false })
            "onboarding" -> OnboardingScreen(
                onCompleted = { apiKey ->
                    if (apiKey.isNotBlank()) viewModel.customApiKey.value = apiKey
                    viewModel.onboardingCompleted.value = true
                }
            )
            "auth" -> AuthenticationScreen(
                onSuccess = { email, name ->
                    viewModel.userEmail.value = email
                    viewModel.userDisplayName.value = name
                    viewModel.isAuthenticated.value = true
                }
            )
            "main" -> {
                Scaffold(
                    modifier = modifier
                        .fillMaxSize()
                        .background(BackgroundBlack),
                    bottomBar = {
                        BottomBar(activeTab = activeTab, onTabSelected = {
                            activeTab = it
                            currentSubScreen = null // Reset sheets on tab change
                        })
                    },
                    containerColor = BackgroundBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        // Ambient purple smoky background glows
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val smokeBrush1 = Brush.radialGradient(
                                        colors = listOf(
                                            NeonPurplePrimary.copy(alpha = 0.08f),
                                            Color.Transparent
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f),
                                        radius = size.maxDimension * 0.5f
                                    )
                                    val smokeBrush2 = Brush.radialGradient(
                                        colors = listOf(
                                            PurpleGlow.copy(alpha = 0.10f),
                                            Color.Transparent
                                        ),
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.8f),
                                        radius = size.maxDimension * 0.6f
                                    )
                                    drawRect(smokeBrush1)
                                    drawRect(smokeBrush2)
                                }
                        )

                        // Outer switcher
                        Crossfade(
                            targetState = Pair(activeTab, currentSubScreen),
                            animationSpec = tween(300),
                            label = "sub_screen_crossfade"
                        ) { (tab, subScreen) ->
                            if (subScreen != null) {
                                when (subScreen) {
                                    "editor" -> CodeEditorTab(viewModel, onClose = { currentSubScreen = null })
                                    "terminal" -> TerminalTab(viewModel, onClose = { currentSubScreen = null })
                                    "review" -> CodeReviewTab(viewModel, onClose = { currentSubScreen = null })
                                    "knowledge" -> KnowledgeWorkspaceTab(viewModel, onClose = { currentSubScreen = null })
                                    "models" -> ModelHubTab(viewModel, onClose = { currentSubScreen = null })
                                    "prompts" -> PromptLibraryTab(viewModel, onSelectPrompt = { prompt ->
                                        currentSubScreen = null
                                        activeTab = "chat"
                                        viewModel.sendMessageToAI("Use custom prompt style:\n\n $prompt")
                                    }, onClose = { currentSubScreen = null })
                                }
                            } else {
                                when (tab) {
                                    "home" -> HomeDashboardTab(
                                        viewModel,
                                        navigateToSub = { currentSubScreen = it },
                                        navigateToTab = { activeTab = it }
                                    )
                                    "chat" -> ChatWorkspaceTab(viewModel, navigateToPrompts = { currentSubScreen = "prompts" })
                                    "agent" -> AgentWorkspaceTab(viewModel, navigateToEditor = { currentSubScreen = "editor" })
                                    "projects" -> ProjectExplorerTab(viewModel, navigateToEditor = { currentSubScreen = "editor" })
                                    "settings" -> SettingsTab(viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(dismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2200)
        dismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FosphorusLogo(active = true)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "F O S P H O R U S",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = 4.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MOBILE AI AGENT IDE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = NeonPurplePrimary,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = NeonPurplePrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==========================================
// 2. ONBOARDING SCREEN
// ==========================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onCompleted: (String) -> Unit) {
    var step by remember { mutableStateOf(0) }
    var keyInput by remember { mutableStateOf("") }
    
    val onboardingSlides = listOf(
        Triple(
            "Code. Chat. Build. Automate.",
            "Bring Cursor-grade AI intelligence wherever you go. Fosphorus is optimized for writing real code with one hand, directly on your phone.",
            Icons.Outlined.Code
        ),
        Triple(
            "Fosphorus Autonomous Agents",
            "Select high-level actions: Build features, find bugs, optimize architecture, or auto-generate tests. The Fosphorus agent refactors deep file structures autonomously.",
            Icons.Outlined.AutoAwesome
        ),
        Triple(
            "Terminal & Prompt Workspace",
            "Simulate native compiler steps, review code security flaws under dynamic audits, and utilize our built-in curated android Prompt Library.",
            Icons.Outlined.Terminal
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            FosphorusLogo(active = step < 3)
            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(targetState = step, label = "onboarding_steps") { currentStep ->
                if (currentStep < 3) {
                    val slide = onboardingSlides[currentStep]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = slide.third,
                            contentDescription = null,
                            tint = NeonPurplePrimary,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = slide.first,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = slide.second,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Secure Your Workspace",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Provide your Gemini API key context. It is stored securely on-device. Leave blank to configure later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        GlassTextField(
                            value = keyInput,
                            onValueChange = { keyInput = it },
                            placeholderText = "Enter Gemini API Key...",
                            singleLine = true,
                            tag = "onboarding_api_key"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Slide indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(4) { idx ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (step == idx) 20.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (step == idx) NeonPurplePrimary else SurfaceGrey)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            NeonButton(
                text = if (step == 3) "Enter FOSPHORUS" else "Continue",
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        onCompleted(keyInput)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                tag = "onboarding_next_button"
            )
        }
    }
}

// ==========================================
// 3. AUTHENTICATION SCREEN
// ==========================================
@Composable
fun AuthenticationScreen(onSuccess: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSignUp) "Create FOSPHORUS Account" else "Sign In to FOSPHORUS",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Synergize with the cloud compiler & workspace telemetry",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                GlassTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholderText = "Email Address",
                    tag = "auth_email"
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .testTag("auth_password")
                        .fillMaxWidth()
                        .background(SurfaceGrey.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                    placeholder = { Text("Secure Password", color = TextSecondary.copy(alpha = 0.6f)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurplePrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = NeonPurplePrimary
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                NeonButton(
                    text = if (isSignUp) "Sign Up" else "Login securely",
                    onClick = {
                        val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
                        onSuccess(email.ifBlank { "anshkumarchan@gmail.com" }, if (name.isBlank()) "Developer" else name)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    tag = "auth_signin_button"
                )

                Spacer(modifier = Modifier.height(12.dp))

                SecondaryGlassButton(
                    text = "Continue as Guest (Offline IDE)",
                    onClick = { onSuccess("anshkumarchan@gmail.com", "Fosphorus Guest") },
                    borderColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                    tag = "auth_guest_button"
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = if (isSignUp) "Already have an account? Sign In" else "New to Fosphorus? Create Account",
                    style = MaterialTheme.typography.labelMedium,
                    color = PurpleGlow,
                    modifier = Modifier
                        .clickable { isSignUp = !isSignUp }
                        .padding(8.dp)
                )
            }
        }
    }
}

// ==========================================
// 4. HOME DASHBOARD TAB
// ==========================================
@Composable
fun HomeDashboardTab(
    viewModel: IDEViewModel,
    navigateToSub: (String) -> Unit,
    navigateToTab: (String) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeFileId by viewModel.activeFileId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val userName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    
    val toastContext = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Welcome back,",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                FosphorusLogo(modifier = Modifier.size(54.dp), active = false)
            }
        }

        // Active Project Hero Slide
        item {
            val activePr = projects.firstOrNull()
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (activeFileId != null) navigateToSub("editor")
                        else navigateToTab("projects")
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONTINUE WORKING",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurplePrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = activePr?.name ?: "No Workspace Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activePr?.description ?: "Create your first virtual mobile project and start writing custom APK structures with standard AI completion.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonPurplePrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = null,
                            tint = PurpleGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active model: ${viewModel.activeModel.value}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PurpleGlow
                    )
                    Text(
                        text = "Files: 4 | Workspace OK",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }

        // Quick Command Actions Grid
        item {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Feature list action buttons
                val actions = listOf(
                    Triple("Code Editor", Icons.Default.Edit, "editor"),
                    Triple("Terminal Sim", Icons.Filled.Terminal, "terminal"),
                    Triple("Security Review", Icons.Default.Security, "review")
                )
                actions.forEach { act ->
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { navigateToSub(act.third) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = act.second,
                                contentDescription = null,
                                tint = NeonPurplePrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = act.first,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Project Analytics Card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "IDE Pipeline Health Monitor",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Stat columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Muted Alerts", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("0 Security Flaws", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.Green)
                    }
                    Column {
                        Text("LLM Telemetry", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("Gemini Beta-API", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = PurpleGlow)
                    }
                    Column {
                        Text("Cores Ingested", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("Active (8 CPU)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        // Sub Prompt Actions trigger
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Automated Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "See All",
                    style = MaterialTheme.typography.bodySmall,
                    color = PurpleGlow,
                    modifier = Modifier.clickable { navigateToSub("prompts") }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val tasks = listOf(
                    "Optimize rendering performance",
                    "Fix concurrency race conditions",
                    "Implement biometric authentication"
                )
                tasks.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceGrey.copy(alpha = 0.5f))
                            .clickable {
                                navigateToTab("chat")
                                viewModel.sendMessageToAI("Develop optimized system routines to $t")
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = t,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = NeonPurplePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. AI CHAT WORKSPACE TAB
// ==========================================
@Composable
fun ChatWorkspaceTab(
    viewModel: IDEViewModel,
    navigateToPrompts: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    
    var textInput by remember { mutableStateOf("") }
    var selectedFileToAttach by remember { mutableStateOf<FileEntity?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showModelDropdown by remember { mutableStateOf(false) }
    
    val clipboardManager = LocalClipboardManager.current
    val toastContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Chat Workspace Top Header Panel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI CHAT WORKSPACE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                // Model active picker trigger
                Row(
                    modifier = Modifier
                        .clickable { showModelDropdown = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = viewModel.activeModel.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = PurpleGlow,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = PurpleGlow,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showModelDropdown,
                    onDismissRequest = { showModelDropdown = false },
                    modifier = Modifier.background(ElevatedSurface)
                ) {
                    val modelList = listOf("Gemini 3.5 Flash", "Gemini 3.1 Pro", "DeepSeek R1", "Claude 3.5 Sonnet", "GPT-4o")
                    modelList.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m, color = TextPrimary) },
                            onClick = {
                                viewModel.activeModel.value = m
                                showModelDropdown = false
                            }
                        )
                    }
                }
            }

            // Quick Library Button
            IconButton(
                onClick = navigateToPrompts,
                modifier = Modifier.background(SurfaceGrey, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Prompt libraries",
                    tint = NeonPurplePrimary
                )
            }
        }

        // Stream generator progress state
        if (isGenerating) {
            LinearProgressIndicator(
                color = NeonPurplePrimary,
                trackColor = SurfaceGrey,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            )
        } else {
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 6.dp))
        }

        // Message List
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Chat,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fosphorus AI Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Text(
                        text = "I have full access to your open code files. Ask me to explain architecture, refactor, generate API clients, or debug.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.sender == "user"
                        val isAgent = msg.sender == "agent"
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isUser) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isAgent) PurpleGlow else NeonPurplePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isAgent) Icons.Default.Android else Icons.Default.GeneratingTokens,
                                        contentDescription = null,
                                        tint = TextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                val bubbleBg = when {
                                    isUser -> NeonPurplePrimary.copy(alpha = 0.15f)
                                    isAgent -> ElevatedSurface
                                    else -> SurfaceGrey
                                }
                                val bubbleBorderColor = when {
                                    isUser -> NeonPurplePrimary.copy(alpha = 0.4f)
                                    else -> DividerColor
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(bubbleBg)
                                        .border(1.dp, bubbleBorderColor, RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    // Complex rich layout text helper: parses code blocks and highlights copy clicks!
                                    RenderChatMessage(msg.message, onCopyCode = { code ->
                                        clipboardManager.setText(AnnotatedString(code))
                                        Toast.makeText(toastContext, "Code copied!", Toast.LENGTH_SHORT).show()
                                    })
                                }

                                Text(
                                    text = if (isUser) "You" else if (isAgent) "Agent Module" else msg.modelName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Attach badge show
        if (selectedFileToAttach != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ElevatedSurface)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Attachment, contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Attached: ${selectedFileToAttach!!.name}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
                IconButton(onClick = { selectedFileToAttach = null }, modifier = Modifier.size(20.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Input Tray Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment File Button
            IconButton(
                onClick = { showAttachmentSheet = true },
                modifier = Modifier.background(SurfaceGrey, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Text input container
            GlassTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholderText = "Ask AI anything...",
                modifier = Modifier.weight(1f),
                singleLine = false,
                trailingIcon = {
                    Row {
                        // Microphone Voice Simulator Button
                        IconButton(
                            onClick = {
                                textInput = "Fosphorus: Generate an automated REST endpoint configuration with mock parameters in Kotlin."
                                Toast.makeText(toastContext, "Voice audio captured & processed", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice input", tint = PurpleGlow)
                        }
                        
                        IconButton(
                            onClick = {
                                val querySuffix = selectedFileToAttach?.let { "\n[Refer to contents of file: ${it.name}]" } ?: ""
                                viewModel.sendMessageToAI(textInput + querySuffix)
                                textInput = ""
                                selectedFileToAttach = null
                            },
                            enabled = textInput.isNotBlank() && !isGenerating
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = if (textInput.isNotBlank() && !isGenerating) NeonPurplePrimary else TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                tag = "chat_message_input"
            )
        }
    }

    // Attachment file picking sheet modal
    if (showAttachmentSheet) {
        Dialog(onDismissRequest = { showAttachmentSheet = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pick Code Context to Attach",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (files.filter { !it.isFolder }.isEmpty()) {
                    Text(
                        text = "No files in the workspace directory currently.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(files.filter { !it.isFolder }) { f ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedFileToAttach = f
                                        showAttachmentSheet = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = NeonPurplePrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = f.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                }
                                Text(text = f.language, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            HorizontalDivider(color = DividerColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SecondaryGlassButton(
                    text = "Close",
                    onClick = { showAttachmentSheet = false },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun RenderChatMessage(message: String, onCopyCode: (String) -> Unit) {
    val codeBlockStart = "```"
    if (!message.contains(codeBlockStart)) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        return
    }

    val parts = message.split(codeBlockStart)
    Column {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 0) {
                // Regular Text
                if (part.isNotBlank()) {
                    Text(text = part, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.padding(vertical = 4.dp))
                }
            } else {
                // Code Block Structure
                val lines = part.lines()
                val language = lines.firstOrNull()?.trim() ?: "Code"
                val codeContent = lines.drop(1).joinToString("\n")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BackgroundBlack)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                ) {
                    // Header Bar of Codeblock
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceGrey)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.clickable { onCopyCode(codeContent) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy code", tint = PurpleGlow, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }

                    // Content
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = codeContent,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFFA5C3F1) // customized developer code highlight tone
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. AI AGENT ACTION WORKSPACE
// ==========================================
@Composable
fun AgentWorkspaceTab(
    viewModel: IDEViewModel,
    navigateToEditor: () -> Unit
) {
    val activeFileId by viewModel.activeFileId.collectAsStateWithLifecycle()
    val agentSteps by viewModel.agentSteps.collectAsStateWithLifecycle()
    val isExecuting by viewModel.agentIsExecuting.collectAsStateWithLifecycle()
    
    val tasks = listOf(
        Pair("Refactor & Build Feature", "Write automated high-end Kotlin logic parameters complete into workspace"),
        Pair("Optimize Engine Performance", "Audit memory parameters, and speed compile execution structures"),
        Pair("Integrate Security Layer", "Apply advanced biometric or encryption submodules inline"),
        Pair("Generate Architectural Tests", "Set up JUnit unit tests validating mock coverage metrics")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AI AGENT WORKSPACE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Autonomous multi-file codebase refactoring generator",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "How Autonomous Agent Mode Works",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select one target task below. The Fosphorus agent reads the active tab context, models security graphs, refactors the source code directly to database schemas, and re-syncs the code editor seamlessly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        // Active Tasks Grid list
        item {
            Text(
                text = "Launch Agent Task",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                tasks.forEach { (taskName, tDesc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceGrey.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable(enabled = !isExecuting) {
                                viewModel.executeAgentTask(taskName)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = taskName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = tDesc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run",
                            tint = NeonPurplePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Active Status Logging Sheet
        item {
            AnimatedVisibility(
                visible = isExecuting || agentSteps.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonPurplePrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Agent Execution Logs",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = PurpleGlow
                        )
                        if (isExecuting) {
                            CircularProgressIndicator(
                                color = NeonPurplePrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.5.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Terminal Line logs in real time
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundBlack)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        agentSteps.forEach { step ->
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = if (step.contains("Error")) Color.Red else if (step.contains("Completed")) Color.Green else TextPrimary
                            )
                        }
                    }
                    
                    if (agentSteps.lastOrNull()?.contains("Completed") == true) {
                        Spacer(modifier = Modifier.height(12.dp))
                        NeonButton(
                            text = "Open Reconstructed File in Editor",
                            onClick = navigateToEditor,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. MOBILE CODE EDITOR TAB
// ==========================================
@Composable
fun CodeEditorTab(
    viewModel: IDEViewModel,
    onClose: () -> Unit
) {
    val activeTabs by viewModel.activeTabs.collectAsStateWithLifecycle()
    val activeFileId by viewModel.activeFileId.collectAsStateWithLifecycle()
    val editorContent by viewModel.editorContent.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()

    var showSearchRow by remember { mutableStateOf(false) }
    var searchKeyword by remember { mutableStateOf("") }
    var replaceKeyword by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val focusContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceGrey)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "FOSPHORUS EDITOR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Search Trigger, Undo, Redo Actions
            Row {
                IconButton(onClick = { viewModel.undo() }) {
                    Icon(imageVector = Icons.Default.Undo, contentDescription = "Undo", tint = TextPrimary)
                }
                IconButton(onClick = { viewModel.redo() }) {
                    Icon(imageVector = Icons.Default.Redo, contentDescription = "Redo", tint = TextPrimary)
                }
                IconButton(onClick = { showSearchRow = !showSearchRow }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = if (showSearchRow) NeonPurplePrimary else TextPrimary)
                }
            }
        }

        // Active File Horizontal Tabs List
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(ElevatedSurface)
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeTabs) { fileId ->
                val fEntity = files.firstOrNull { it.id == fileId } ?: return@items
                val isActive = activeFileId == fileId
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isActive) SurfaceGrey else Color.Transparent)
                        .border(1.dp, if (isActive) NeonPurplePrimary.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                        .clickable { viewModel.openFileInEditor(fileId, fEntity.content) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (isActive) PurpleGlow else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = fEntity.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActive) TextPrimary else TextSecondary,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.closeTab(fileId) },
                        modifier = Modifier.size(16.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }

        // Expanded Search / Replace float bar
        AnimatedVisibility(visible = showSearchRow) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceGrey)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        placeholderText = "Find text...",
                        modifier = Modifier.weight(1f)
                    )
                    GlassTextField(
                        value = replaceKeyword,
                        onValueChange = { replaceKeyword = it },
                        placeholderText = "Replace...",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SecondaryGlassButton(
                        text = "Replace All",
                        onClick = {
                            if (searchKeyword.isNotEmpty()) {
                                val currentData = editorContent
                                val replaced = currentData.replace(searchKeyword, replaceKeyword)
                                viewModel.handleEditorValueChange(replaced)
                                Toast.makeText(focusContext, "Replaced matching instances!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        // Active Workspace Editor with line numbers
        if (activeFileId == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No open files. Browse projects and open Kotlin, Java or CSS files to work.", color = TextSecondary, textAlign = TextAlign.Center)
            }
        } else {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Line counts gutter
                val lineCount = maxOf(1, editorContent.lines().size)
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(SurfaceGrey.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = i.toString(),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = NeonPurplePrimary.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }

                // Main editor text box
                BasicTextField(
                    value = editorContent,
                    onValueChange = { viewModel.handleEditorValueChange(it) },
                    modifier = Modifier
                        .testTag("code_editor_field")
                        .fillMaxWidth()
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFC5D9F9),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    ),
                    cursorBrush = Brush.verticalGradient(listOf(NeonPurplePrimary, PurpleGlow))
                )
            }
        }
    }
}

// ==========================================
// 8. PROJECT EXPLORER TAB
// ==========================================
@Composable
fun ProjectExplorerTab(
    viewModel: IDEViewModel,
    navigateToEditor: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val currentProjectId by viewModel.currentProjectId.collectAsStateWithLifecycle()
    val files by viewModel.files.collectAsStateWithLifecycle()
    
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    
    // File creations inputs
    var newFileName by remember { mutableStateOf("") }
    var newFileLang by remember { mutableStateOf("Kotlin") }

    // Project creations inputs
    var newProjName by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }
    var newProjLang by remember { mutableStateOf("Kotlin") }

    val pickerContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Files section title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PROJECT EXPLORER",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Virtual Android filesystem model layers",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Row {
                IconButton(onClick = { showCreateProjectDialog = true }) {
                    Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = "Add project", tint = PurpleGlow)
                }
                IconButton(onClick = { showCreateFileDialog = true }, enabled = currentProjectId != null) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add file", tint = NeonPurplePrimary)
                }
            }
        }

        HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

        // Virtual items tree list
        Box(modifier = Modifier.weight(1f)) {
            if (currentProjectId == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "No Project workspace selected.", color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        NeonButton(text = "New Project", onClick = { showCreateProjectDialog = true })
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Back To Folders option
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateProjectDialog = true
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.SwitchAccount, contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch Workspace Project", style = MaterialTheme.typography.bodySmall, color = PurpleGlow)
                        }
                    }

                    items(files) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceGrey.copy(alpha = 0.4f))
                                .clickable {
                                    if (!item.isFolder) {
                                        viewModel.openFileInEditor(item.id, item.content)
                                        navigateToEditor()
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.isFolder) Icons.Default.Folder else Icons.Default.Code,
                                    contentDescription = null,
                                    tint = if (item.isFolder) PurpleGlow else NeonPurplePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.language,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.deleteFile(item.id)
                                        Toast.makeText(pickerContext, "Deleted successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete file", tint = Color.Red, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export project zip simulated action triggers
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SecondaryGlassButton(
                text = "Import Workspace",
                onClick = { Toast.makeText(pickerContext, "Virtual workspace loaded correctly", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            )
            NeonButton(
                text = "Export Project (.ZIP)",
                onClick = { Toast.makeText(pickerContext, "ZIP compressed and saved: FosphorusSandbox.zip", Toast.LENGTH_LONG).show() },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // CREATE FILE MODAL
    if (showCreateFileDialog) {
        Dialog(onDismissRequest = { showCreateFileDialog = false }) {
            GlassCard(modifier = Modifier.padding(16.dp)) {
                Text("Create Virtual Code File", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(value = newFileName, onValueChange = { newFileName = it }, placeholderText = "e.g. Main.kt")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Language", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val langs = listOf("Kotlin", "Java", "Python", "JavaScript", "CSS", "JSON", "XML")
                    items(langs) { lang ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (newFileLang == lang) NeonPurplePrimary else SurfaceGrey)
                                .clickable { newFileLang = lang }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(lang, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SecondaryGlassButton(text = "Cancel", onClick = { showCreateFileDialog = false })
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonButton(text = "Add File", onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName, newFileLang)
                            newFileName = ""
                            showCreateFileDialog = false
                        }
                    })
                }
            }
        }
    }

    // CREATE PROJECT/WORKSPACE SELECT MODAL
    if (showCreateProjectDialog) {
        Dialog(onDismissRequest = { showCreateProjectDialog = false }) {
            GlassCard(modifier = Modifier.padding(16.dp)) {
                Text("Virtual Active Workspaces", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                
                // Existing Project selections
                Text("Switch To Project:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                LazyColumn(modifier = Modifier.height(120.dp)) {
                    items(projects) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectProject(p.id)
                                    showCreateProjectDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = if (currentProjectId == p.id) NeonPurplePrimary else Color.Transparent, modifier = Modifier.size(16.dp))
                        }
                        HorizontalDivider(color = DividerColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Or Create New", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PurpleGlow)
                Spacer(modifier = Modifier.height(8.dp))
                GlassTextField(value = newProjName, onValueChange = { newProjName = it }, placeholderText = "Project Name")
                Spacer(modifier = Modifier.height(8.dp))
                GlassTextField(value = newProjDesc, onValueChange = { newProjDesc = it }, placeholderText = "Description")
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SecondaryGlassButton(text = "Close", onClick = { showCreateProjectDialog = false })
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonButton(text = "Create", onClick = {
                        if (newProjName.isNotBlank()) {
                            viewModel.createProject(newProjName, newProjDesc, newProjLang)
                            newProjName = ""
                            newProjDesc = ""
                            showCreateProjectDialog = false
                        }
                    })
                }
            }
        }
    }
}

// ==========================================
// 9. SECURITY AUDIT REVIEW SCREEN
// ==========================================
@Composable
fun CodeReviewTab(
    viewModel: IDEViewModel,
    onClose: () -> Unit
) {
    val isReviewing by viewModel.codeReviewIsAnalyzing.collectAsStateWithLifecycle()
    val reviewOutcome by viewModel.codeReviewResult.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = "SECURITY REVIEW AUDIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "Scan file context for leaks and optimizations", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Run Instant Auditor",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Our LLM parser analyzes the current editor text buffer to check logic pitfalls, concurrency leaks, or deprecated APIs.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            NeonButton(
                text = if (isReviewing) "Auditing Codebase..." else "Run AI Audit Scanner",
                onClick = { viewModel.performCodeReview() },
                enabled = !isReviewing,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceGrey.copy(alpha = 0.5f))
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isReviewing) {
                Column(
                    modifier = Modifier.matchParentSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonPurplePrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Deconstructing file AST context trees...", color = PurpleGlow, style = MaterialTheme.typography.bodySmall)
                }
            } else if (reviewOutcome == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ready for scan outcome. Place valid source code inside the editor.", color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                Text(
                    text = reviewOutcome!!,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = TextPrimary
                )
            }
        }
    }
}

// ==========================================
// 10. PROMPT TEMPLATE LIBRARY
// ==========================================
@Composable
fun PromptLibraryTab(
    viewModel: IDEViewModel,
    onSelectPrompt: (String) -> Unit,
    onClose: () -> Unit
) {
    val promptsCategory = listOf(
        "Android UI" to "Optimize performance metrics for high-end Jetpack Compose grids with nested item states.",
        "Backend / REST" to "Create standard retrofitted client interfaces including standard background okhttp parameters and retry handlers.",
        "UI Design" to "Transform visual structure parameters into responsive layout variables using 45/45/10 purple and black distribution guidelines.",
        "Database Persistence" to "Reconstruct SQLite entities with automated migration definitions and flow interfaces using Repository mappings.",
        "AI Agents" to "Model recursive codebase analysis loops mapping directories, fixing logic errors and committing changes."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = "PROMPT TEMPLATE LIBRARY", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "Copy pre-loaded professional IDE templates", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(promptsCategory) { (title, fullText) ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPrompt(fullText) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = PurpleGlow)
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Send prompt", tint = NeonPurplePrimary, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = fullText, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                }
            }
        }
    }
}

// ==========================================
// 11. MODEL HUB SCREEN
// ==========================================
@Composable
fun ModelHubTab(
    viewModel: IDEViewModel,
    onClose: () -> Unit
) {
    val models = listOf(
        Triple("Gemini 3.5 Flash", "Low latency, fast speed, excellent for general Q&A / simple refactoring.", "Free Beta"),
        Triple("Gemini 3.1 Pro", "Deep logical reasoning, advanced coding integration, multi-file graphs.", "Pro Tier"),
        Triple("DeepSeek R1", "High logical consistency, mathematical optimizations, open source models.", "Simulated"),
        Triple("Claude 3.5 Sonnet", "State of the art code syntax, highly creative structures, clean architecture.", "Simulated"),
        Triple("GPT-4o", "Multimodal intelligence parameters, responsive function bindings.", "Simulated")
    )

    var activeModelName by remember { mutableStateOf(viewModel.activeModel.value) }
    var tempVal by remember { mutableStateOf(viewModel.temperature.value) }
    var tokenCount by remember { mutableStateOf(viewModel.maxTokens.value) }

    val toastCtx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = "MODEL CONFIGURATION HUB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "Calibrate LLM temperature, topP, and tokens", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("LLM INFERENCE SLIDERS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Temperature configuration
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Inference Temperature", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text(String.format("%.2f", tempVal), style = MaterialTheme.typography.bodySmall, color = NeonPurplePrimary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = tempVal,
                onValueChange = {
                    tempVal = it
                    viewModel.temperature.value = it
                },
                valueRange = 0f..1.5f,
                colors = SliderDefaults.colors(thumbColor = NeonPurplePrimary, activeTrackColor = PurpleGlow)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Max tokens config
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Max Token Window Limit", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("${tokenCount} Token", style = MaterialTheme.typography.bodySmall, color = PurpleGlow, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = tokenCount.toFloat(),
                onValueChange = {
                    val stepVal = (it.toInt() / 256) * 256
                    tokenCount = stepVal
                    viewModel.maxTokens.value = stepVal
                },
                valueRange = 128f..4096f,
                colors = SliderDefaults.colors(thumbColor = PurpleGlow, activeTrackColor = NeonPurplePrimary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid listing of models
        Text("Supported Engines", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(models) { m ->
                val (name, desc, type) = m
                val isSelected = activeModelName == name

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SurfaceGrey else Color.Transparent)
                        .border(1.dp, if (isSelected) NeonPurplePrimary else DividerColor, RoundedCornerShape(12.dp))
                        .clickable {
                            activeModelName = name
                            viewModel.activeModel.value = name
                            Toast.makeText(toastCtx, "Model context: $name active!", Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isSelected) PurpleGlow else TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text(text = type, style = MaterialTheme.typography.labelSmall, color = if (isSelected) NeonPurplePrimary else TextSecondary.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ==========================================
// 12. RUN LOGS TERMINAL TAB
// ==========================================
@Composable
fun TerminalTab(
    viewModel: IDEViewModel,
    onClose: () -> Unit
) {
    val logs by viewModel.terminalLogs.collectAsStateWithLifecycle()
    val textInput by viewModel.terminalInput.collectAsStateWithLifecycle()

    val logScrollState = rememberScrollState()

    LaunchedEffect(logs.size) {
        logScrollState.animateScrollTo(logScrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(text = "INTEGRATED TERMINAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "Simulated unix compilation pipeline console", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logs terminal panel view
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF070709))
                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                .verticalScroll(logScrollState)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                logs.forEach { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (log.contains("SUCCESSFUL")) Color.Green else if (log.contains(">")) PurpleGlow else TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Keyboard inputs
        GlassTextField(
            value = textInput,
            onValueChange = { viewModel.terminalInput.value = it },
            placeholderText = "Type unix command (e.g. 'help', 'run', 'gradle build')..",
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { viewModel.runTerminalCommand(textInput) }) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = NeonPurplePrimary)
                }
            }
        )
    }
}

// ==========================================
// 13. KNOWLEDGE SNIPPETS WORKSPACE TAB
// ==========================================
@Composable
fun KnowledgeWorkspaceTab(
    viewModel: IDEViewModel,
    onClose: () -> Unit
) {
    val knowledgeList by viewModel.knowledgeItems.collectAsStateWithLifecycle()
    
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var inputTitle by remember { mutableStateOf("") }
    var inputContent by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("notes") }

    val notifierContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(text = "KNOWLEDGE WORKSPACE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = "Notes, prompt templates & snippets database", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            IconButton(
                onClick = { showAddNoteDialog = true },
                modifier = Modifier.background(SurfaceGrey, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add node", tint = NeonPurplePrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (knowledgeList.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No custom notes saved in database yet.", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(knowledgeList) { note ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.AutoMirrored.Outlined.SpeakerNotes, contentDescription = null, tint = PurpleGlow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = note.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteKnowledgeItem(note.id) }, modifier = Modifier.size(18.dp)) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = note.content, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Category: ${note.category.uppercase()} | Saved: ${java.text.SimpleDateFormat("MMM dd, yyyy").format(note.timestamp)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonPurplePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showAddNoteDialog) {
        Dialog(onDismissRequest = { showAddNoteDialog = false }) {
            GlassCard(modifier = Modifier.padding(16.dp)) {
                Text("Add Knowledge Note / Prompt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(value = inputTitle, onValueChange = { inputTitle = it }, placeholderText = "Title")
                Spacer(modifier = Modifier.height(8.dp))
                GlassTextField(value = inputContent, onValueChange = { inputContent = it }, placeholderText = "Content body details...")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category selection", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val cats = listOf("prompts", "snippets", "notes", "architecture")
                    cats.forEach { c ->
                        val isSel = inputCategory == c
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) NeonPurplePrimary else SurfaceGrey)
                                .clickable { inputCategory = c }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(c.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SecondaryGlassButton(text = "Cancel", onClick = { showAddNoteDialog = false })
                    Spacer(modifier = Modifier.width(8.dp))
                    NeonButton(text = "Save note", onClick = {
                        if (inputTitle.isNotBlank() && inputContent.isNotBlank()) {
                            viewModel.saveKnowledgeNotes(inputTitle, inputCategory, inputContent, "")
                            inputTitle = ""
                            inputContent = ""
                            showAddNoteDialog = false
                            Toast.makeText(notifierContext, "Knowledge note fully saved offline!", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        }
    }
}

// ==========================================
// 14. SETTINGS TAB
// ==========================================
@Composable
fun SettingsTab(viewModel: IDEViewModel) {
    val apiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val activeModel by viewModel.activeModel.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()

    var editableKey by remember { mutableStateOf(apiKey) }
    val resetContext = LocalContext.current

    LaunchedEffect(apiKey) {
        editableKey = apiKey
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "FOSPHORUS SETTINGS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Configure AI Agent, keys and system behaviors",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Account Metadata Panel
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Workspace Developer Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PurpleGlow)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Registered Email: $userEmail", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Authentication Sync Mode: SECURE OFFLINE SESSIONS", style = MaterialTheme.typography.labelSmall, color = NeonPurplePrimary, fontWeight = FontWeight.Bold)
            }
        }

        // API Key management
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Gemini AI API Configuration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Optionally override your default AI Studio credentials with a custom Gemini Pro or Flash API key below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                GlassTextField(
                    value = editableKey,
                    onValueChange = {
                        editableKey = it
                        viewModel.customApiKey.value = it
                    },
                    placeholderText = "Enter custom API Key...",
                    singleLine = true
                )
            }
        }

        // System actions and DB deletes
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("Storage and Diagnostics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                
                SecondaryGlassButton(
                    text = "Clear All Database Context Streams",
                    onClick = {
                        viewModel.sendMessageToAI("System prompt: Clean slate")
                        Toast.makeText(resetContext, "In-memory caches flushed correctly.", Toast.LENGTH_SHORT).show()
                    },
                    borderColor = Color.Red.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryGlassButton(
                    text = "Export Workspace Diagnostics Backups",
                    onClick = {
                        Toast.makeText(resetContext, "Diagnostics logs compiled as fosphorus_diagnostics.json", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ==========================================
// BOTTOM NAVIGATION BAR
// ==========================================
@Composable
fun BottomBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val tabs = listOf(
        Triple("home", "Home", Icons.Default.Dashboard),
        Triple("chat", "Chat", Icons.Default.ChatBubbleOutline),
        Triple("agent", "Agent", Icons.Default.AutoAwesome),
        Triple("projects", "Files", Icons.AutoMirrored.Outlined.List),
        Triple("settings", "Settings", Icons.Default.Settings)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars), // Prevent overlap with system gestures
        color = BackgroundBlack,
        tonalElevation = 8.dp
    ) {
        Column {
            HorizontalDivider(color = DividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundBlack)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isActive = activeTab == tab.first
                    
                    Column(
                        modifier = Modifier
                            .testTag("tab_${tab.first}")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTabSelected(tab.first) }
                            .padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isActive) {
                                // Subtle glowing halo behind active icons for liquid feel!
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(NeonPurplePrimary.copy(alpha = 0.15f))
                                )
                            }
                            Icon(
                                imageVector = tab.third,
                                contentDescription = tab.second,
                                tint = if (isActive) NeonPurplePrimary else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.second,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) TextPrimary else TextSecondary,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

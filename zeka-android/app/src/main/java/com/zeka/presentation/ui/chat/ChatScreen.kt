package com.zeka.presentation.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import com.zeka.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.collectAsState
import kotlinx.serialization.json.*
import android.widget.Toast
import java.util.UUID
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeka.presentation.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zeka.presentation.viewmodel.ChatViewModel
import com.zeka.presentation.ui.personas.PersonaScreen
import com.zeka.presentation.ui.providers.ProviderScreen

data class Message(
    val id: String,
    val role: String, // "user", "assistant"
    val content: String,
    val time: String,
    val isStreaming: Boolean = false,
    val attachment: Attachment? = null,
    val tokens: Int = 0,
    val cost: Double = 0.0
)

data class Attachment(
    val name: String,
    val size: String,
    val type: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel(),
    onNewChatClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    var textState by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf("None") }
    var currentTab by remember { mutableStateOf("Sohbetler") }
    var settingsSubScreen by remember { mutableStateOf("Main") }
    var ttsEnabled by remember { mutableStateOf(false) }
    var biometricLockEnabled by remember { mutableStateOf(false) }
    var isAttachmentMenuOpen by remember { mutableStateOf(false) }
    var showAddMcpDialog by remember { mutableStateOf(false) }
    var showAddSkillDialog by remember { mutableStateOf(false) }
    var showAddPluginDialog by remember { mutableStateOf(false) }
    var activePopover by remember { mutableStateOf<String?>(null) }
    var activeCatalogMode by remember { mutableStateOf<String?>(null) }
    var selectedAttachment by remember { mutableStateOf<Attachment?>(null) }
    var currentConversationId by remember { mutableStateOf("default-conversation-id") }
    var isCodeMode by remember { mutableStateOf(false) }
    var selectedWorkspaceName by remember { mutableStateOf<String?>(null) }
    var selectedWorkspacePath by remember { mutableStateOf<String?>(null) }
    var showWorkspaceDialog by remember { mutableStateOf(false) }
    var activeCodeTab by remember { mutableStateOf("Konsol") }
    var activeSkillTag by remember { mutableStateOf<String?>(null) }

    val agentSession by viewModel.agentSession.collectAsState()
    val isAgentRunning by viewModel.isAgentRunning.collectAsState()
    val mcpConsentRequest by viewModel.mcpConsentRequest.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val conversations by viewModel.conversations.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(currentConversationId) {
        viewModel.initDatabase(context)
        viewModel.loadCachedMessages(currentConversationId)
        
        // Populate default mock data on first launch if empty
        if (currentConversationId == "default-conversation-id" && viewModel.messages.isEmpty()) {
            val initialMock = listOf(
                Message(
                    id = "1",
                    role = "user",
                    content = "Kuantum bilgisayarlar nasıl çalışır?\nBasitçe açıklayabilir misin?",
                    time = "09:41"
                ),
                Message(
                    id = "2",
                    role = "assistant",
                    content = "Kuantum bilgisayarlar, klasik bilgisayarlardan farklı olarak kuantum bitleri (kubit) kullanır. Kubitler aynı anda 0 ve 1 durumunda olabilir. Bu özelliğe süperpozisyon denir.\n\nAyrıca, kubitler birbirleriyle dolaşıklık (entanglement) durumuna girebilir. Bu sayede, kuantum bilgisayarlar belirli problemleri çok daha hızlı çözebilir.\n\nÖrnek kullanım alanları:\n• İlaç keşfi\n• Kriptografi\n• Lojistik optimizasyonu\n• Finansal modelleme\n\nAncak bu teknoloji hala gelişim aşamasındadır ve pratik kullanımı için zaman gerekmektedir.",
                    time = "09:41"
                ),
                Message(
                    id = "3",
                    role = "user",
                    content = "Aşağıdaki rehber PDF dosyasını incele.",
                    time = "09:42",
                    attachment = Attachment(
                        name = "Kuantum_Bilgisayarlar_Rehberi.pdf",
                        size = "1.2 MB",
                        type = "PDF"
                    )
                ),
                Message(
                    id = "4",
                    role = "assistant",
                    content = "Dosyanız yüklendi. İçeriği analiz edip sorularınızı yanıtlayabilirim.",
                    time = "09:42"
                )
            )
            viewModel.messages.addAll(initialMock)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = PureBlack,
                drawerTonalElevation = 0.dp
            ) {
                ZekaConversationsDrawer(
                    conversations = conversations,
                    activeConversationId = currentConversationId,
                    onConversationSelected = { id ->
                        currentConversationId = id
                        scope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        val newId = UUID.randomUUID().toString()
                        currentConversationId = newId
                        viewModel.messages.clear()
                        scope.launch { drawerState.close() }
                    },
                    onDeleteConversation = { id ->
                        viewModel.deleteConversation(id)
                        if (currentConversationId == id) {
                            val newId = UUID.randomUUID().toString()
                            currentConversationId = newId
                            viewModel.messages.clear()
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (currentTab == "Sohbetler") {
                    TopAppBar(
                        title = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.beyaz_zeka_logo),
                                    contentDescription = "Zeka Logo",
                                    modifier = Modifier.height(38.dp)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Sharp.Menu,
                                    contentDescription = "Menu",
                                    tint = OffWhite
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                val newId = UUID.randomUUID().toString()
                                currentConversationId = newId
                                viewModel.messages.clear()
                                Toast.makeText(context, "Yeni Sohbet Başlatıldı", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Sharp.Edit,
                                    contentDescription = "New Chat",
                                    tint = OffWhite
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = PureBlack,
                            titleContentColor = OffWhite,
                            navigationIconContentColor = OffWhite,
                            actionIconContentColor = OffWhite
                        )
                    )
                }
            },
        bottomBar = {
            ZekaBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        },
        containerColor = PureBlack
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "tabTransition"
            ) { targetTab ->
            when (targetTab) {
                "Sohbetler" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(PureBlack)
                    ) {
                        val listState = rememberLazyListState()
                        LaunchedEffect(viewModel.messages.size) {
                            if (viewModel.messages.isNotEmpty()) {
                                listState.animateScrollToItem(viewModel.messages.size - 1)
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {

                        // Model Selector Bar inside a Box for Dropdown Menu anchoring
                        var showModelSelectorDropdown by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            ModelSelectorBar(
                                selectedModel = selectedModel,
                                onModelClick = { showModelSelectorDropdown = true },
                                onTuneClick = { currentTab = "Ayarlar" }
                            )

                            DropdownMenu(
                                expanded = showModelSelectorDropdown,
                                onDismissRequest = { showModelSelectorDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Graphite)
                                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                    .padding(4.dp)
                            ) {
                                val addedModelsForSelect = remember(showModelSelectorDropdown) {
                                    com.zeka.data.local.model.ConfiguredModelStore.loadModels(context)
                                }

                                DropdownMenuItem(
                                    text = {
                                        ModelSelectDropdownItem(
                                            name = "None",
                                            provider = "Sağlayıcı Yok",
                                            logoRes = null,
                                            isSelected = selectedModel == "None"
                                        )
                                    },
                                    onClick = {
                                        selectedModel = "None"
                                        showModelSelectorDropdown = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        ModelSelectDropdownItem(
                                            name = "Yerel Çevrimdışı Model",
                                            provider = "Cihaz İçi LLM (Gemma)",
                                            logoRes = null,
                                            isSelected = selectedModel == "Yerel Çevrimdışı Model"
                                        )
                                    },
                                    onClick = {
                                        selectedModel = "Yerel Çevrimdışı Model"
                                        showModelSelectorDropdown = false
                                    }
                                )

                                addedModelsForSelect.forEach { model ->
                                    val logoRes = when (model.provider) {
                                        "OpenAI" -> R.drawable.ic_openai_logo
                                        "Anthropic" -> R.drawable.ic_anthropic_logo
                                        "Google" -> R.drawable.ic_google_logo
                                        "OpenRouter" -> R.drawable.ic_openrouter_logo
                                        "Mistral" -> R.drawable.ic_mistral_logo
                                        else -> null
                                    }

                                    DropdownMenuItem(
                                        text = {
                                            ModelSelectDropdownItem(
                                                name = model.name,
                                                provider = model.provider,
                                                logoRes = logoRes,
                                                isSelected = selectedModel == model.name
                                            )
                                        },
                                        onClick = {
                                            selectedModel = model.name
                                            showModelSelectorDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Active Workspace & Quota Info Panel
                        if (isCodeMode && selectedWorkspaceName != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Graphite.copy(alpha = 0.4f))
                                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Folder,
                                            contentDescription = "Workspace",
                                            tint = Color(0xFF00FFCC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Çalışma Alanı: $selectedWorkspaceName",
                                            color = Color(0xFF00FFCC),
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "Değiştir",
                                        color = OffWhite.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable { showWorkspaceDialog = true }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Kalan Token: 49,850 / 50,000",
                                        color = OffWhite.copy(alpha = 0.4f),
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "Kalan CPU: 3,595 / 3,600 sn",
                                        color = OffWhite.copy(alpha = 0.4f),
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "Hafıza: Aktif (2 Kayıt)",
                                        color = Color(0xFF00FF99),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Main Content Area
                        if (isCodeMode && agentSession != null) {
                            val artifacts by viewModel.agentArtifacts.collectAsState()
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                // Monokrom Tab Selector
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PureBlack)
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                                ) {
                                    listOf("Konsol", "Çıktılar").forEach { tab ->
                                        val active = activeCodeTab == tab
                                        Text(
                                            text = tab,
                                            color = if (active) Color(0xFF00FFCC) else OffWhite.copy(alpha = 0.5f),
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontSize = 13.sp,
                                            modifier = Modifier
                                                .clickable { activeCodeTab = tab }
                                                .padding(vertical = 6.dp)
                                                .drawBehind {
                                                    if (active) {
                                                        drawLine(
                                                            color = Color(0xFF00FFCC),
                                                            start = Offset(0f, size.height),
                                                            end = Offset(size.width, size.height),
                                                            strokeWidth = 2.dp.toPx()
                                                        )
                                                    }
                                                }
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    if (activeCodeTab == "Konsol") {
                                        AgentTerminalPanel(
                                            session = agentSession!!,
                                            isRunning = isAgentRunning,
                                            onApproveNextStep = {
                                                viewModel.executeNextAgentStep("mock-jwt-token")
                                            }
                                        )
                                    } else {
                                        AgentArtifactsPanel(artifacts = artifacts)
                                    }
                                }
                            }
                        } else {
                            // Chat Messages list
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                reverseLayout = false
                            ) {
                                item {
                                    DateDivider(date = "Bugün")
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                items(
                                    items = viewModel.messages,
                                    key = { it.id }
                                ) { message ->
                                    Box(modifier = Modifier.animateContentSize()) {
                                        MessageRow(message = message)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }

                            // Attachment Preview Chip
                            if (selectedAttachment != null) {
                                AttachmentPreviewChip(
                                    attachment = selectedAttachment!!,
                                    onCloseClick = { selectedAttachment = null }
                                )
                            }

                            // Active Local MCP Tool indicator
                            val toolStatus by viewModel.toolStatus.collectAsState()
                            if (toolStatus != null) {
                                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                                val borderAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "borderAlpha"
                                )
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF0D0D0D))
                                        .border(
                                            width = 1.2.dp,
                                            color = Color(0xFF00FFCC).copy(alpha = borderAlpha),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = Color(0xFF00FFCC),
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = toolStatus!!,
                                        color = OffWhite,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Skills Suggestion List (Pops up when typing '/')
                        val configuredSkills = remember { com.zeka.data.local.model.ConfiguredSkillStore.loadSkills(context) }
                        val showSkillsSuggestions = textState.startsWith("/")
                        val suggestionQuery = if (showSkillsSuggestions) textState.substring(1) else ""
                        val filteredSkills = configuredSkills.filter {
                            it.name.contains(suggestionQuery, ignoreCase = true) ||
                            it.triggerKeyword.contains(suggestionQuery, ignoreCase = true)
                        }

                        if (showSkillsSuggestions && filteredSkills.isNotEmpty()) {
                            SkillsSuggestionPopup(
                                skills = filteredSkills,
                                onSkillSelected = { skill ->
                                    activeSkillTag = skill.name
                                    textState = "" // Clear input field after suggestion select
                                }
                            )
                        }

                        // Input Bar Area
                        ChatInputBar(
                            textValue = textState,
                            onValueChange = { textState = it },
                            isAttachmentMenuOpen = isAttachmentMenuOpen,
                            onAttachmentMenuToggle = { isAttachmentMenuOpen = it },
                            isCodeMode = isCodeMode,
                            onCodeModeToggle = {
                                val targetMode = !isCodeMode
                                isCodeMode = targetMode
                                if (targetMode && selectedWorkspacePath == null) {
                                    showWorkspaceDialog = true
                                }
                            },
                            activeSkillTag = activeSkillTag,
                            onRemoveSkillTag = { activeSkillTag = null },
                            onPhotoSelect = {
                                selectedAttachment = Attachment(
                                    name = "Zeka_Mock_Image.png",
                                    size = "1.4 MB",
                                    type = "PNG"
                                )
                            },
                            onPdfSelect = {
                                selectedAttachment = Attachment(
                                    name = "Kullanıcı_Belgesi.pdf",
                                    size = "840 KB",
                                    type = "PDF"
                                )
                            },
                            onAddPluginClick = { activeCatalogMode = "plugin" },
                            onAddSkillClick = { activeCatalogMode = "skill" },
                            onAddMcpClick = { activeCatalogMode = "mcp" },
                            onSendClick = {
                                if (selectedModel == "None") {
                                    Toast.makeText(context, "Lütfen ayarlardan bir model sağlayıcı ekleyin ve model seçin.", Toast.LENGTH_LONG).show()
                                } else {
                                    if (textState.isNotBlank() || selectedAttachment != null || activeSkillTag != null) {
                                        val skillTag = activeSkillTag
                                        val prompt = if (skillTag != null) "/${skillTag.lowercase()} $textState" else textState
                                        val attachment = selectedAttachment
                                        textState = ""
                                        selectedAttachment = null
                                        activeSkillTag = null

                                        val addedModels = com.zeka.data.local.model.ConfiguredModelStore.loadModels(context)
                                        val matchedModel = addedModels.find { it.name == selectedModel }
                                        val activeProvider = matchedModel?.provider?.lowercase() ?: "anthropic"
                                        val activeModelName = matchedModel?.name ?: "claude-3-5-sonnet"

                                        if (isCodeMode) {
                                            viewModel.startAgentSession(
                                                authToken = "mock-jwt-token",
                                                workspaceId = "Abdullah6262637-zeka",
                                                hostPath = selectedWorkspacePath ?: "c:\\Users\\HP\\Desktop\\Zeka",
                                                prompt = prompt,
                                                provider = activeProvider,
                                                modelName = activeModelName
                                            )
                                        } else {
                                            viewModel.sendMessageStream(
                                                context = context,
                                                conversationId = currentConversationId,
                                                provider = activeProvider,
                                                model = activeModelName,
                                                prompt = prompt,
                                                authToken = "mock-jwt-token",
                                                attachment = attachment
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        }

                        // Animated Catalog Overlay Screen
                        AnimatedVisibility(
                            visible = activeCatalogMode != null,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(350, easing = FastOutSlowInEasing)
                            ) + fadeOut()
                        ) {
                            if (activeCatalogMode != null) {
                                CatalogView(
                                    mode = activeCatalogMode!!,
                                    onModeChange = { activeCatalogMode = it },
                                    onClose = { activeCatalogMode = null },
                                    onAddMcpClick = { showAddMcpDialog = true },
                                    onAddSkillClick = { showAddSkillDialog = true },
                                    onAddPluginClick = { showAddPluginDialog = true },
                                    context = context
                                )
                            }
                        }

                        if (showWorkspaceDialog) {
                            WorkspaceSelectionDialog(
                                onWorkspaceSelected = { name, path ->
                                    selectedWorkspaceName = name
                                    selectedWorkspacePath = path
                                    showWorkspaceDialog = false
                                },
                                onDismiss = {
                                    showWorkspaceDialog = false
                                    if (selectedWorkspacePath == null) {
                                        isCodeMode = false
                                    }
                                }
                            )
                        }
                    }
                }
                "Asistanlar" -> {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        PersonaScreen(onBackClick = { currentTab = "Sohbetler" })
                    }
                }
                "Ayarlar" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(PureBlack)
                    ) {
                        AnimatedContent(
                            targetState = settingsSubScreen,
                            transitionSpec = {
                                if (targetState == "Providers") {
                                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(250))) togetherWith
                                            (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(250)))
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(250))) togetherWith
                                            (slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(250)))
                                }
                            },
                            label = "settingsSubScreenTransition"
                        ) { subScreen ->
                            if (subScreen == "Providers") {
                                ProviderScreen(onBackClick = { settingsSubScreen = "Main" })
                            } else {
                                val addedModels = remember(settingsSubScreen) {
                                    com.zeka.data.local.model.ConfiguredModelStore.loadModels(context)
                                }
                                val addedMcpServers = remember(settingsSubScreen) {
                                    com.zeka.data.local.model.ConfiguredMcpStore.loadServers(context)
                                }
                                val addedSkills = remember(settingsSubScreen) {
                                    com.zeka.data.local.model.ConfiguredSkillStore.loadSkills(context)
                                }
                                val addedPlugins = remember(settingsSubScreen) {
                                    com.zeka.data.local.model.ConfiguredPluginStore.loadPlugins(context)
                                }
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp, vertical = 16.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "Ayarlar",
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 28.sp,
                                            color = OffWhite,
                                            modifier = Modifier.padding(bottom = 24.dp)
                                        )
                                    }

                                    // SECTION 1: API Yapılandırması
                                    item {
                                        SettingsCategoryHeader(title = "API YAPILANDIRMASI")
                                    }
                                    item {
                                        SettingsItemRow(
                                            title = "Model Sağlayıcıları (BYOK)",
                                            subtitle = "OpenAI, Anthropic, Gemini, DeepSeek API anahtarları",
                                            icon = Icons.Sharp.VpnKey,
                                            onClick = { settingsSubScreen = "Providers" }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // SECTION: EKLENEN MODELLER
                                    item {
                                        SettingsCategoryHeader(title = "EKLENEN MODELLER")
                                    }
                                    if (addedModels.isEmpty()) {
                                        item {
                                            Text(
                                                text = "Henüz eklenmiş bir model yok. Sağlayıcılar bölümünden ekleyebilirsiniz.",
                                                color = MidGray,
                                                fontFamily = InterFontFamily,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }
                                    } else {
                                        items(addedModels) { model ->
                                            val logoRes = when (model.provider) {
                                                "OpenAI" -> R.drawable.ic_openai_logo
                                                "Anthropic" -> R.drawable.ic_anthropic_logo
                                                "Google" -> R.drawable.ic_google_logo
                                                "OpenRouter" -> R.drawable.ic_openrouter_logo
                                                "Mistral" -> R.drawable.ic_mistral_logo
                                                else -> null
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Graphite)
                                                    .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (logoRes != null) {
                                                    Image(
                                                        painter = painterResource(id = logoRes),
                                                        contentDescription = model.provider,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = model.name,
                                                        fontFamily = SpaceGroteskFontFamily,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = OffWhite
                                                    )
                                                    Text(
                                                        text = "${model.provider} | Temp: ${model.temperature}",
                                                        fontFamily = InterFontFamily,
                                                        fontSize = 11.sp,
                                                        color = MidGray
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = addedModels.filter { it.id != model.id }
                                                        com.zeka.data.local.model.ConfiguredModelStore.saveModels(context, updated)
                                                        settingsSubScreen = "Reload"
                                                        settingsSubScreen = "Main"
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = RedAccent,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                        }
                                        item {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }

                                    // SECTION: MCP Sunucuları
                                    item {
                                         SettingsCategoryHeader(title = "AKTİF MCP SUNUCULARI")
                                    }
                                    if (addedMcpServers.isEmpty()) {
                                         item {
                                             Text(
                                                 text = "Henüz eklenmiş bir MCP sunucusu yok. Sohbet girişindeki '+' butonundan ekleyebilirsiniz.",
                                                 color = MidGray,
                                                 fontFamily = InterFontFamily,
                                                 fontSize = 11.sp,
                                                 modifier = Modifier.padding(vertical = 8.dp)
                                             )
                                             Spacer(modifier = Modifier.height(16.dp))
                                         }
                                    } else {
                                         items(addedMcpServers) { server ->
                                             Row(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .clip(RoundedCornerShape(8.dp))
                                                     .background(Graphite)
                                                     .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                                                     .padding(12.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Box(
                                                     modifier = Modifier
                                                         .size(8.dp)
                                                         .clip(CircleShape)
                                                         .background(if (server.isConnected) Color.Green else MidGray)
                                                 )
                                                 Spacer(modifier = Modifier.width(10.dp))
                                                 Column(modifier = Modifier.weight(1f)) {
                                                     Text(
                                                         text = server.name,
                                                         fontFamily = SpaceGroteskFontFamily,
                                                         fontWeight = FontWeight.Bold,
                                                         fontSize = 13.sp,
                                                         color = OffWhite
                                                     )
                                                     Text(
                                                         text = server.url,
                                                         fontFamily = InterFontFamily,
                                                         fontSize = 11.sp,
                                                         color = MidGray
                                                     )
                                                 }
                                                 IconButton(
                                                     onClick = {
                                                         val updated = addedMcpServers.filter { it.id != server.id }
                                                         com.zeka.data.local.model.ConfiguredMcpStore.saveServers(context, updated)
                                                         settingsSubScreen = "Reload"
                                                         settingsSubScreen = "Main"
                                                     },
                                                     modifier = Modifier.size(24.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.Delete,
                                                         contentDescription = "Delete",
                                                         tint = RedAccent,
                                                         modifier = Modifier.size(18.dp)
                                                     )
                                                 }
                                             }
                                             Spacer(modifier = Modifier.height(12.dp))
                                         }
                                         item {
                                             Spacer(modifier = Modifier.height(8.dp))
                                         }
                                    }

                                    // SECTION: Özel Yetenekler
                                    item {
                                         SettingsCategoryHeader(title = "ÖZEL YETENEKLER (SKILLS)")
                                    }
                                    if (addedSkills.isEmpty()) {
                                         item {
                                             Text(
                                                 text = "Henüz eklenmiş bir özel yetenek yok. Sohbet girişindeki '+' butonundan ekleyebilirsiniz.",
                                                 color = MidGray,
                                                 fontFamily = InterFontFamily,
                                                 fontSize = 11.sp,
                                                 modifier = Modifier.padding(vertical = 8.dp)
                                             )
                                             Spacer(modifier = Modifier.height(16.dp))
                                         }
                                    } else {
                                         items(addedSkills) { skill ->
                                             Row(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .clip(RoundedCornerShape(8.dp))
                                                     .background(Graphite)
                                                     .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                                                     .padding(12.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = skill.triggerKeyword,
                                                     color = OffWhite,
                                                     fontFamily = SpaceGroteskFontFamily,
                                                     fontWeight = FontWeight.Bold,
                                                     fontSize = 12.sp,
                                                     modifier = Modifier
                                                         .clip(RoundedCornerShape(4.dp))
                                                         .background(PureBlack)
                                                         .border(1.dp, DividerColor, RoundedCornerShape(4.dp))
                                                         .padding(horizontal = 6.dp, vertical = 3.dp)
                                                 )
                                                 Spacer(modifier = Modifier.width(10.dp))
                                                 Column(modifier = Modifier.weight(1f)) {
                                                     Text(
                                                         text = skill.name,
                                                         fontFamily = SpaceGroteskFontFamily,
                                                         fontWeight = FontWeight.Bold,
                                                         fontSize = 13.sp,
                                                         color = OffWhite
                                                     )
                                                     Text(
                                                         text = if (skill.promptInstruction.length > 40) skill.promptInstruction.take(38) + "..." else skill.promptInstruction,
                                                         fontFamily = InterFontFamily,
                                                         fontSize = 11.sp,
                                                         color = MidGray
                                                     )
                                                 }
                                                 IconButton(
                                                     onClick = {
                                                         val updated = addedSkills.filter { it.id != skill.id }
                                                         com.zeka.data.local.model.ConfiguredSkillStore.saveSkills(context, updated)
                                                         settingsSubScreen = "Reload"
                                                         settingsSubScreen = "Main"
                                                     },
                                                     modifier = Modifier.size(24.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.Delete,
                                                         contentDescription = "Delete",
                                                         tint = RedAccent,
                                                         modifier = Modifier.size(18.dp)
                                                     )
                                                 }
                                             }
                                             Spacer(modifier = Modifier.height(12.dp))
                                         }
                                         item {
                                             Spacer(modifier = Modifier.height(8.dp))
                                         }
                                    }

                                    // SECTION: Eklentiler
                                    item {
                                         SettingsCategoryHeader(title = "YÜKLENEN EKLENTİLER (PLUGINS)")
                                    }
                                    if (addedPlugins.isEmpty()) {
                                         item {
                                             Text(
                                                 text = "Henüz yüklenmiş bir eklenti yok. Sohbet girişindeki '+' butonundan ekleyebilirsiniz.",
                                                 color = MidGray,
                                                 fontFamily = InterFontFamily,
                                                 fontSize = 11.sp,
                                                 modifier = Modifier.padding(vertical = 8.dp)
                                             )
                                             Spacer(modifier = Modifier.height(16.dp))
                                         }
                                    } else {
                                         items(addedPlugins) { plugin ->
                                             Row(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .clip(RoundedCornerShape(8.dp))
                                                     .background(Graphite)
                                                     .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                                                     .padding(12.dp),
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Switch(
                                                     checked = plugin.isEnabled,
                                                     onCheckedChange = { isChecked ->
                                                         val updated = addedPlugins.map {
                                                             if (it.id == plugin.id) it.copy(isEnabled = isChecked) else it
                                                         }
                                                         com.zeka.data.local.model.ConfiguredPluginStore.savePlugins(context, updated)
                                                         settingsSubScreen = "Reload"
                                                         settingsSubScreen = "Main"
                                                     },
                                                     colors = SwitchDefaults.colors(
                                                         checkedThumbColor = PureBlack,
                                                         checkedTrackColor = OffWhite,
                                                         uncheckedThumbColor = MidGray,
                                                         uncheckedTrackColor = Graphite
                                                     ),
                                                     modifier = Modifier.scale(0.8f)
                                                 )
                                                 Spacer(modifier = Modifier.width(10.dp))
                                                 Column(modifier = Modifier.weight(1f)) {
                                                     Text(
                                                         text = plugin.name,
                                                         fontFamily = SpaceGroteskFontFamily,
                                                         fontWeight = FontWeight.Bold,
                                                         fontSize = 13.sp,
                                                         color = OffWhite
                                                     )
                                                     Text(
                                                         text = plugin.description,
                                                         fontFamily = InterFontFamily,
                                                         fontSize = 11.sp,
                                                         color = MidGray
                                                     )
                                                 }
                                                 IconButton(
                                                     onClick = {
                                                         val updated = addedPlugins.filter { it.id != plugin.id }
                                                         com.zeka.data.local.model.ConfiguredPluginStore.savePlugins(context, updated)
                                                         settingsSubScreen = "Reload"
                                                         settingsSubScreen = "Main"
                                                     },
                                                     modifier = Modifier.size(24.dp)
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.Delete,
                                                         contentDescription = "Delete",
                                                         tint = RedAccent,
                                                         modifier = Modifier.size(18.dp)
                                                     )
                                                 }
                                             }
                                             Spacer(modifier = Modifier.height(12.dp))
                                         }
                                         item {
                                             Spacer(modifier = Modifier.height(8.dp))
                                         }
                                    }

                                    // SECTION 2: Ses Entegrasyonu
                                    item {
                                        SettingsCategoryHeader(title = "SES & HOPARLÖR")
                                    }
                                    item {
                                        SettingsSwitchRow(
                                            title = "Sesli Yanıt Okuma (TTS)",
                                            subtitle = "Gelen yanıtları otomatik olarak seslendir",
                                            icon = Icons.Sharp.VolumeUp,
                                            checked = ttsEnabled,
                                            onCheckedChange = { ttsEnabled = it }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // SECTION 3: Güvenlik Koruması
                                    item {
                                        SettingsCategoryHeader(title = "GÜVENLİK KORUMASI")
                                    }
                                    item {
                                        SettingsSwitchRow(
                                            title = "Biyometrik Kilit",
                                            subtitle = "Uygulama açılışında parmak izi/yüz tanıma iste",
                                            icon = Icons.Sharp.Lock,
                                            checked = biometricLockEnabled,
                                            onCheckedChange = { biometricLockEnabled = it }
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }

                                    // SECTION 4: Veri & Önbellek Yönetimi
                                    item {
                                        SettingsCategoryHeader(title = "VERİ & BELLEK YÖNETİMİ")
                                    }
                                    item {
                                        SettingsItemRow(
                                            title = "Yerel Önbelleği Temizle",
                                            subtitle = "Room veritabanındaki tüm mesaj geçmişini siler",
                                            icon = Icons.Sharp.Delete,
                                            onClick = {
                                                viewModel.clearAllConversations()
                                                Toast.makeText(context, "Tüm sohbet geçmişi silindi", Toast.LENGTH_SHORT).show()
                                            },
                                            titleColor = RedAccent
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    // SECTION 5: Hakkında
                                    item {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.beyaz_zeka_logo),
                                                contentDescription = "Zeka Logo",
                                                modifier = Modifier.height(54.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Version 1.0.0 (Stable)",
                                                color = MidGray,
                                                fontFamily = InterFontFamily,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                 text = "Kullanıcının kendi API anahtarlarıyla çalışan şifreli ve gizlilik odaklı yapay zeka asistanı.",
                                                color = MidGray,
                                                fontFamily = InterFontFamily,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Animated Overlays for MCP, Skills & Plugins
            AnimatedAddMcpOverlay(
                visible = showAddMcpDialog,
                onDismiss = { showAddMcpDialog = false },
                context = context
            )

            AnimatedAddSkillOverlay(
                visible = showAddSkillDialog,
                onDismiss = { showAddSkillDialog = false },
                context = context
            )

            AnimatedAddPluginOverlay(
                visible = showAddPluginDialog,
                onDismiss = { showAddPluginDialog = false },
                context = context
            )

            mcpConsentRequest?.let { request ->
                McpConsentDialog(
                    toolName = request.toolName,
                    description = request.description,
                    onApprove = { viewModel.approveMcpRequest() },
                    onDeny = { viewModel.denyMcpRequest() }
                )
            }
        }
    }
}
}

@Composable
fun ModelSelectorBar(
    selectedModel: String,
    onModelClick: () -> Unit,
    onTuneClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onModelClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Anthropic/Model Logo Icon
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PureBlack)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AI",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = OffWhite
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedModel,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = OffWhite
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Model",
                tint = MidGray,
                modifier = Modifier.size(16.dp)
            )
        }

        IconButton(
            onClick = onTuneClick,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Sharp.Tune,
                contentDescription = "Tune parameters",
                tint = OffWhite,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DateDivider(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DividerColor)
        )
        Text(
            text = date,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = MidGray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(DividerColor)
        )
    }
}

@Composable
fun MessageRow(message: Message) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            // Assistant Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PureBlack)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Z",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = OffWhite
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Speaker Name Label
            Text(
                text = if (isUser) "Siz" else "Zeka",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = if (isUser) WhiteAccent else MidGray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Attachment (if any)
            message.attachment?.let { attachment ->
                AttachmentCard(attachment = attachment)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Clean Text Area (No background box)
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.content,
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = OffWhite,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (message.isStreaming) {
                        val infiniteTransition = rememberInfiniteTransition(label = "caret")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = keyframes { durationMillis = 400 },
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "alpha"
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Box(
                            modifier = Modifier
                                .size(width = 6.dp, height = 15.dp)
                                .background(OffWhite.copy(alpha = alpha))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.tokens > 0) {
                        Text(
                            text = "${message.tokens} tkn | \$${"%.5f".format(message.cost)}",
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            color = MidGray
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.time,
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            color = MidGray
                        )
                        if (isUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read status",
                                tint = OffWhite,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Actions for Assistant response
            if (!isUser && !message.isStreaming) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy",
                        tint = MidGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { /* Copy text */ }
                    )
                    Icon(
                        imageVector = Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = MidGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { /* Like */ }
                    )
                    Icon(
                        imageVector = Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike",
                        tint = MidGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { /* Dislike */ }
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = MidGray,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { /* Regenerate */ }
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(12.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Graphite)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User",
                    tint = OffWhite,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentCard(attachment: Attachment) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PureBlack)
                    .border(1.dp, DividerColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Document",
                    tint = OffWhite,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = attachment.name,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = OffWhite,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${attachment.size} • ${attachment.type}",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = MidGray
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.Visibility,
            contentDescription = "Preview",
            tint = OffWhite,
            modifier = Modifier
                .size(20.dp)
                .clickable { /* Preview PDF */ }
        )
    }
}

@Composable
fun ChatInputBar(
    textValue: String,
    onValueChange: (String) -> Unit,
    isAttachmentMenuOpen: Boolean,
    onAttachmentMenuToggle: (Boolean) -> Unit,
    isCodeMode: Boolean,
    onCodeModeToggle: () -> Unit,
    activeSkillTag: String?,
    onRemoveSkillTag: () -> Unit,
    onPhotoSelect: () -> Unit,
    onPdfSelect: () -> Unit,
    onAddPluginClick: () -> Unit,
    onAddSkillClick: () -> Unit,
    onAddMcpClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rotationAngle by animateFloatAsState(
            targetValue = if (isAttachmentMenuOpen) 45f else 0f,
            animationSpec = tween(durationMillis = 200),
            label = "plusRotation"
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Graphite)
                .border(1.dp, DividerColor, CircleShape)
                .clickable { onAttachmentMenuToggle(!isAttachmentMenuOpen) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add",
                tint = OffWhite,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = rotationAngle)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        AnimatedContent(
            targetState = isAttachmentMenuOpen,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { width -> width } + fadeIn(animationSpec = tween(200))) togetherWith
                            (slideOutHorizontally { width -> -width } + fadeOut(animationSpec = tween(200)))
                } else {
                    (slideInHorizontally { width -> -width } + fadeIn(animationSpec = tween(200))) togetherWith
                            (slideOutHorizontally { width -> width } + fadeOut(animationSpec = tween(200)))
                }
            },
            modifier = Modifier.weight(1f),
            label = "inputBarTransition"
        ) { menuOpen ->
            if (menuOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttachmentQuickOption(
                        title = "PDF",
                        icon = Icons.Sharp.Description,
                        onClick = {
                            onPdfSelect()
                            onAttachmentMenuToggle(false)
                        }
                    )
                    AttachmentQuickOption(
                        title = "Resim",
                        icon = Icons.Sharp.Image,
                        onClick = {
                            onPhotoSelect()
                            onAttachmentMenuToggle(false)
                        }
                    )
                    AttachmentQuickOption(
                        title = "Eklenti",
                        icon = Icons.Sharp.Extension,
                        onClick = {
                            onAddPluginClick()
                            onAttachmentMenuToggle(false)
                        }
                    )
                    AttachmentQuickOption(
                        title = "Yetenek",
                        icon = Icons.Sharp.AutoAwesome,
                        onClick = {
                            onAddSkillClick()
                            onAttachmentMenuToggle(false)
                        }
                    )
                    AttachmentQuickOption(
                        title = "MCP",
                        icon = Icons.Sharp.Hub,
                        onClick = {
                            onAddMcpClick()
                            onAttachmentMenuToggle(false)
                        }
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(CircleShape)
                            .background(Graphite)
                            .border(1.dp, DividerColor, CircleShape)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // A. Code Mode Toggle in the Corner
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onCodeModeToggle() }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.Code,
                                contentDescription = "Code Mode Toggle",
                                tint = if (isCodeMode) Color(0xFF00FFCC) else MidGray,
                                modifier = Modifier.size(18.dp)
                            )
                            if (isCodeMode) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "KOD",
                                    color = Color(0xFF00FFCC),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // B. Active Skill Tag Capsule
                        if (activeSkillTag != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF161616))
                                    .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Sharp.AutoAwesome,
                                    contentDescription = "Active Skill",
                                    tint = Color(0xFF00FFCC),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = activeSkillTag.lowercase(),
                                    color = OffWhite,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MidGray,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clickable { onRemoveSkillTag() }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // C. Input text field
                        BasicTextField(
                            value = textValue,
                            onValueChange = onValueChange,
                            textStyle = TextStyle(
                                color = OffWhite,
                                fontFamily = InterFontFamily,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(OffWhite),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (textValue.isEmpty()) {
                                    Text(
                                        text = if (activeSkillTag != null) "" else "Mesajınızı yazın...",
                                        color = MidGray,
                                        fontFamily = InterFontFamily,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = MidGray,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { /* STT activation */ }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WhiteAccent)
                            .clickable(onClick = onSendClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = PureBlack,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZekaBottomNavigation(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(PureBlack)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val items = listOf(
            Triple("Sohbetler", Icons.Sharp.ChatBubbleOutline, Icons.Sharp.ChatBubble),
            Triple("Asistanlar", Icons.Sharp.PersonOutline, Icons.Sharp.Person),
            Triple("Ayarlar", Icons.Sharp.Settings, Icons.Sharp.Settings)
        )

        items.forEach { (title, outlineIcon, filledIcon) ->
            val isSelected = currentTab == title
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTabSelected(title) }
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) filledIcon else outlineIcon,
                    contentDescription = title,
                    tint = if (isSelected) OffWhite else MidGray,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentPreviewChip(attachment: Attachment, onCloseClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Graphite)
            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (attachment.type == "PDF") Icons.Sharp.Description else Icons.Sharp.Image,
            contentDescription = null,
            tint = OffWhite,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.name,
                color = OffWhite,
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = attachment.size,
                color = MidGray,
                fontFamily = InterFontFamily,
                fontSize = 10.sp
            )
        }
        IconButton(onClick = onCloseClick, modifier = Modifier.size(20.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = MidGray,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AttachmentOptionRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PureBlack)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = OffWhite, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = title, color = OffWhite, fontFamily = InterFontFamily, fontSize = 14.sp)
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title,
        fontFamily = SpaceGroteskFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = MidGray,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
    )
}

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    titleColor: Color = OffWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Graphite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (titleColor == OffWhite) OffWhite else titleColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = titleColor
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = MidGray
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Go",
            tint = MidGray,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Graphite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OffWhite,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = OffWhite
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = MidGray
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PureBlack,
                checkedTrackColor = OffWhite,
                uncheckedThumbColor = MidGray,
                uncheckedTrackColor = Graphite
            )
        )
    }
}

@Composable
fun ZekaConversationsDrawer(
    conversations: List<com.zeka.data.local.db.ConversationEntity>,
    activeConversationId: String,
    onConversationSelected: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp)
    ) {
        // Drawer Header Logo + Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.beyaz_zeka_logo),
                contentDescription = "Zeka Logo",
                modifier = Modifier.height(36.dp)
            )
        }

        // New Chat Button (+)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Graphite)
                .clickable(onClick = onNewChatClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Sharp.Add,
                contentDescription = "New Chat",
                tint = OffWhite,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Yeni Sohbet Başlat",
                color = OffWhite,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section Title: GEÇMİŞ SOHBETLER
        Text(
            text = "GEÇMİŞ SOHBETLER",
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MidGray,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Conversations List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (conversations.isEmpty()) {
                item {
                    Text(
                        text = "Henüz geçmiş sohbet yok.",
                        color = MidGray,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(conversations) { conv ->
                    val isActive = conv.id == activeConversationId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isActive) Graphite else Color.Transparent)
                            .clickable { onConversationSelected(conv.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (isActive) OffWhite else MidGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = conv.title,
                                color = if (isActive) OffWhite else MidGray,
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                maxLines = 1,
                                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                            )
                        }

                        // Quick Delete Button
                        IconButton(
                            onClick = { onDeleteConversation(conv.id) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.Delete,
                                contentDescription = "Delete Conversation",
                                tint = if (isActive) RedAccent else MidGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelSelectDropdownItem(
    name: String,
    provider: String,
    logoRes: Int?,
    isSelected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = provider,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(DividerColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = OffWhite
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = OffWhite
            )
            Text(
                text = provider,
                fontFamily = InterFontFamily,
                fontSize = 9.sp,
                color = MidGray
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Sharp.Check,
                contentDescription = "Selected",
                tint = OffWhite,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun AttachmentQuickOption(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = OffWhite,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = OffWhite
        )
    }
}

data class CatalogItem(
    val title: String,
    val subtitle: String,
    val tag: String,
    val downloads: String,
    val logoRes: Int?,
    val logoIcon: ImageVector?,
    val urlOrKeyword: String
)

@Composable
fun CatalogView(
    mode: String, // "mcp", "skill", "plugin"
    onModeChange: (String) -> Unit,
    onClose: () -> Unit,
    onAddMcpClick: () -> Unit,
    onAddSkillClick: () -> Unit,
    onAddPluginClick: () -> Unit,
    context: android.content.Context
) {
    val addedMcp = remember(mode) { com.zeka.data.local.model.ConfiguredMcpStore.loadServers(context) }
    val addedSkills = remember(mode) { com.zeka.data.local.model.ConfiguredSkillStore.loadSkills(context) }
    val addedPlugins = remember(mode) { com.zeka.data.local.model.ConfiguredPluginStore.loadPlugins(context) }

    var selectedTab by remember(mode) { mutableStateOf("recommended") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Tab Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            val tabTitle1 = when (mode) {
                "mcp" -> "Önerilen MCP'ler"
                "skill" -> "Önerilen Yetenekler"
                "plugin" -> "Önerilen Eklentiler"
                else -> ""
            }
            val tabTitle2 = when (mode) {
                "mcp" -> "MCP'lerim"
                "skill" -> "Yeteneklerim"
                "plugin" -> "Eklentilerim"
                else -> ""
            }

            Column(
                modifier = Modifier
                    .clickable { selectedTab = "recommended" }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tabTitle1,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selectedTab == "recommended") OffWhite else MidGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(48.dp)
                        .background(if (selectedTab == "recommended") OffWhite else Color.Transparent)
                )
            }

            Column(
                modifier = Modifier
                    .clickable { selectedTab = "my" }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tabTitle2,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (selectedTab == "my") OffWhite else MidGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(48.dp)
                        .background(if (selectedTab == "my") OffWhite else Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sub-header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (mode) {
                    "mcp" -> "Zeka ile birlikte kullanabileceğin hazır MCP sunucuları."
                    "skill" -> "Sohbette # anahtarı ile tetikleyebileceğin hazır yetenekler."
                    "plugin" -> "Yapay zekanın kabiliyetlerini artıran hazır eklentiler."
                    else -> ""
                },
                color = MidGray,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    when (mode) {
                        "mcp" -> onAddMcpClick()
                        "skill" -> onAddSkillClick()
                        "plugin" -> onAddPluginClick()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Graphite, contentColor = OffWhite),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = when (mode) {
                        "mcp" -> "+ MCP Ekle"
                        "skill" -> "+ Yetenek Ekle"
                        "plugin" -> "+ Eklenti Ekle"
                        else -> ""
                    },
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cards list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == "recommended") {
                if (mode == "mcp") {
                    val recommended = listOf(
                        CatalogItem("GitHub", "Repository'leri arayın, dosya okuyun ve içerik alın.", "Resmi", "12.4K", R.drawable.ic_github_logo, null, "http://localhost:8081/mcp/github"),
                        CatalogItem("Notion", "Notion sayfalarınızı arayın, okuyun ve yönetin.", "Resmi", "8.7K", R.drawable.ic_notion_logo, null, "http://localhost:8081/mcp/notion"),
                        CatalogItem("Google Drive", "Dosyalarınıza erişin, arayın ve içerik alın.", "Resmi", "15.2K", R.drawable.ic_google_drive_logo, null, "http://localhost:8081/mcp/google-drive"),
                        CatalogItem("Slack", "Kanalları, mesajları okuyun ve arama yapın.", "Topluluk", "6.1K", R.drawable.ic_slack_logo, null, "http://localhost:8081/mcp/slack"),
                        CatalogItem("PostgreSQL", "Veritabanınıza bağlanın, sorgular çalıştırın.", "Topluluk", "5.3K", R.drawable.ic_postgresql_logo, null, "http://localhost:8081/mcp/postgresql"),
                        CatalogItem("Airtable", "Veri tabanlarınızı okuyun ve içerik alın.", "Topluluk", "3.8K", R.drawable.ic_airtable_logo, null, "http://localhost:8081/mcp/airtable")
                    )

                    items(recommended) { item ->
                        val isAdded = addedMcp.any { it.url == item.urlOrKeyword }
                        CatalogCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            tag = item.tag,
                            downloads = item.downloads,
                            logoRes = item.logoRes,
                            logoIcon = item.logoIcon,
                            isAdded = isAdded,
                            onToggle = {
                                val current = addedMcp.toMutableList()
                                if (isAdded) {
                                    current.removeAll { it.url == item.urlOrKeyword }
                                    Toast.makeText(context, "${item.title} kaldırıldı.", Toast.LENGTH_SHORT).show()
                                } else {
                                    current.add(com.zeka.data.local.model.ConfiguredMcpServer("mcp-${System.currentTimeMillis()}", item.title, item.urlOrKeyword, true))
                                    Toast.makeText(context, "${item.title} eklendi.", Toast.LENGTH_SHORT).show()
                                }
                                com.zeka.data.local.model.ConfiguredMcpStore.saveServers(context, current)
                            }
                        )
                    }
                }

                if (mode == "skill") {
                    val recommended = listOf(
                        CatalogItem("Yazılım Desteği", "Sadece temiz ve optimize edilmiş kod ver.", "Resmi", "20.1K", null, Icons.Sharp.Code, "#kod"),
                        CatalogItem("Akıllı Çeviri", "Verilen metni İngilizceye çevir.", "Resmi", "14.4K", null, Icons.Sharp.Translate, "#cevir"),
                        CatalogItem("Hızlı Özet", "Metni en önemli 3 madde halinde özetle.", "Topluluk", "9.2K", null, Icons.Sharp.Article, "#ozet")
                    )

                    items(recommended) { item ->
                        val isAdded = addedSkills.any { it.triggerKeyword == item.urlOrKeyword }
                        CatalogCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            tag = item.tag,
                            downloads = item.downloads,
                            logoRes = item.logoRes,
                            logoIcon = item.logoIcon,
                            isAdded = isAdded,
                            onToggle = {
                                val current = addedSkills.toMutableList()
                                if (isAdded) {
                                    current.removeAll { it.triggerKeyword == item.urlOrKeyword }
                                    Toast.makeText(context, "${item.title} kaldırıldı.", Toast.LENGTH_SHORT).show()
                                } else {
                                    current.add(com.zeka.data.local.model.ConfiguredSkill("skill-${System.currentTimeMillis()}", item.title, item.urlOrKeyword, item.subtitle))
                                    Toast.makeText(context, "${item.title} eklendi.", Toast.LENGTH_SHORT).show()
                                }
                                com.zeka.data.local.model.ConfiguredSkillStore.saveSkills(context, current)
                            }
                        )
                    }
                }

                if (mode == "plugin") {
                    val recommended = listOf(
                        CatalogItem("Borsa & Kripto", "Canlı finansal verileri takip etme eklentisi.", "Resmi", "11.3K", null, Icons.Sharp.TrendingUp, "plugin-finance"),
                        CatalogItem("Hesap Analizcisi", "CSV verilerini görselleştirme eklentisi.", "Resmi", "8.9K", null, Icons.Sharp.TableChart, "plugin-csv"),
                        CatalogItem("Dall-E 3 Üretici", "Yapay zeka ile görsel üretme eklentisi.", "Topluluk", "16.4K", null, Icons.Sharp.Brush, "plugin-dalle")
                    )

                    items(recommended) { item ->
                        val isAdded = addedPlugins.any { it.name == item.title }
                        CatalogCard(
                            title = item.title,
                            subtitle = item.subtitle,
                            tag = item.tag,
                            downloads = item.downloads,
                            logoRes = item.logoRes,
                            logoIcon = item.logoIcon,
                            isAdded = isAdded,
                            onToggle = {
                                val current = addedPlugins.toMutableList()
                                if (isAdded) {
                                    current.removeAll { it.name == item.title }
                                    Toast.makeText(context, "${item.title} kaldırıldı.", Toast.LENGTH_SHORT).show()
                                } else {
                                    current.add(com.zeka.data.local.model.ConfiguredPlugin("plugin-${System.currentTimeMillis()}", item.title, item.subtitle, true))
                                    Toast.makeText(context, "${item.title} yüklendi.", Toast.LENGTH_SHORT).show()
                                }
                                com.zeka.data.local.model.ConfiguredPluginStore.savePlugins(context, current)
                            }
                        )
                    }
                }
            } else {
                // "My Added Items" Tab
                if (mode == "mcp") {
                    if (addedMcp.isEmpty()) {
                        item {
                            Text(text = "Henüz eklenmiş bir MCP sunucusu yok.", color = MidGray, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    } else {
                        items(addedMcp) { server ->
                            CatalogCard(
                                title = server.name,
                                subtitle = server.url,
                                tag = if (server.isConnected) "Aktif" else "Bağlantı Yok",
                                downloads = "Özel",
                                logoRes = R.drawable.ic_github_logo,
                                logoIcon = null,
                                isAdded = true,
                                onToggle = {
                                    val current = addedMcp.filter { it.id != server.id }
                                    com.zeka.data.local.model.ConfiguredMcpStore.saveServers(context, current)
                                }
                            )
                        }
                    }
                }

                if (mode == "skill") {
                    if (addedSkills.isEmpty()) {
                        item {
                            Text(text = "Henüz eklenmiş bir özel yetenek yok.", color = MidGray, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    } else {
                        items(addedSkills) { skill ->
                            CatalogCard(
                                title = skill.name,
                                subtitle = skill.promptInstruction,
                                tag = skill.triggerKeyword,
                                downloads = "Özel",
                                logoRes = null,
                                logoIcon = Icons.Sharp.Code,
                                isAdded = true,
                                onToggle = {
                                    val current = addedSkills.filter { it.id != skill.id }
                                    com.zeka.data.local.model.ConfiguredSkillStore.saveSkills(context, current)
                                }
                            )
                        }
                    }
                }

                if (mode == "plugin") {
                    if (addedPlugins.isEmpty()) {
                        item {
                            Text(text = "Henüz yüklenmiş bir eklenti yok.", color = MidGray, fontSize = 12.sp, fontFamily = InterFontFamily)
                        }
                    } else {
                        items(addedPlugins) { plugin ->
                            CatalogCard(
                                title = plugin.name,
                                subtitle = plugin.description,
                                tag = if (plugin.isEnabled) "Aktif" else "Pasif",
                                downloads = "Özel",
                                logoRes = null,
                                logoIcon = Icons.Sharp.Extension,
                                isAdded = true,
                                onToggle = {
                                    val current = addedPlugins.filter { it.id != plugin.id }
                                    com.zeka.data.local.model.ConfiguredPluginStore.savePlugins(context, current)
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Floating Bottom Navigation Pill inside Catalog View
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Graphite)
                    .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Close button [ X ]
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PureBlack)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = OffWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Eklenti Tab Button
                CatalogPillButton(
                    title = "Eklenti",
                    icon = Icons.Sharp.Extension,
                    isSelected = mode == "plugin",
                    onClick = { onModeChange("plugin") }
                )

                // Yetenek Tab Button
                CatalogPillButton(
                    title = "Yetenek",
                    icon = Icons.Sharp.Star,
                    isSelected = mode == "skill",
                    onClick = { onModeChange("skill") }
                )

                // MCP Tab Button
                CatalogPillButton(
                    title = "MCP",
                    icon = Icons.Sharp.Layers,
                    isSelected = mode == "mcp",
                    onClick = { onModeChange("mcp") }
                )
            }
        }
    }
}

@Composable
fun CatalogCard(
    title: String,
    subtitle: String,
    tag: String,
    downloads: String,
    logoRes: Int?,
    logoIcon: ImageVector?,
    isAdded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo / Icon Container
        if (logoRes != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = logoRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        } else if (logoIcon != null) {
            Icon(
                imageVector = logoIcon,
                contentDescription = title,
                tint = Color(0xFF00FFCC),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info Column
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = OffWhite
                )
                // If it is a skill, draw a small trigger tag beside title
                if (tag.startsWith("#")) {
                    Text(
                        text = tag,
                        color = OffWhite,
                        fontSize = 9.sp,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(PureBlack)
                            .border(1.5.dp, DividerColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                color = MidGray,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tag indicator (only show if it is not trigger tag already shown)
                if (!tag.startsWith("#")) {
                    val tagColor = if (tag == "Resmi" || tag == "Aktif") Color.Green else Color(0xFF9C27B0)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(tagColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tag,
                            color = MidGray,
                            fontFamily = InterFontFamily,
                            fontSize = 9.sp
                        )
                    }
                }

                // Downloads indicator
                Text(
                    text = "📥 $downloads",
                    color = MidGray,
                    fontFamily = InterFontFamily,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Action Button
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAdded) PureBlack else OffWhite,
                contentColor = if (isAdded) OffWhite else PureBlack
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier
                .height(30.dp)
                .border(1.dp, if (isAdded) DividerColor else Color.Transparent, RoundedCornerShape(8.dp)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isAdded) "Kaldır" else "Ekle",
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun CatalogPillButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) PureBlack else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) OffWhite else MidGray,
            modifier = Modifier.size(16.dp)
        )
        if (isSelected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = OffWhite,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun AnimatedAddMcpOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            var mcpName by remember(visible) { mutableStateOf("") }
            var mcpUrl by remember(visible) { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        )
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Graphite)
                    .border(1.dp, DividerColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(MidGray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Yeni MCP Sunucusu Ekle",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OffWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Sunucu Adı", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = mcpName,
                        onValueChange = { mcpName = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Sunucu URL (SSE Endpoint)", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = mcpUrl,
                        onValueChange = { mcpUrl = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OffWhite),
                            modifier = Modifier.weight(1f).height(44.dp).border(1.dp, DividerColor, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "İptal", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                if (mcpName.isNotBlank() && mcpUrl.isNotBlank()) {
                                    val current = com.zeka.data.local.model.ConfiguredMcpStore.loadServers(context).toMutableList()
                                    val newServer = com.zeka.data.local.model.ConfiguredMcpServer(
                                        id = "mcp-${System.currentTimeMillis()}",
                                        name = mcpName,
                                        url = mcpUrl,
                                        isConnected = true
                                    )
                                    current.add(newServer)
                                    com.zeka.data.local.model.ConfiguredMcpStore.saveServers(context, current)
                                    Toast.makeText(context, "$mcpName sunucusu başarıyla eklendi.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OffWhite, contentColor = PureBlack),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Kaydet", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedAddSkillOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            var skillName by remember(visible) { mutableStateOf("") }
            var skillTrigger by remember(visible) { mutableStateOf("") }
            var skillInstruction by remember(visible) { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        )
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Graphite)
                    .border(1.dp, DividerColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(MidGray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Yeni Özel Yetenek Ekle",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OffWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Yetenek Adı", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Tetikleyici Kelime (Örn: #kod)", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = skillTrigger,
                        onValueChange = { skillTrigger = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Sistem Talimatı (Prompt Instruction)", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = skillInstruction,
                        onValueChange = { skillInstruction = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OffWhite),
                            modifier = Modifier.weight(1f).height(44.dp).border(1.dp, DividerColor, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "İptal", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                if (skillName.isNotBlank() && skillTrigger.isNotBlank() && skillInstruction.isNotBlank()) {
                                    val current = com.zeka.data.local.model.ConfiguredSkillStore.loadSkills(context).toMutableList()
                                    val newSkill = com.zeka.data.local.model.ConfiguredSkill(
                                        id = "skill-${System.currentTimeMillis()}",
                                        name = skillName,
                                        triggerKeyword = skillTrigger,
                                        promptInstruction = skillInstruction
                                    )
                                    current.add(newSkill)
                                    com.zeka.data.local.model.ConfiguredSkillStore.saveSkills(context, current)
                                    Toast.makeText(context, "$skillName yeteneği başarıyla eklendi.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OffWhite, contentColor = PureBlack),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Kaydet", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedAddPluginOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    context: android.content.Context
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureBlack.copy(alpha = 0.7f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            var pluginName by remember(visible) { mutableStateOf("") }
            var pluginDesc by remember(visible) { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(350, easing = FastOutSlowInEasing)
                        )
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Graphite)
                    .border(1.dp, DividerColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(MidGray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Yeni Eklenti Yükle",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = OffWhite
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Eklenti Adı", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = pluginName,
                        onValueChange = { pluginName = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Eklenti Açıklaması", color = MidGray, fontSize = 11.sp, fontFamily = InterFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    BasicTextField(
                        value = pluginDesc,
                        onValueChange = { pluginDesc = it },
                        textStyle = TextStyle(color = OffWhite, fontFamily = InterFontFamily, fontSize = 13.sp),
                        cursorBrush = SolidColor(OffWhite),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = PureBlack, contentColor = OffWhite),
                            modifier = Modifier.weight(1f).height(44.dp).border(1.dp, DividerColor, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "İptal", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                if (pluginName.isNotBlank() && pluginDesc.isNotBlank()) {
                                    val current = com.zeka.data.local.model.ConfiguredPluginStore.loadPlugins(context).toMutableList()
                                    val newPlugin = com.zeka.data.local.model.ConfiguredPlugin(
                                        id = "plugin-${System.currentTimeMillis()}",
                                        name = pluginName,
                                        description = pluginDesc,
                                        isEnabled = true
                                    )
                                    current.add(newPlugin)
                                    com.zeka.data.local.model.ConfiguredPluginStore.savePlugins(context, current)
                                    Toast.makeText(context, "$pluginName eklentisi başarıyla yüklendi.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Lütfen tüm alanları doldurun.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = OffWhite, contentColor = PureBlack),
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "Yükle", fontFamily = SpaceGroteskFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SegmentedModeSelector(
    isCodeMode: Boolean,
    onModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isCodeMode, label = "ModeTransition")
    
    val indicatorOffset by transition.animateDp(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) },
        label = "IndicatorOffset"
    ) { codeMode ->
        if (codeMode) 130.dp else 0.dp
    }

    Box(
        modifier = modifier
            .width(268.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Graphite.copy(alpha = 0.6f))
            .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(132.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(17.dp))
                .background(
                    if (isCodeMode) {
                        Brush.horizontalGradient(listOf(Color(0xFF00FFCC), Color(0xFF0099FF)))
                    } else {
                        Brush.horizontalGradient(listOf(Color(0xFF8A2BE2), Color(0xFF5D3FD3)))
                    }
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChanged(false) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sohbet",
                    color = if (!isCodeMode) Color.White else OffWhite.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChanged(true) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Kod Modu",
                    color = if (isCodeMode) Color.White else OffWhite.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun WorkspaceSelectionDialog(
    onWorkspaceSelected: (name: String, path: String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Graphite)
                .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Çalışma Alanı Seç",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ajanın izole Docker sandbox ortamında kod geliştireceği proje klasörünü seçin.",
                    color = OffWhite.copy(alpha = 0.7f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Default active workspace option
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PureBlack)
                        .border(1.dp, Color(0xFF8A2BE2).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            onWorkspaceSelected("Abdullah6262637/Zeka", "c:\\Users\\HP\\Desktop\\Zeka")
                        }
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Abdullah6262637/Zeka",
                            color = Color(0xFF00FFCC),
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "c:\\Users\\HP\\Desktop\\Zeka",
                            color = OffWhite.copy(alpha = 0.5f),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "İptal",
                            color = OffWhite.copy(alpha = 0.7f),
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentTerminalPanel(
    session: com.zeka.presentation.viewmodel.AgentSession,
    isRunning: Boolean,
    onApproveNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureBlack)
            .padding(16.dp)
    ) {
        Text(
            text = "GÖREV PLANI VE AKIŞI",
            color = Color(0xFF00FFCC),
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Steps timeline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                .background(Graphite.copy(alpha = 0.3f))
                .padding(12.dp)
        ) {
            session.tasks.forEachIndexed { index, task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                when (task.status) {
                                    "completed" -> Color(0xFF00FF99).copy(alpha = 0.2f)
                                    "running" -> Color(0xFF0099FF).copy(alpha = 0.2f)
                                    "failed" -> Color(0xFFFF3366).copy(alpha = 0.2f)
                                    else -> Color.Gray.copy(alpha = 0.2f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (index + 1).toString(),
                            color = when (task.status) {
                                "completed" -> Color(0xFF00FF99)
                                "running" -> Color(0xFF0099FF)
                                "failed" -> Color(0xFFFF3366)
                                else -> Color.Gray
                            },
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = task.title,
                            color = if (task.status == "running") Color.White else OffWhite.copy(alpha = 0.8f),
                            fontWeight = if (task.status == "running") FontWeight.Bold else FontWeight.Normal,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp
                        )
                        Text(
                            text = task.command,
                            color = OffWhite.copy(alpha = 0.4f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Terminal Log Console
        Text(
            text = "TERMİNAL LOG ÇIKTISI",
            color = OffWhite.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold,
            fontFamily = SpaceGroteskFontFamily,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        val currentTask = session.tasks.getOrNull(session.currentTaskIndex) ?: session.tasks.lastOrNull()
        val logs = if (currentTask != null) {
            val stdout = currentTask.stdout
            val stderr = currentTask.stderr
            if (stdout.isBlank() && stderr.isBlank()) {
                "Komut bekleniyor..."
            } else {
                stdout + stderr
            }
        } else {
            "Sistem hazır."
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0D0D0D))
                .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = logs,
                color = if (currentTask?.status == "failed") Color(0xFFFF3366) else Color(0xFF00FF99),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            )
        }

        // Approval Card / Control Button
        if (session.status == "planned" && !isRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E140A))
                    .border(1.dp, Color(0xFFFF9900).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "ONAY BEKLİYOR",
                        color = Color(0xFFFF9900),
                        fontWeight = FontWeight.Bold,
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bir sonraki komut çalıştırılmak üzere onayınızı bekliyor:",
                        color = OffWhite.copy(alpha = 0.8f),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PureBlack)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = currentTask?.command ?: "echo",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onApproveNextStep,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = PureBlack),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text(
                            text = "Onayla ve Çalıştır",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                }
            }
        }
    }
}
}

@Composable
fun AgentArtifactsPanel(
    artifacts: List<com.zeka.presentation.viewmodel.AgentArtifact>,
    modifier: Modifier = Modifier
) {
    if (artifacts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(PureBlack),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Henüz çıktı üretilmedi.",
                color = OffWhite.copy(alpha = 0.5f),
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 14.sp
            )
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(PureBlack)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(
                items = artifacts,
                key = { it.id }
            ) { artifact ->
                when (artifact.type) {
                    "plan" -> PlanChecklistCard(artifact = artifact)
                    "diff" -> DiffViewerCard(artifact = artifact)
                    "screenshot" -> ScreenshotCard(artifact = artifact)
                    else -> DefaultArtifactCard(artifact = artifact)
                }
            }
        }
    }
}

@Composable
fun ScreenshotCard(
    artifact: com.zeka.presentation.viewmodel.AgentArtifact,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite.copy(alpha = 0.5f))
            .border(1.5.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artifact.title,
                    color = Color(0xFF0099FF),
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp
                )
                Text(
                    text = artifact.createdAt,
                    color = OffWhite.copy(alpha = 0.4f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D0D))
                    .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
            ) {
                coil.compose.AsyncImage(
                    model = artifact.content,
                    contentDescription = artifact.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun PlanChecklistCard(
    artifact: com.zeka.presentation.viewmodel.AgentArtifact,
    modifier: Modifier = Modifier
) {
    val tasks: List<String> = remember(artifact.content) {
        try {
            val json = Json.parseToJsonElement(artifact.content).jsonObject
            val array = json["tasks"]?.jsonArray
            array?.mapIndexed { index, element ->
                val obj = element.jsonObject
                obj["title"]?.jsonPrimitive?.content ?: "Adım $index"
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite.copy(alpha = 0.5f))
            .border(1.5.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "GÖREV PLANI",
                    color = Color(0xFF00FFCC),
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp
                )
                Text(
                    text = artifact.createdAt,
                    color = OffWhite.copy(alpha = 0.4f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            for (taskTitle in tasks) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = Color(0xFF00FF99),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = taskTitle,
                        color = OffWhite.copy(alpha = 0.9f),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DiffViewerCard(
    artifact: com.zeka.presentation.viewmodel.AgentArtifact,
    modifier: Modifier = Modifier
) {
    val lines = remember(artifact.content) { artifact.content.split("\n") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite.copy(alpha = 0.5f))
            .border(1.5.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artifact.title,
                    color = Color(0xFF8A2BE2),
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp
                )
                Text(
                    text = artifact.createdAt,
                    color = OffWhite.copy(alpha = 0.4f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0D0D0D))
                    .border(1.5.dp, DividerColor, RoundedCornerShape(8.dp))
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                Column {
                    lines.forEach { line ->
                        val isAddition = line.startsWith("+") && !line.startsWith("+++")
                        val isDeletion = line.startsWith("-") && !line.startsWith("---")
                        val isHeader = line.startsWith("@@") || line.startsWith("diff")

                        val bgColor = when {
                            isAddition -> Color(0xFF00FF99).copy(alpha = 0.12f)
                            isDeletion -> Color(0xFFFF3366).copy(alpha = 0.12f)
                            isHeader -> Color(0xFF0099FF).copy(alpha = 0.08f)
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isAddition -> Color(0xFF00FF99)
                            isDeletion -> Color(0xFFFF3366)
                            isHeader -> Color(0xFF0099FF)
                            else -> OffWhite.copy(alpha = 0.7f)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = line,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DefaultArtifactCard(
    artifact: com.zeka.presentation.viewmodel.AgentArtifact,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Graphite.copy(alpha = 0.5f))
            .border(1.5.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = artifact.title,
                    color = OffWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 12.sp
                )
                Text(
                    text = artifact.createdAt,
                    color = OffWhite.copy(alpha = 0.4f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = artifact.content,
                color = OffWhite.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D0D0D))
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun McpConsentDialog(
    toolName: String,
    description: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDeny
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PureBlack)
                .border(2.dp, Color(0xFF00FFCC), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                    contentDescription = "MCP Uyarısı",
                    tint = Color(0xFF00FFCC),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MCP Erişim Talebi",
                    color = OffWhite,
                    fontWeight = FontWeight.Bold,
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$toolName: $description",
                    color = OffWhite.copy(alpha = 0.8f),
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDeny,
                        colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Reddet",
                            color = OffWhite,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "İzin Ver",
                            color = PureBlack,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SkillsSuggestionPopup(
    skills: List<com.zeka.data.local.model.ConfiguredSkill>,
    onSkillSelected: (com.zeka.data.local.model.ConfiguredSkill) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0D0D0D))
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "YETENEKLER (SKILLS)",
                color = MidGray,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            for (skill in skills) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSkillSelected(skill) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Sharp.AutoAwesome,
                        contentDescription = "Skill",
                        tint = Color(0xFF00FFCC),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "/${skill.name.lowercase()}",
                            color = OffWhite,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (skill.promptInstruction.length > 60) skill.promptInstruction.take(60) + "..." else skill.promptInstruction,
                            color = MidGray,
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}




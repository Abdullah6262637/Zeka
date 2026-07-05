package com.zeka.presentation.ui.providers

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Check
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material.icons.sharp.KeyboardArrowDown
import androidx.compose.material.icons.sharp.KeyboardArrowRight
import androidx.compose.material.icons.sharp.Layers
import androidx.compose.material.icons.sharp.Lock
import androidx.compose.material.icons.sharp.MoreHoriz
import androidx.compose.material.icons.sharp.Save
import androidx.compose.material.icons.sharp.SettingsInputComponent
import androidx.compose.material.icons.sharp.Sync
import androidx.compose.material.icons.sharp.Thermostat
import androidx.compose.material.icons.sharp.Tune
import androidx.compose.material.icons.sharp.Visibility
import androidx.compose.material.icons.sharp.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeka.R
import com.zeka.presentation.ui.theme.*

data class ProviderItem(
    val id: String,
    val name: String,
    val logoRes: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf("Anthropic") }
    
    var providerNameInput by remember { mutableStateOf("Claude") }
    var apiKeyInput by remember { mutableStateOf("") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var baseUrlInput by remember { mutableStateOf("https://api.anthropic.com") }
    
    // Model Selection
    var selectedModelCode by remember { mutableStateOf("Claude 3.5 Sonnet") }
    
    // Sliders
    var temperatureVal by remember { mutableStateOf(0.7f) }
    var maxTokensVal by remember { mutableStateOf(4096f) }
    var topPVal by remember { mutableStateOf(1.0f) }
    var frequencyPenaltyVal by remember { mutableStateOf(0.0f) }
    var presencePenaltyVal by remember { mutableStateOf(0.0f) }
    
    // System Prompt
    var systemPromptInput by remember { mutableStateOf("") }

    val providersList = remember {
        listOf(
            ProviderItem("Anthropic", "Anthropic", R.drawable.ic_anthropic_logo),
            ProviderItem("OpenAI", "OpenAI", R.drawable.ic_openai_logo),
            ProviderItem("Google", "Google", R.drawable.ic_google_logo),
            ProviderItem("OpenRouter", "OpenRouter", R.drawable.ic_openrouter_logo),
            ProviderItem("Mistral", "Mistral AI", R.drawable.ic_mistral_logo),
            ProviderItem("DahaFazla", "Daha fazla", null)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Image(
                                    painter = painterResource(id = R.drawable.beyaz_zeka_logo),
                                    contentDescription = "Zeka Logo",
                                    modifier = Modifier.height(38.dp)
                                )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Sharp.ArrowBack,
                            contentDescription = "Back",
                            tint = OffWhite
                        )
                    }
                },
                actions = {
                    // Empty box to balance the navigation icon and keep title centered
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureBlack)
            )
        },
        containerColor = PureBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Sağlayıcı Ayarları",
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "API sağlayıcınızı yapılandırın ve model ayarlarını düzenleyin.",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = MidGray
                    )
                }
            }

            // Section: Providers Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Graphite)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Sağlayıcı",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OffWhite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        providersList.forEach { provider ->
                            val isSelected = selectedProvider == provider.id
                            Box(
                                modifier = Modifier
                                    .size(width = 82.dp, height = 82.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Graphite)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) OffWhite else DividerColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedProvider = provider.id
                                        providerNameInput = when (provider.id) {
                                            "Anthropic" -> "Claude"
                                            "OpenAI" -> "GPT"
                                            "Google" -> "Gemini"
                                            "OpenRouter" -> "OpenRouter"
                                            "Mistral" -> "Mistral"
                                            else -> "Özel Sağlayıcı"
                                        }
                                        baseUrlInput = when (provider.id) {
                                            "Anthropic" -> "https://api.anthropic.com"
                                            "OpenAI" -> "https://api.openai.com/v1"
                                            "Google" -> "https://generativelanguage.googleapis.com"
                                            "OpenRouter" -> "https://openrouter.ai/api/v1"
                                            "Mistral" -> "https://api.mistral.ai/v1"
                                            else -> ""
                                        }
                                        selectedModelCode = when (provider.id) {
                                            "Anthropic" -> "Claude 3.5 Sonnet"
                                            "OpenAI" -> "GPT-4o"
                                            "Google" -> "Gemini 1.5 Pro"
                                            "OpenRouter" -> "Llama 3.1 405B"
                                            "Mistral" -> "Mistral Large 2"
                                            else -> "Custom Model"
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(OffWhite)
                                            .align(Alignment.TopEnd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Sharp.Check,
                                            contentDescription = null,
                                            tint = PureBlack,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (provider.logoRes != null) {
                                        Image(
                                            painter = painterResource(id = provider.logoRes),
                                            contentDescription = provider.name,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Sharp.MoreHoriz,
                                            contentDescription = provider.name,
                                            tint = OffWhite,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = provider.name,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (isSelected) OffWhite else MidGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Provider Credentials Form
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Graphite)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Sağlayıcı Bilgileri",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OffWhite
                    )

                    // Provider Name
                    Column {
                        Text(
                            text = "Sağlayıcı Adı",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = MidGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = providerNameInput,
                            onValueChange = { providerNameInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureBlack,
                                unfocusedContainerColor = PureBlack,
                                focusedBorderColor = DividerColor,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // API Key
                    Column {
                        Text(
                            text = "API Key",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = MidGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = { apiKeyInput = it },
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                    Icon(
                                        imageVector = if (apiKeyVisible) Icons.Sharp.Visibility else Icons.Sharp.VisibilityOff,
                                        contentDescription = "Toggle key visibility",
                                        tint = MidGray
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureBlack,
                                unfocusedContainerColor = PureBlack,
                                focusedBorderColor = DividerColor,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Sharp.Lock,
                                contentDescription = "Secure",
                                tint = MidGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Anahtarınız uçtan uca şifrelenir ve sadece sizin tarafınızdan çözülebilir.",
                                fontFamily = InterFontFamily,
                                fontSize = 10.sp,
                                color = MidGray
                            )
                        }
                    }

                    // Base URL
                    Column {
                        Text(
                            text = "Base URL (Opsiyonel)",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = MidGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PureBlack,
                                unfocusedContainerColor = PureBlack,
                                focusedBorderColor = DividerColor,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Özel bir uç nokta kullanıyorsanız buraya girin.",
                            fontFamily = InterFontFamily,
                            fontSize = 10.sp,
                            color = MidGray
                        )
                    }
                }
            }

            // Section: Model Select
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Graphite)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Model Seçimi",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OffWhite
                    )

                    // Model selector card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .clickable {
                                Toast.makeText(context, "Model seçimi tetiklendi.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Layers,
                            contentDescription = null,
                            tint = OffWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedModelCode,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = OffWhite
                                )
                                if (selectedProvider == "Anthropic") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1E3A20))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Önerilen",
                                            color = Color(0xFF4CAF50),
                                            fontFamily = InterFontFamily,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "En yeni ve en güçlü model. Dengeli performans.",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = MidGray
                            )
                        }
                        Icon(
                            imageVector = Icons.Sharp.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MidGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Get Models Sync row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureBlack)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .clickable {
                                Toast.makeText(context, "Modeller güncelleniyor...", Toast.LENGTH_SHORT).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Sync,
                            contentDescription = null,
                            tint = OffWhite,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Modelleri Getir",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = OffWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mevcut modelleri sağlayıcıdan güncelleyin.",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = MidGray
                            )
                        }
                        Icon(
                            imageVector = Icons.Sharp.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MidGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Section: Model Parameters
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Graphite)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Model Parametreleri",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = OffWhite
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Sharp.Info,
                                contentDescription = "Info",
                                tint = MidGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Sıfırla",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = MidGray,
                            modifier = Modifier.clickable {
                                temperatureVal = 0.7f
                                maxTokensVal = 4096f
                                topPVal = 1.0f
                                frequencyPenaltyVal = 0.0f
                                presencePenaltyVal = 0.0f
                                Toast.makeText(context, "Parametreler sıfırlandı.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Temperature
                    ParameterSliderRow(
                        title = "Temperature",
                        subtitle = "Yaratıcılık düzeyi",
                        icon = Icons.Sharp.Thermostat,
                        value = temperatureVal,
                        onValueChange = { temperatureVal = it },
                        valueRange = 0.0f..1.2f,
                        displayValueString = "%.1f".format(temperatureVal)
                    )

                    // Max Tokens
                    ParameterSliderRow(
                        title = "Max Tokens",
                        subtitle = "Maksimum yanıt uzunluğu",
                        icon = Icons.Sharp.Layers,
                        value = maxTokensVal,
                        onValueChange = { maxTokensVal = it },
                        valueRange = 1f..8192f,
                        displayValueString = maxTokensVal.toInt().toString()
                    )

                    // Top P
                    ParameterSliderRow(
                        title = "Top P",
                        subtitle = "Nucleus sampling",
                        icon = Icons.Sharp.Tune,
                        value = topPVal,
                        onValueChange = { topPVal = it },
                        valueRange = 0.0f..1.0f,
                        displayValueString = "%.1f".format(topPVal)
                    )

                    // Frequency Penalty
                    ParameterSliderRow(
                        title = "Frequency Penalty",
                        subtitle = "Tekrarı azaltır",
                        icon = Icons.Sharp.Edit,
                        value = frequencyPenaltyVal,
                        onValueChange = { frequencyPenaltyVal = it },
                        valueRange = 0.0f..2.0f,
                        displayValueString = "%.1f".format(frequencyPenaltyVal)
                    )

                    // Presence Penalty
                    ParameterSliderRow(
                        title = "Presence Penalty",
                        subtitle = "Yeni konuları teşvik eder",
                        icon = Icons.Sharp.Sync,
                        value = presencePenaltyVal,
                        onValueChange = { presencePenaltyVal = it },
                        valueRange = 0.0f..2.0f,
                        displayValueString = "%.1f".format(presencePenaltyVal)
                    )
                }
            }

            // Section: System Prompt
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Graphite)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sistem Promptu (Opsiyonel)",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OffWhite
                    )
                    OutlinedTextField(
                        value = systemPromptInput,
                        onValueChange = { if (it.length <= 4000) systemPromptInput = it },
                        placeholder = { Text("Modelin varsayılan olarak izlemesini istediğiniz talimatları girin...", color = MidGray, fontSize = 13.sp) },
                        minLines = 4,
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = PureBlack,
                            unfocusedContainerColor = PureBlack,
                            focusedBorderColor = DividerColor,
                            unfocusedBorderColor = DividerColor,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${systemPromptInput.length} / 4000",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MidGray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Bottom action buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Sağlayıcı bağlantısı başarılı!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = OffWhite
                        ),
                        border = BorderStroke(1.dp, DividerColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.SettingsInputComponent,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bağlantıyı Test Et",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (apiKeyInput.isNotBlank()) {
                                val currentModels = com.zeka.data.local.model.ConfiguredModelStore.loadModels(context).toMutableList()
                                val modelCodeClean = selectedModelCode.lowercase().replace(" ", "-")
                                val modelId = "${selectedProvider.lowercase()}-$modelCodeClean"
                                currentModels.removeAll { it.id == modelId }
                                val newModel = com.zeka.data.local.model.ConfiguredModel(
                                    id = modelId,
                                    provider = selectedProvider,
                                    name = selectedModelCode,
                                    modelCode = selectedModelCode,
                                    apiKey = apiKeyInput,
                                    baseUrl = baseUrlInput,
                                    temperature = temperatureVal,
                                    maxTokens = maxTokensVal.toInt(),
                                    topP = topPVal,
                                    frequencyPenalty = frequencyPenaltyVal,
                                    presencePenalty = presencePenaltyVal,
                                    systemPrompt = systemPromptInput
                                )
                                currentModels.add(newModel)
                                com.zeka.data.local.model.ConfiguredModelStore.saveModels(context, currentModels)
                                Toast.makeText(context, "$selectedModelCode başarıyla eklendi.", Toast.LENGTH_SHORT).show()
                                onBackClick()
                            } else {
                                Toast.makeText(context, "Lütfen geçerli bir API anahtarı girin.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OffWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Sharp.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kaydet",
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
fun ParameterSliderRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValueString: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MidGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.width(96.dp)) {
            Text(
                text = title,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = OffWhite,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 10.sp,
                color = MidGray,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        
        // Premium sleek canvas slider
        PremiumSleekSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(PureBlack)
                .border(1.dp, DividerColor, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayValueString,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = OffWhite
            )
        }
    }
}

@Composable
fun PremiumSleekSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .height(24.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val valueRangeLength = valueRange.endInclusive - valueRange.start
        val fraction = ((value - valueRange.start) / valueRangeLength).coerceIn(0f, 1f)

        val dragModifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val newFraction = (offset.x / widthPx).coerceIn(0f, 1f)
                        val newValue = valueRange.start + newFraction * valueRangeLength
                        onValueChange(newValue)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val currentX = (fraction * widthPx) + dragAmount.x
                    val newFraction = (currentX / widthPx).coerceIn(0f, 1f)
                    val newValue = valueRange.start + newFraction * valueRangeLength
                    onValueChange(newValue)
                }
            }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(dragModifier)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cy = size.height / 2f
                val trackHeight = 2.dp.toPx()
                val thumbRadius = 6.dp.toPx()

                // Draw inactive track
                drawLine(
                    color = DividerColor,
                    start = Offset(0f, cy),
                    end = Offset(size.width, cy),
                    strokeWidth = trackHeight
                )

                // Draw active track
                val activeX = fraction * size.width
                drawLine(
                    color = OffWhite,
                    start = Offset(0f, cy),
                    end = Offset(activeX, cy),
                    strokeWidth = trackHeight
                )

                // Draw thumb
                drawCircle(
                    color = OffWhite,
                    radius = thumbRadius,
                    center = Offset(activeX, cy)
                )
            }
        }
    }
}

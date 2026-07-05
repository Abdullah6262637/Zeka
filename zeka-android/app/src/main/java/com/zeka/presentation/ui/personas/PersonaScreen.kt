package com.zeka.presentation.ui.personas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Delete
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zeka.presentation.ui.theme.*

data class Persona(
    val id: String,
    val name: String,
    val systemPrompt: String,
    val icon: String,
    val temperature: Float,
    val isCustom: Boolean
)

data class PromptTemplate(
    val title: String,
    val description: String,
    val systemPrompt: String,
    val category: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    onBackClick: () -> Unit = {}
) {
    var nameInput by remember { mutableStateOf("") }
    var promptInput by remember { mutableStateOf("") }
    var temperatureVal by remember { mutableStateOf(0.7f) }
    var showCreateForm by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Asistanlarım, 1 = Prompt Kütüphanesi

    val personasList = remember {
        mutableStateListOf(
            Persona("1", "Kodlama Yardımcısı", "Sen uzman bir yazılım geliştiricisisin. Kodları optimize et ve temiz yaz.", "💻", 0.3f, false),
            Persona("2", "Türkçe Öğretmeni", "Sen bir Türkçe öğretmenisin. İmla hatalarını düzelt ve açıklamalar yap.", "✍️", 0.5f, false),
            Persona("3", "Fikir Fırtınası", "Sen yaratıcı bir kreatif direktörsün. Çılgın ve yenilikçi fikirler üret.", "💡", 0.9f, true)
        )
    }

    val promptTemplates = remember {
        listOf(
            PromptTemplate(
                title = "Kod Denetleyicisi & Refaktör",
                description = "Yazdığınız kodları inceleyip performans ve güvenlik açıklarını analiz eden asistan.",
                systemPrompt = "Sen kıdemli bir yazılım mimarısın. Gönderilen kodları temiz kod (clean code), SOLID prensipleri ve performans açısından incele. Hataları tespit et ve daha optimize edilmiş refaktör alternatifleri sun.",
                category = "Yazılım",
                icon = "🛠️"
            ),
            PromptTemplate(
                title = "Yapay Zeka Prompt Mühendisi",
                description = "LLM modelleri için en optimize edilmiş sistem talimatlarını ve promptları tasarlayan uzman.",
                systemPrompt = "Sen profesyonel bir Prompt Mühendisisin. Kullanıcının yapay zekaya yaptırmak istediği işi al ve bunun için mükemmel yapılandırılmış, rol tanımlı, kısıtlamaları net olan bir sistem promptu üret.",
                category = "Yapay Zeka",
                icon = "🧠"
            ),
            PromptTemplate(
                title = "Kişisel Fitness & Diyet Koçu",
                description = "Hedeflerinize göre kalori hesabı ve egzersiz programları tasarlayan antrenör.",
                systemPrompt = "Sen sertifikalı bir kişisel spor antrenörü ve beslenme uzmanısın. Kullanıcının yaş, boy, kilo ve hedeflerine göre özelleştirilmiş haftalık antrenörlük ve makro kalori planlaması yap.",
                category = "Sağlık",
                icon = "💪"
            ),
            PromptTemplate(
                title = "İngilizce Konuşma Pratiği",
                description = "Sizinle seviyenize göre İngilizce sohbet edip dil bilgiisi hatalarınızı açıklayan rehber.",
                systemPrompt = "You are an encouraging English conversation partner. Chat with the user in English. Keep your responses concise. Correct their grammatical mistakes in a friendly way and explain the corrections.",
                category = "Eğitim",
                icon = "🇬🇧"
            ),
            PromptTemplate(
                title = "Kreatif Metin Yazarı",
                description = "Sosyal medya postları, reklam metinleri ve blog yazıları hazırlayan metin yazarı.",
                systemPrompt = "Sen ödüllü bir kreatif metin yazarısın. Dikkat çekici başlıklar, yüksek dönüşümlü reklam metinleri ve akıcı sosyal medya içerikleri tasarla. Tonun samimi ve ikna edici olsun.",
                category = "Pazarlama",
                icon = "✍️"
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Asistan Personaları",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = OffWhite
                    )
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
                    if (activeTab == 0) {
                        IconButton(onClick = { showCreateForm = !showCreateForm }) {
                            Icon(
                                imageVector = Icons.Sharp.Add,
                                contentDescription = "New Persona",
                                tint = OffWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureBlack)
            )
        },
        containerColor = PureBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Custom AMOLED Tabs Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Asistanlarım", "Prompt Kütüphanesi").forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Graphite else PureBlack)
                            .border(1.dp, if (isSelected) OffWhite else DividerColor, RoundedCornerShape(8.dp))
                            .clickable { activeTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSelected) OffWhite else MidGray
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeTab == 0) {
                    // Form to create a new Persona
                    if (showCreateForm) {
                        item {
                            Text(
                                text = "Yeni Persona Oluştur",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = OffWhite,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Graphite)
                                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "Asistan Adı",
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        placeholder = { Text("Örn: Fitness Danışmanı", color = MidGray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = PureBlack,
                                            unfocusedContainerColor = PureBlack,
                                            focusedBorderColor = OffWhite,
                                            unfocusedBorderColor = DividerColor,
                                            focusedTextColor = OffWhite,
                                            unfocusedTextColor = OffWhite
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Sistem Promptu (Talimatlar)",
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = promptInput,
                                        onValueChange = { promptInput = it },
                                        placeholder = { Text("Sen bir uzman spor antrenörüsün...", color = MidGray) },
                                        minLines = 3,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = PureBlack,
                                            unfocusedContainerColor = PureBlack,
                                            focusedBorderColor = OffWhite,
                                            unfocusedBorderColor = DividerColor,
                                            focusedTextColor = OffWhite,
                                            unfocusedTextColor = OffWhite
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Yaratıcılık Derecesi (Temperature)",
                                            fontFamily = InterFontFamily,
                                            fontSize = 12.sp,
                                            color = MidGray
                                        )
                                        Text(
                                            text = "%.1f".format(temperatureVal),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 12.sp,
                                            color = OffWhite
                                        )
                                    }
                                    Slider(
                                        value = temperatureVal,
                                        onValueChange = { temperatureVal = it },
                                        valueRange = 0.0f..1.2f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = OffWhite,
                                            activeTrackColor = OffWhite,
                                            inactiveTrackColor = DividerColor
                                        )
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (nameInput.isNotBlank() && promptInput.isNotBlank()) {
                                            personasList.add(
                                                Persona(
                                                    id = java.util.UUID.randomUUID().toString(),
                                                    name = nameInput,
                                                    systemPrompt = promptInput,
                                                    icon = "🤖",
                                                    temperature = temperatureVal,
                                                    isCustom = true
                                                )
                                            )
                                            nameInput = ""
                                            promptInput = ""
                                            showCreateForm = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WhiteAccent,
                                        contentColor = PureBlack
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Personayı Oluştur",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // List of Personas
                    item {
                        Text(
                            text = "Mevcut Asistanlar",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = OffWhite,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(personasList) { persona ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Graphite)
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = persona.icon,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = persona.name,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OffWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = persona.systemPrompt,
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = MidGray,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Temp: ${"%.1f".format(persona.temperature)}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MidGray
                                    )
                                }
                            }

                            if (persona.isCustom) {
                                IconButton(
                                    onClick = { personasList.remove(persona) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Sharp.Delete,
                                        contentDescription = "Delete Persona",
                                        tint = MidGray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Curated Prompt Templates Gallery
                    item {
                        Text(
                            text = "Önerilen Prompt Şablonları",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = OffWhite,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(promptTemplates) { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Graphite)
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = template.icon,
                                    fontSize = 24.sp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.title,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = OffWhite
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = template.description,
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        color = MidGray
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DividerColor)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = template.category,
                                            fontFamily = InterFontFamily,
                                            fontSize = 10.sp,
                                            color = OffWhite
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    nameInput = template.title
                                    promptInput = template.systemPrompt
                                    showCreateForm = true
                                    activeTab = 0
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhiteAccent,
                                    contentColor = PureBlack
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "Kullan",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

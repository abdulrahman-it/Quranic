package com.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibrationEffect
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.theme.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale

// ==========================================
// DATA MODELS
// ==========================================

data class Surah(
    val id: Int,
    val nameAr: String,
    val nameEn: String,
    val translation: String,
    val type: String, // مكة, المدينة
    val verseCount: Int,
    val verses: List<Verse>
)

data class Verse(
    val id: Int,
    val textAr: String,
    val textEn: String
)

data class Thiker(
    val id: Int,
    val text: String,
    val translation: String,
    val source: String,
    val target: Int,
    val count: MutableState<Int> = mutableStateOf(0)
)

enum class AppTab(val titleAr: String, val titleEn: String, val icon: ImageVector) {
    HOME("الرئيسية", "Home", Icons.Default.Home),
    QURAN("القرآن", "Quran", Icons.Default.List),
    ATHKAR("الأذكار", "Athkar", Icons.Default.Favorite),
    PRAYER("المواقيت", "Prayers", Icons.Default.Search)
}

// ==========================================
// OFFLINE JSON LOADER & PRAYER HELPERS
// ==========================================

fun loadAthkarFromAssets(context: Context): Map<String, List<Thiker>> {
    val result = mutableMapOf<String, List<Thiker>>()
    try {
        val jsonString = context.assets.open("athkar.json").bufferedReader().use { it.readText() }
        val rootObj = JSONObject(jsonString)
        val keys = listOf("morning", "evening", "sleep", "wakeup", "duas")
        for (key in keys) {
            val arr = rootObj.getJSONArray(key)
            val list = mutableListOf<Thiker>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Thiker(
                        id = obj.getInt("id"),
                        text = obj.getString("text"),
                        translation = obj.getString("translation"),
                        source = obj.getString("source"),
                        target = obj.getInt("target")
                    )
                )
            }
            result[key] = list
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return result
}

fun calculateNextPrayerCountdown(prayerTimes: Map<String, String>): String {
    try {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMin = now.get(Calendar.MINUTE)
        val currentSec = now.get(Calendar.SECOND)
        val nowSecs = currentHour * 3600 + currentMin * 60 + currentSec

        val prayerSecs = mutableListOf<Pair<String, Int>>()
        val keys = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        for (key in keys) {
            val timeStr = prayerTimes[key] ?: continue
            val cleanStr = timeStr.replace("AM", "").replace("PM", "").trim()
            val parts = cleanStr.split(":")
            if (parts.size >= 2) {
                var hr = parts[0].toInt()
                val mn = parts[1].split(" ")[0].toInt()
                if (timeStr.contains("PM") && hr < 12) hr += 12
                if (timeStr.contains("AM") && hr == 12) hr = 0
                prayerSecs.add(Pair(key, hr * 3600 + mn * 60))
            }
        }

        prayerSecs.sortBy { it.second }

        var nextPrayer = prayerSecs.firstOrNull { it.second > nowSecs }
        var diffSecs = 0
        if (nextPrayer != null) {
            diffSecs = nextPrayer.second - nowSecs
        } else {
            val firstPrayerTomorrow = prayerSecs.firstOrNull()
            if (firstPrayerTomorrow != null) {
                diffSecs = (24 * 3600 - nowSecs) + firstPrayerTomorrow.second
            }
        }

        val h = diffSecs / 3600
        val m = (diffSecs % 3600) / 60
        val s = diffSecs % 60
        
        fun toArabicDigits(num: Int): String {
            val sStr = String.format("%02d", num)
            var res = sStr
            val arDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
            for (i in 0..9) {
                res = res.replace(i.toString(), arDigits[i].toString())
            }
            return res
        }
        
        return "${toArabicDigits(h)}:${toArabicDigits(m)}:${toArabicDigits(s)}"
    } catch (e: Exception) {
        return "٠٣:١٨:٤٧"
    }
}

// ==========================================
// PRESTIGE COLOR PALETTE WRAPPER FOR HIGHEST-FIDELITY THEMEING
// ==========================================

data class AppColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val primary: Color,
    val secondary: Color,
    val accent: Color
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember(context) { context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE) }
            var isDarkMode by remember { mutableStateOf(sharedPrefs.getBoolean("dark_mode", false)) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                val appBg = if (isDarkMode) Color(0xFF0F1512) else BgIvory
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = appBg
                ) { innerPadding ->
                    QuranCompanionApp(
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { darkMode ->
                            isDarkMode = darkMode
                            sharedPrefs.edit().putBoolean("dark_mode", darkMode).apply()
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ==========================================
// REUSABLE PREMIUM CARD MODIFIERS (No Neon / No Glassmorphism)
// ==========================================

fun Modifier.premiumCard(
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = SurfaceWhite,
    borderColor: Color = BorderCream
): Modifier = this.then(
    Modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(backgroundColor)
        .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(cornerRadius))
)

// ==========================================
// MAIN COMPONENT CONTAINER
// ==========================================

@Composable
fun QuranCompanionApp(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember(context) { context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE) }

    // Dynamic color system computed from dark mode state
    val colors = remember(isDarkMode) {
        if (isDarkMode) {
            AppColors(
                bg = Color(0xFF0F1512),
                surface = Color(0xFF161F1A),
                card = Color(0xFF222B26),
                textPrimary = Color(0xFFECE7DC),
                textSecondary = Color(0xFFA5A196),
                border = Color(0xFF233028),
                primary = Color(0xFF81C784),
                secondary = Color(0xFFE6C15C),
                accent = Color(0xFF34D399)
            )
        } else {
            AppColors(
                bg = BgIvory,
                surface = SurfaceWhite,
                card = CardSand,
                textPrimary = TextObsidian,
                textSecondary = TextSandGray,
                border = BorderCream,
                primary = PrimaryPine,
                secondary = SecondaryBronze,
                accent = AccentEmerald
            )
        }
    }

    // Persisted states
    var lastReadId by remember { mutableStateOf(sharedPrefs.getInt("last_read_id", 18)) }
    var lastReadNameAr by remember { mutableStateOf(sharedPrefs.getString("last_read_name_ar", "الكَهْف") ?: "الكَهْف") }
    var lastReadNameEn by remember { mutableStateOf(sharedPrefs.getString("last_read_name_en", "Al-Kahf") ?: "Al-Kahf") }
    var lastReadVerseIndex by remember { mutableStateOf(sharedPrefs.getInt("last_read_verse_index", 23)) }
    var lastReadTimestamp by remember { mutableStateOf(sharedPrefs.getString("last_read_timestamp", "June 22, 2026") ?: "June 22, 2026") }

    var wirdReadToday by remember { mutableStateOf(sharedPrefs.getInt("wird_read_today", 4)) }
    var wirdTarget by remember { mutableStateOf(sharedPrefs.getInt("wird_target", 10)) }

    var currentTab by remember { mutableStateOf(AppTab.HOME) }
    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var arabicFontSize by remember { mutableStateOf(24f) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }

    // Dynamic states
    var isFetchingPrayers by remember { mutableStateOf(false) }
    var prayerTimes by remember { mutableStateOf(mapOf(
        "الفجر" to "04:12 AM",
        "الشروق" to "05:48 AM",
        "الظهر" to "12:32 PM",
        "العصر" to "04:10 PM",
        "المغرب" to "07:25 PM",
        "العشاء" to "08:55 PM"
    )) }
    var hijriDateString by remember { mutableStateOf("٣ ذو الحجة ١٤٤٧ هـ") }
    var gregorianDateString by remember { mutableStateOf("June 21, 2026") }
    var countdownString by remember { mutableStateOf("٠٣:١٨:٤٧") }

    // Async Quran surah verses states
    var isFetchingVerses by remember { mutableStateOf(false) }
    val cachedVerses = remember { mutableStateMapOf<Int, List<Verse>>() }

    // Sample Datasets inside View State (Athkar load offline from local JSON asset file)
    val surahs = remember { getMockSurahs() }
    val loadedAthkar = remember(context) { loadAthkarFromAssets(context) }
    val morningAthkar = remember { loadedAthkar["morning"] ?: getMockMorningAthkar() }
    val eveningAthkar = remember { loadedAthkar["evening"] ?: getMockEveningAthkar() }
    val sleepAthkar = remember { loadedAthkar["sleep"] ?: emptyList() }
    val wakeupAthkar = remember { loadedAthkar["wakeup"] ?: emptyList() }
    val duasAthkar = remember { loadedAthkar["duas"] ?: emptyList() }
    val praisesAthkar = remember { getMockPraises() }

    // Load AlAdhan Prayer Times API on startup
    LaunchedEffect(Unit) {
        isFetchingPrayers = true
        try {
            val url = URL("https://api.aladhan.com/v1/timingsByCity?city=Makkah&country=Saudi+Arabia")
            withContext(Dispatchers.IO) {
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                if (connection.responseCode == 200) {
                    val text = connection.inputStream.bufferedReader().use { it.readText() }
                    val dataObj = JSONObject(text).getJSONObject("data")
                    val timings = dataObj.getJSONObject("timings")
                    
                    fun format12h(time24: String): String {
                        try {
                            val parts = time24.split(":")
                            if (parts.size >= 2) {
                                val hr24 = parts[0].toInt()
                                val min = parts[1]
                                val ampm = if (hr24 >= 12) "PM" else "AM"
                                val hr12 = when {
                                    hr24 == 0 -> 12
                                    hr24 > 12 -> hr24 - 12
                                    else -> hr24
                                }
                                return String.format("%02d:%s %s", hr12, min, ampm)
                            }
                        } catch (e: Exception) {}
                        return time24
                    }

                    val updatedTimes = mapOf(
                        "الفجر" to format12h(timings.getString("Fajr")),
                        "الشروق" to format12h(timings.getString("Sunrise")),
                        "الظهر" to format12h(timings.getString("Dhuhr")),
                        "العصر" to format12h(timings.getString("Asr")),
                        "المغرب" to format12h(timings.getString("Maghrib")),
                        "العشاء" to format12h(timings.getString("Isha"))
                    )

                    val dateObj = dataObj.getJSONObject("date")
                    val hijriObj = dateObj.getJSONObject("hijri")
                    val hijriDay = hijriObj.getString("day")
                    val hijriYear = hijriObj.getString("year")
                    val hijriMonth = hijriObj.getJSONObject("month").getString("ar")

                    fun toArabicNumerals(input: String): String {
                        var res = input
                        val arDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
                        for (i in 0..9) {
                            res = res.replace(i.toString(), arDigits[i].toString())
                        }
                        return res
                    }

                    val updatedHijri = "${toArabicNumerals(hijriDay)} $hijriMonth ${toArabicNumerals(hijriYear)} هـ"
                    val updatedGregor = "${dateObj.getJSONObject("gregorian").getJSONObject("month").getString("en")} ${dateObj.getJSONObject("gregorian").getString("day")}, ${dateObj.getJSONObject("gregorian").getString("year")}"

                    withContext(Dispatchers.Main) {
                        prayerTimes = updatedTimes
                        hijriDateString = updatedHijri
                        gregorianDateString = updatedGregor
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFetchingPrayers = false
        }
    }

    // Live Ticking Countdown Timer
    LaunchedEffect(prayerTimes) {
        while (true) {
            countdownString = calculateNextPrayerCountdown(prayerTimes)
            delay(1000)
        }
    }

    // Helper to fetch Surah verses with loading state and cache
    val scope = rememberCoroutineScope()
    val fetchSurahLambda: (Surah) -> Unit = { surah ->
        if (cachedVerses.containsKey(surah.id)) {
            selectedSurah = surah.copy(verses = cachedVerses[surah.id]!!)
        } else {
            // Offline fallbacks for commonly viewed Surahs (Al-Fatihah setup)
            if (surah.id == 1) {
                val fatihahVerses = listOf(
                    Verse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful."),
                    Verse(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "All praise is due to Allah, Lord of all the worlds."),
                    Verse(3, "الرَّحْمَنِ الرَّحِيمِ", "The Most Gracious, the Most Merciful."),
                    Verse(4, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense."),
                    Verse(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help."),
                    Verse(6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path -"),
                    Verse(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor...")
                )
                cachedVerses[1] = fatihahVerses
                selectedSurah = surah.copy(verses = fatihahVerses)
            } else {
                isFetchingVerses = true
                scope.launch {
                    try {
                        val fetchedList = withContext(Dispatchers.IO) {
                            val url = URL("https://api.alquran.cloud/v1/surah/${surah.id}")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000
                            if (connection.responseCode == 200) {
                                val text = connection.inputStream.bufferedReader().use { it.readText() }
                                val dataObj = JSONObject(text).getJSONObject("data")
                                val ayahs = dataObj.getJSONArray("ayahs")
                                val list = mutableListOf<Verse>()
                                for (i in 0 until ayahs.length()) {
                                    val ayah = ayahs.getJSONObject(i)
                                    list.add(
                                        Verse(
                                            id = ayah.getInt("numberInSurah"),
                                            textAr = ayah.getString("text"),
                                            textEn = ""
                                        )
                                    )
                                }
                                list
                            } else {
                                throw Exception("API Error")
                            }
                        }
                        cachedVerses[surah.id] = fetchedList
                        selectedSurah = surah.copy(verses = fetchedList)
                    } catch (e: Exception) {
                        Toast.makeText(context, "الرجاء التحقق من جودة الاتصال بالشبكة لتحميل آيات السورة الكريمة", Toast.LENGTH_LONG).show()
                    } finally {
                        isFetchingVerses = false
                    }
                }
            }
        }
    }

    val activePrayerPair = remember(prayerTimes) { findNextPrayer(prayerTimes) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant Spiritual Branding Header with live Hijri dates
            HeaderSection(hijriDate = hijriDateString, gregorianDate = gregorianDateString, colors = colors)

            // Main Content Switching Frame
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(250),
                    label = "tab_crossfade"
                ) { targetTab ->
                    when (targetTab) {
                        AppTab.HOME -> HomeScreen(
                            surahs = surahs,
                            nextPrayerName = activePrayerPair.first,
                            nextPrayerTime = activePrayerPair.second,
                            timeRemaining = countdownString,
                            lastReadId = lastReadId,
                            lastReadNameAr = lastReadNameAr,
                            lastReadNameEn = lastReadNameEn,
                            lastReadVerseIndex = lastReadVerseIndex,
                            lastReadTimestamp = lastReadTimestamp,
                            wirdReadToday = wirdReadToday,
                            wirdTarget = wirdTarget,
                            onWirdIncrement = { delta ->
                                val newValue = (wirdReadToday + delta).coerceAtLeast(0)
                                wirdReadToday = newValue
                                sharedPrefs.edit().putInt("wird_read_today", newValue).apply()
                            },
                            isDarkMode = isDarkMode,
                            colors = colors,
                            onSurahClick = { fetchSurahLambda(it) },
                            onTabNavigate = { currentTab = it }
                        )
                        AppTab.QURAN -> QuranIndexScreen(
                            surahs = surahs,
                            lastReadId = lastReadId,
                            lastReadNameAr = lastReadNameAr,
                            lastReadVerseIndex = lastReadVerseIndex,
                            colors = colors,
                            isDarkMode = isDarkMode,
                            onSurahClick = { fetchSurahLambda(it) }
                        )
                        AppTab.ATHKAR -> AthkarScreen(
                            morningList = morningAthkar,
                            eveningList = eveningAthkar,
                            sleepList = sleepAthkar,
                            wakeupList = wakeupAthkar,
                            duasList = duasAthkar,
                            praisesList = praisesAthkar,
                            hapticEnabled = hapticFeedbackEnabled,
                            colors = colors,
                            isDarkMode = isDarkMode,
                            context = context
                        )
                        AppTab.PRAYER -> PrayerScreen(
                            prayerTimes = prayerTimes,
                            hijriDate = hijriDateString,
                            gregorianDate = gregorianDateString,
                            countdownString = countdownString,
                            arabicFontSize = arabicFontSize,
                            onFontSizeChange = { arabicFontSize = it },
                            hapticEnabled = hapticFeedbackEnabled,
                            onHapticChange = { hapticFeedbackEnabled = it },
                            isDarkMode = isDarkMode,
                            onDarkModeChange = onDarkModeChange,
                            colors = colors,
                            onTriggerQibla = {
                                Toast.makeText(context, "اتجاه القبلة المباشر: ٢٩٠٫٤ درجة نحو مكة المكرمة", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }

            // Beautiful Bottom Navigation Bar
            IvoryBottomNavigation(
                selectedTab = currentTab,
                colors = colors,
                onTabSelected = { currentTab = it }
            )
        }

        // Surah Detail Reader Modal overlay
        if (selectedSurah != null) {
            BackHandler(enabled = true) {
                selectedSurah = null
            }
            SurahReaderModal(
                surah = selectedSurah!!,
                arabicFontSize = arabicFontSize,
                colors = colors,
                isDarkMode = isDarkMode,
                onBookmark = { s, verseId ->
                    lastReadId = s.id
                    lastReadNameAr = s.nameAr
                    lastReadNameEn = s.nameEn
                    lastReadVerseIndex = verseId
                    val formatter = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH)
                    lastReadTimestamp = formatter.format(java.util.Date())
                    
                    sharedPrefs.edit().apply {
                        putInt("last_read_id", s.id)
                        putString("last_read_name_ar", s.nameAr)
                        putString("last_read_name_en", s.nameEn)
                        putInt("last_read_verse_index", verseId)
                        putString("last_read_timestamp", lastReadTimestamp)
                        apply()
                    }
                },
                onClose = { selectedSurah = null }
            )
        }

        // Loading and API fetching overlay
        if (isFetchingVerses) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { /* prevent clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري تحميل الآيات العطرة...",
                            fontFamily = ArabicFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }
                }
            }
        }
    }
}

fun findNextPrayer(prayerTimes: Map<String, String>): Pair<String, String> {
    try {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMin = now.get(Calendar.MINUTE)
        val nowSecs = currentHour * 3600 + currentMin * 60

        val prayerSecs = mutableListOf<Triple<String, String, Int>>()
        val keys = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        for (key in keys) {
            val timeStr = prayerTimes[key] ?: continue
            val cleanStr = timeStr.replace("AM", "").replace("PM", "").trim()
            val parts = cleanStr.split(":")
            if (parts.size >= 2) {
                var hr = parts[0].toInt()
                val mn = parts[1].split(" ")[0].toInt()
                if (timeStr.contains("PM") && hr < 12) hr += 12
                if (timeStr.contains("AM") && hr == 12) hr = 0
                prayerSecs.add(Triple(key, timeStr, hr * 3600 + mn * 60))
            }
        }

        prayerSecs.sortBy { it.third }

        val next = prayerSecs.firstOrNull { it.third > nowSecs }
        if (next != null) {
            return Pair("صلاة ${next.first}", next.second)
        } else {
            val first = prayerSecs.firstOrNull()
            return if (first != null) {
                Pair("صلاة ${first.first} (غداً)", first.second)
            } else {
                Pair("صلاة المغرب", "07:25 PM")
            }
        }
    } catch (e: Exception) {
        return Pair("صلاة المغرب", "07:25 PM")
    }
}

// ==========================================
// SUB-COMPONENTS: SYSTEM HEADER
// ==========================================

@Composable
fun HeaderSection(hijriDate: String, gregorianDate: String, colors: AppColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
            Text(
                text = "البَوَّابَة الإِسْلَامِيَّة",
                style = TextStyle(
                    fontFamily = ArabicFontFamily,
                    fontSize = 12.sp,
                    color = colors.secondary,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "السَّلَامُ عَلَيْكُمْ وَرَحْمَةُ اللهِ",
                style = TextStyle(
                    fontFamily = ArabicFontFamily,
                    fontSize = 18.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Small Celestial Hijri Tag
        Box(
            modifier = Modifier
                .premiumCard(cornerRadius = 12.dp, backgroundColor = colors.card, borderColor = colors.border)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = hijriDate,
                    style = TextStyle(
                        fontFamily = ArabicFontFamily,
                        fontSize = 11.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = gregorianDate,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = colors.textSecondary
                    )
                )
            }
        }
    }
}

// ==========================================
// TAB 1: HOME DASHBOARD
// ==========================================

@Composable
fun HomeScreen(
    surahs: List<Surah>,
    nextPrayerName: String,
    nextPrayerTime: String,
    timeRemaining: String,
    lastReadId: Int,
    lastReadNameAr: String,
    lastReadNameEn: String,
    lastReadVerseIndex: Int,
    lastReadTimestamp: String,
    wirdReadToday: Int,
    wirdTarget: Int,
    onWirdIncrement: (Int) -> Unit,
    isDarkMode: Boolean,
    colors: AppColors,
    onSurahClick: (Surah) -> Unit,
    onTabNavigate: (AppTab) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Next Prayer Card with high-prestige highlight
        item {
            Text(
                text = "مواقيت الصلاة اليومية",
                style = TextStyle(
                    fontFamily = ArabicFontFamily,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard(
                        cornerRadius = 18.dp,
                        backgroundColor = colors.surface,
                        borderColor = colors.secondary
                    )
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "الصلاة القادمة",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ArabicFontFamily
                            )
                        }
                        Text(
                            text = nextPrayerName,
                            style = TextStyle(
                                fontFamily = ArabicFontFamily,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = nextPrayerTime,
                        style = TextStyle(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            color = colors.primary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeRemaining,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = colors.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "الوقت المتبقي للأذان:",
                            style = TextStyle(
                                fontFamily = ArabicFontFamily,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }
        }

        // Continue Reading & Daily Wird Dashboard CARD
        item {
            Text(
                text = "متابعة القراءة والورد اليومي",
                style = TextStyle(
                    fontFamily = ArabicFontFamily,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(6.dp))
            val currentLastReadSurah = surahs.find { it.id == lastReadId } ?: surahs.first()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard(cornerRadius = 18.dp, backgroundColor = colors.card, borderColor = colors.border)
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Wird progress percentage pill
                        Box(
                            modifier = Modifier
                                .background(colors.accent.copy(alpha = if (isDarkMode) 0.15f else 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val pct = if (wirdTarget > 0) (wirdReadToday * 100 / wirdTarget).coerceAtMost(100) else 0
                            Text(
                                text = "الورد اليومي: $pct%",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = ArabicFontFamily
                            )
                        }

                        // Right: Continue Reading Label
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.secondary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "موضع القراءة الأخير",
                                style = TextStyle(
                                    fontFamily = ArabicFontFamily,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Partition: Interactive Daily Wird Incrementer
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surface)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "الورد: $wirdReadToday / $wirdTarget آية",
                                fontSize = 11.sp,
                                fontFamily = ArabicFontFamily,
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Minus Button
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.card)
                                        .clickable {
                                            if (wirdReadToday > 0) onWirdIncrement(-1)
                                            triggerHapticFeedback(context, true)
                                        }
                                ) {
                                    Text("-", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Interactive mini slider or progress line
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(colors.border)
                                ) {
                                    val progressFraction = if (wirdTarget > 0) (wirdReadToday.toFloat() / wirdTarget).coerceIn(0f, 1f) else 0f
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progressFraction)
                                            .background(colors.primary)
                                    )
                                }

                                // Plus Button
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary)
                                        .clickable {
                                            onWirdIncrement(1)
                                            triggerHapticFeedback(context, true)
                                        }
                                ) {
                                    Text("+", color = if (isDarkMode) Color(0xFF0F1512) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Right Partition: Last read Surah details and Quick Reader Button
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.weight(1f).padding(start = 12.dp)
                        ) {
                            Text(
                                text = "سُورَة ${lastReadNameAr}",
                                style = TextStyle(
                                    fontFamily = ArabicNaskhFamily,
                                    fontSize = 18.sp,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "الآية $lastReadVerseIndex • $lastReadTimestamp",
                                style = TextStyle(
                                    fontFamily = ArabicFontFamily,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { 
                                    triggerHapticFeedback(context, true)
                                    onSurahClick(currentLastReadSurah) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    "تابع القراءة 📖",
                                    color = if (isDarkMode) Color(0xFF0F1512) else Color.White,
                                    fontSize = 10.sp,
                                    fontFamily = ArabicFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Actions Grid (2x2) with dynamic coloring elements
        item {
            Text(
                text = "الخدمات والتحصين السريع",
                style = TextStyle(
                    fontFamily = ArabicFontFamily,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(6.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Qiblah
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                            .clickable {
                                triggerHapticFeedback(context, true)
                                Toast
                                    .makeText(context, "اتجاه القبلة المباشر: ٢٩٠٫٤ درجة نحو مكة المكرمة", Toast.LENGTH_SHORT)
                                    .show()
                                onTabNavigate(AppTab.PRAYER)
                            }
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                            Text("🧭", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "اتجاه القبلة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontFamily = ArabicFontFamily
                            )
                            Text(
                                "تحديد الاتجاه بدقة",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = ArabicFontFamily
                            )
                        }
                    }

                    // Hisn Al-Muslim
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                            .clickable { 
                                triggerHapticFeedback(context, true)
                                onTabNavigate(AppTab.ATHKAR) 
                            }
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                            Text("🕋", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "حصن المسلم",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontFamily = ArabicFontFamily
                            )
                            Text(
                                "أدعية مأثورة شاملة",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = ArabicFontFamily
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Holy Quran
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                            .clickable { 
                                triggerHapticFeedback(context, true)
                                onTabNavigate(AppTab.QURAN) 
                            }
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                            Text("📖", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "المصحف الشريف",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontFamily = ArabicFontFamily
                            )
                            Text(
                                "قراءة وتفسير ميسر",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = ArabicFontFamily
                            )
                        }
                    }

                    // Tasbih Counter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                            .clickable { 
                                triggerHapticFeedback(context, true)
                                onTabNavigate(AppTab.ATHKAR) 
                            }
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                            Text("📿", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "المسبحة الذكية",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontFamily = ArabicFontFamily
                            )
                            Text(
                                "متابعة ورد الاستغفار",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                fontFamily = ArabicFontFamily
                            )
                        }
                    }
                }
            }
        }

        // Spiritual Statement Footer
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "“أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ”",
                    style = TextStyle(
                        fontFamily = ArabicFontFamily,
                        fontSize = 15.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(color = colors.border, blurRadius = 4f)
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ==========================================
// TAB 2: QURAN INDEX SCREEN
// ==========================================

@Composable
fun QuranIndexScreen(
    surahs: List<Surah>,
    lastReadId: Int,
    lastReadNameAr: String,
    lastReadVerseIndex: Int,
    colors: AppColors,
    isDarkMode: Boolean,
    onSurahClick: (Surah) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) surahs
        else surahs.filter {
            it.nameAr.contains(searchQuery) || it.nameEn.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // High-end Search box matching SemanticLight and dark mode guidelines
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "ابحث عن السورة الكريمة...",
                    style = TextStyle(fontFamily = ArabicFontFamily, color = colors.textSecondary, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("surah_search_field")
                .premiumCard(cornerRadius = 14.dp, backgroundColor = colors.surface, borderColor = colors.border),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            textStyle = TextStyle(
                textAlign = TextAlign.Right,
                textDirection = TextDirection.Rtl,
                fontSize = 14.sp,
                fontFamily = ArabicFontFamily
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Last Read Section as header item inside Index screen if search is empty
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (searchQuery.isBlank()) {
                item {
                    val currentLastReadSurah = surahs.find { it.id == lastReadId } ?: surahs.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumCard(cornerRadius = 18.dp, backgroundColor = colors.card, borderColor = colors.border)
                            .clickable {
                                triggerHapticFeedback(context, true)
                                onSurahClick(currentLastReadSurah)
                            }
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📖", fontSize = 24.sp)
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "تابع من حيث توقفت",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontFamily = ArabicFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "سُورَة ${lastReadNameAr} (الآية ${lastReadVerseIndex})",
                                    fontSize = 16.sp,
                                    color = colors.primary,
                                    fontFamily = ArabicNaskhFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "عناوين السور الكريمة",
                        style = TextStyle(
                            fontFamily = ArabicFontFamily,
                            fontSize = 14.sp,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }

            if (filteredSurahs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "عفوًا، لم يتم العثور على أي نتائج مطابقة.",
                            color = colors.textSecondary,
                            fontSize = 13.sp,
                            fontFamily = ArabicFontFamily
                        )
                    }
                }
            } else {
                items(filteredSurahs) { surah ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                            .clickable {
                                triggerHapticFeedback(context, true)
                                onSurahClick(surah)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Surah ${surah.nameEn}",
                                color = colors.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${surah.verseCount} آيات • ${surah.type}",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = ArabicFontFamily
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = surah.nameAr,
                                color = colors.textPrimary,
                                fontSize = 18.sp,
                                fontFamily = ArabicNaskhFamily,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(colors.card)
                                    .border(1.dp, colors.border, CircleShape)
                            ) {
                                Text(
                                    text = "${surah.id}",
                                    color = colors.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: ATHKAR SCREEN
// ==========================================

@Composable
fun AthkarScreen(
    morningList: List<Thiker>,
    eveningList: List<Thiker>,
    sleepList: List<Thiker>,
    wakeupList: List<Thiker>,
    duasList: List<Thiker>,
    praisesList: List<Thiker>,
    hapticEnabled: Boolean,
    colors: AppColors,
    isDarkMode: Boolean,
    context: Context
) {
    var activeCategoryIndex by remember { mutableStateOf(0) }
    val categories = listOf(
        "أذكار الصباح", 
        "أذكار المساء", 
        "أذكار النوم", 
        "أذكار الاستيقاظ", 
        "الأدعية اليومية", 
        "الاستغفار والتسابيح"
    )
    val currentData = when (activeCategoryIndex) {
        0 -> morningList
        1 -> eveningList
        2 -> sleepList
        3 -> wakeupList
        4 -> duasList
        else -> praisesList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // High-end category tab filters (Scrollable LazyRow with premium padding)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .premiumCard(cornerRadius = 14.dp, backgroundColor = colors.card, borderColor = colors.border)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(categories.size) { index ->
                val title = categories[index]
                val selected = index == activeCategoryIndex
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) colors.surface else Color.Transparent)
                        .clickable { activeCategoryIndex = index }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = ArabicFontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) colors.primary else colors.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Counters Floating control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clickable {
                        currentData.forEach { it.count.value = 0 }
                        triggerHapticFeedback(context, hapticEnabled)
                    }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🔄",
                    fontSize = 14.sp
                )
                Text(
                    text = "صفّر العدادات",
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444), // Crimson Red
                    fontFamily = ArabicFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = categories[activeCategoryIndex],
                fontSize = 14.sp,
                color = colors.textPrimary,
                fontFamily = ArabicFontFamily,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Rich Legible List of Athkar
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(currentData) { thiker ->
                var count by thiker.count
                val isCompleted = count >= thiker.target

                val completedColor = if (isDarkMode) Color(0xFF13271C) else Color(0xFFEBFDF3)
                val activeCardColor = colors.surface

                val cardBg = if (isCompleted) completedColor else activeCardColor
                val borderCol = if (isCompleted) colors.accent else colors.border

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                        .clickable {
                            if (count < thiker.target) {
                                count++
                                triggerHapticFeedback(context, hapticEnabled)
                            }
                        }
                        .padding(16.dp)
                ) {
                    Column {
                        // Thiker Text Calligraphy block
                        Text(
                            text = thiker.text,
                            fontSize = 18.sp,
                            fontFamily = ArabicNaskhFamily,
                            color = colors.textPrimary,
                            style = TextStyle(textDirection = TextDirection.Rtl, lineHeight = 28.sp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Metadata + Active clicker badge (with zero audio icon references)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Counter badge
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) colors.accent.copy(alpha = 0.2f) else colors.card)
                                    .border(
                                        1.dp,
                                        if (isCompleted) colors.accent else colors.border,
                                        CircleShape
                                    )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (isCompleted) {
                                        Text(
                                            text = "تم",
                                            color = colors.accent,
                                            fontSize = 12.sp,
                                            fontFamily = ArabicFontFamily,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Text(
                                            text = "$count",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.primary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(16.dp)
                                                .height(1.dp)
                                                .background(colors.textSecondary.copy(alpha = 0.3f))
                                        )
                                        Text(
                                            text = "${thiker.target}",
                                            fontSize = 9.sp,
                                            color = colors.textSecondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Source details / footnote
                            Text(
                                text = thiker.source,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontFamily = ArabicFontFamily,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: PRAYER & SETTINGS INTEGRATED
// ==========================================

@Composable
fun PrayerScreen(
    prayerTimes: Map<String, String>,
    hijriDate: String,
    gregorianDate: String,
    countdownString: String,
    arabicFontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    hapticEnabled: Boolean,
    onHapticChange: (Boolean) -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    colors: AppColors,
    onTriggerQibla: () -> Unit
) {
    val prayers = listOf(
        Pair("الفجر", prayerTimes["الفجر"] ?: "04:12 AM"),
        Pair("الشروق", prayerTimes["الشروق"] ?: "05:48 AM"),
        Pair("الظهر", prayerTimes["الظهر"] ?: "12:32 PM"),
        Pair("العصر", prayerTimes["العصر"] ?: "04:10 PM"),
        Pair("المغرب", prayerTimes["المغرب"] ?: "07:25 PM"),
        Pair("العشاء", prayerTimes["العشاء"] ?: "08:55 PM")
    )
    val activePrayerPair = remember(prayerTimes) { findNextPrayer(prayerTimes) }
    val activePrayer = activePrayerPair.first.replace("صلاة ", "").trim()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Daily prayer timer banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard(cornerRadius = 20.dp, backgroundColor = colors.card, borderColor = colors.secondary)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "متبقي للأذان القادم في مكة المكرمة",
                        fontFamily = ArabicFontFamily,
                        color = colors.secondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = countdownString,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = colors.primary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "الأذان القادم: ${activePrayerPair.first} بقامة مكة",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = ArabicFontFamily
                    )
                }
            }
        }

        // Today's prayer times checklist
        item {
            Text(
                text = "مواقيت الفريضة لليوم",
                fontFamily = ArabicFontFamily,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                prayers.forEach { prayer ->
                    val isActive = prayer.first == activePrayer
                    val cardBg = if (isActive) colors.card else colors.surface
                    val borderCol = if (isActive) colors.secondary else colors.border

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = prayer.second,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) colors.primary else colors.textSecondary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = prayer.first,
                                fontFamily = ArabicFontFamily,
                                fontSize = 14.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) colors.secondary else colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) colors.secondary else colors.border)
                             )
                        }
                    }
                }
            }
        }

        // Settings custom drawer inside system
        item {
            Text(
                text = "الإعدادات العامة للتطبيق",
                color = colors.textPrimary,
                fontFamily = ArabicFontFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Typography controller
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "مقاس خط الآيات القرآنية الكريمة",
                        color = colors.secondary,
                        fontFamily = ArabicFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.card)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                            fontSize = arabicFontSize.sp,
                            fontFamily = ArabicNaskhFamily,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = arabicFontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 18f..36f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.border
                        )
                    )

                    Text(
                        text = "عرض الحجم الحالي: ${arabicFontSize.toInt()}sp",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontFamily = ArabicFontFamily,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left
                    )
                }
            }
        }

        // Dark Mode Toggle switch
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.border
                        )
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "الوضع الداكن الفاخر",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = ArabicFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "واجهة ليلية مريحة لتلاوة هادئة",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = ArabicFontFamily
                        )
                    }
                }
            }
        }

        // Haptic Feedback switch
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .premiumCard(backgroundColor = colors.surface, borderColor = colors.border)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = onHapticChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.primary,
                            checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.border
                        )
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "الاهتزاز اللمسي الذكي (Haptic)",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontFamily = ArabicFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تغذية حسية مرتدة مع كل تسبيحة",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontFamily = ArabicFontFamily
                        )
                    }
                }
            }
        }

        // Soft visual copyrights
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "مصحف المدينة النبوية الشريف - رقمي مستدام",
                    fontSize = 11.sp,
                    color = TextSandGray,
                    fontFamily = ArabicFontFamily
                )
                Text(
                    text = "v1.0 Lightweight Quran • Jetpack Compose Native",
                    fontSize = 10.sp,
                    color = TextSandGray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS: THE QURAN READER OVERLAY (Reading-first / No Audio)
// ==========================================

@Composable
fun SurahReaderModal(
    surah: Surah,
    arabicFontSize: Float,
    colors: AppColors,
    isDarkMode: Boolean,
    onBookmark: (Surah, Int) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* intercept background clicks */ }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Reader Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dimiss Button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.border, CircleShape)
                ) {
                    Text("✕", fontSize = 12.sp, color = colors.textPrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "سورة ${surah.nameAr}",
                        fontFamily = ArabicNaskhFamily,
                        fontSize = 22.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Surah ${surah.nameEn} • ${surah.type}",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                // Star bookmark badge (stores Surah start as milestone)
                IconButton(
                    onClick = {
                        triggerHapticFeedback(context, true)
                        onBookmark(surah, 1)
                        Toast.makeText(context, "تم حفظ بداية سورة ${surah.nameAr} كعلامة رجوع!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(1.dp, colors.border, CircleShape)
                ) {
                    Text("⭐", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Surah Scroll list (Reading-first layout)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .premiumCard(cornerRadius = 20.dp, backgroundColor = colors.surface, borderColor = colors.border)
                    .padding(18.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    // Render Basmalah (except for At-Tawbah)
                    if (surah.id != 9) {
                        item {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                                style = TextStyle(
                                    fontFamily = ArabicNaskhFamily,
                                    fontSize = (arabicFontSize + 2).sp,
                                    color = colors.secondary,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }

                    items(surah.verses) { verse ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    triggerHapticFeedback(context, true)
                                    clipboardManager.setText(AnnotatedString(verse.textAr))
                                    Toast.makeText(context, "تم نسخ الآية الكريمة", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.Top
                            ) {
                                // Verses text readout
                                Text(
                                    text = verse.textAr,
                                    style = TextStyle(
                                        fontFamily = ArabicNaskhFamily,
                                        fontSize = arabicFontSize.sp,
                                        color = colors.textPrimary,
                                        lineHeight = (arabicFontSize * 1.6f).sp,
                                        textDirection = TextDirection.Rtl,
                                        textAlign = TextAlign.Right
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Elegant mini bookmark star + Ayah Number Circle Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Granular Verse Bookmark trigger
                                    IconButton(
                                        onClick = {
                                            triggerHapticFeedback(context, true)
                                            onBookmark(surah, verse.id)
                                            Toast.makeText(context, "تم حفظ موضع تلاوة الآية ${verse.id} من سورة ${surah.nameAr}!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("🔖", fontSize = 11.sp)
                                    }

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(colors.card)
                                            .border(1.dp, colors.border, CircleShape)
                                    ) {
                                        Text(
                                            text = "${verse.id}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.secondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Verse Translation field (if populated - but user asked for authentic Arabic Quran text only, let's keep English text empty or supporting it depending on API response. We can support it cleanly if present)
                            if (verse.textEn.isNotBlank()) {
                                Text(
                                    text = verse.textEn,
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        color = colors.textSecondary,
                                        lineHeight = 18.sp,
                                        textAlign = TextAlign.Left
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 42.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(colors.border)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS: SYSTEM BOTTOM NAV BAR
// ==========================================

@Composable
fun IvoryBottomNavigation(
    selectedTab: AppTab,
    colors: AppColors,
    onTabSelected: (AppTab) -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .premiumCard(cornerRadius = 20.dp, backgroundColor = colors.surface, borderColor = colors.border)
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = tab == selectedTab

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            triggerHapticFeedback(context, true)
                            onTabSelected(tab)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.titleEn,
                            tint = if (isSelected) colors.primary else colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.titleAr,
                            fontFamily = ArabicFontFamily,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.primary else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SYSTEM UTILS
// ==========================================

fun triggerHapticFeedback(context: Context, enabled: Boolean) {
    if (!enabled) return
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(45)
            }
        }
    } catch (e: Exception) {
        // Fallback gracefully on sandbox systems
    }
}

// ==========================================
// MOCK DATA GENERATOR (Strictly matching Al-Fatihah, Al-Baqarah, Al-Kahf, Yasin, Al-Mulk)
// ==========================================

// ==========================================
// COMPREHENSIVE 114 SURAHS DATA GENERATOR
// ==========================================

fun getMockSurahs(): List<Surah> {
    val medinanSurahIds = setOf(
        2, 3, 4, 5, 8, 9, 22, 24, 33, 47, 48, 49, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 76, 98, 99, 110
    )

    val verseCounts = mapOf(
        1 to 7, 2 to 286, 3 to 200, 4 to 176, 5 to 120, 6 to 165, 7 to 206, 8 to 75, 9 to 129, 10 to 109,
        11 to 123, 12 to 111, 13 to 43, 14 to 52, 15 to 99, 16 to 128, 17 to 111, 18 to 110, 19 to 98, 20 to 135,
        21 to 112, 22 to 78, 23 to 118, 24 to 64, 25 to 77, 26 to 227, 27 to 93, 28 to 88, 29 to 69, 30 to 60,
        31 to 34, 32 to 30, 33 to 73, 34 to 54, 35 to 45, 36 to 83, 37 to 182, 38 to 88, 39 to 75, 40 to 85,
        41 to 54, 42 to 53, 43 to 89, 44 to 59, 45 to 37, 46 to 35, 47 to 38, 48 to 29, 49 to 18, 50 to 45,
        51 to 60, 52 to 49, 53 to 62, 54 to 55, 55 to 78, 56 to 96, 57 to 29, 58 to 22, 59 to 24, 60 to 13,
        61 to 14, 62 to 11, 63 to 11, 64 to 18, 65 to 12, 66 to 12, 67 to 30, 68 to 52, 69 to 52, 70 to 44,
        71 to 28, 72 to 28, 73 to 20, 74 to 56, 75 to 40, 76 to 31, 77 to 50, 78 to 40, 79 to 46, 80 to 42,
        81 to 29, 82 to 19, 83 to 36, 84 to 25, 85 to 22, 86 to 17, 87 to 19, 88 to 26, 89 to 30, 90 to 20,
        91 to 15, 92 to 21, 93 to 11, 94 to 8, 95 to 8, 96 to 19, 97 to 5, 98 to 8, 99 to 8, 100 to 11,
        101 to 11, 102 to 8, 103 to 3, 104 to 9, 105 to 5, 106 to 4, 107 to 7, 108 to 3, 109 to 6, 110 to 3,
        111 to 5, 112 to 4, 113 to 5, 114 to 6
    )

    val list = listOf(
        Triple(1, "الفَاتِحَة", "Al-Fatihah" to "The Opening"),
        Triple(2, "البَقَرَة", "Al-Baqarah" to "The Cow"),
        Triple(3, "آلِ عِمْرَان", "Al-Imran" to "The Family of Imran"),
        Triple(4, "النِّسَاء", "An-Nisa" to "The Women"),
        Triple(5, "المَائِدَة", "Al-Ma'idah" to "The Table Spread"),
        Triple(6, "الأَنْعَام", "Al-An'am" to "The Cattle"),
        Triple(7, "الأَعْرَاف", "Al-A'raf" to "The Heights"),
        Triple(8, "الأَنْفَال", "Al-Anfal" to "The Spoils of War"),
        Triple(9, "التَّوْبَة", "At-Tawbah" to "The Repentance"),
        Triple(10, "يُونُس", "Yunus" to "Jonah"),
        Triple(11, "هُود", "Hud" to "Hud"),
        Triple(12, "يُوسُف", "Yusuf" to "Joseph"),
        Triple(13, "الرَّعْد", "Ar-Ra'd" to "The Thunder"),
        Triple(14, "إِبْرَاهِيم", "Ibrahim" to "Abraham"),
        Triple(15, "الحِجْر", "Al-Hijr" to "The Rocky Tract"),
        Triple(16, "النَّحْل", "An-Nahl" to "The Bee"),
        Triple(17, "الإِسْرَاء", "Al-Isra" to "The Night Journey"),
        Triple(18, "الكَهْف", "Al-Kahf" to "The Cave"),
        Triple(19, "مَرْيَم", "Maryam" to "Mary"),
        Triple(20, "طه", "Taha" to "Ta-Ha"),
        Triple(21, "الأَنْبِيَاء", "Al-Anbiya" to "The Prophets"),
        Triple(22, "الحَجّ", "Al-Hajj" to "The Pilgrimage"),
        Triple(23, "المُؤْمِنُونَ", "Al-Mu'minun" to "The Believers"),
        Triple(24, "النُّور", "An-Nur" to "The Light"),
        Triple(25, "الفُرْقَان", "Al-Furqan" to "The Criterion"),
        Triple(26, "الشُّعَرَاء", "Ash-Shu'ara" to "The Poets"),
        Triple(27, "النَّمْل", "An-Naml" to "The Ant"),
        Triple(28, "القَصَص", "Al-Qasas" to "The Stories"),
        Triple(29, "العَنْكَبُوت", "Al-Ankabut" to "The Spider"),
        Triple(30, "الرُّوم", "Ar-Rum" to "The Romans"),
        Triple(31, "لُقْمَان", "Luqman" to "Luqman"),
        Triple(32, "السَّجْدَة", "As-Sajdah" to "The Prostration"),
        Triple(33, "الأَحْزَاب", "Al-Ahzab" to "The Combined Forces"),
        Triple(34, "سَبَأ", "Saba" to "Sheba"),
        Triple(35, "فَاطِر", "Fatir" to "The Originator"),
        Triple(36, "يَاسِين", "Yasin" to "Ya-Seen"),
        Triple(37, "الصَّافَّات", "As-Saffat" to "Those who set the Ranks"),
        Triple(38, "ص", "Sad" to "The Letter Sad"),
        Triple(39, "الزُّمَر", "Az-Zumar" to "The Troops"),
        Triple(40, "غَافِر", "Ghafir" to "The Forgiver"),
        Triple(41, "فُصِّلَت", "Fussilat" to "Explained in Detail"),
        Triple(42, "الشُّورَى", "Ash-Shura" to "The Consultation"),
        Triple(43, "الزُّخْرُف", "Az-Zukhruf" to "The Ornaments of Gold"),
        Triple(44, "الدُّخَان", "Ad-Dukhan" to "The Smoke"),
        Triple(45, "الجَاثِيَة", "Al-Jathiyah" to "The Crouching"),
        Triple(46, "الأَحْقَاف", "Al-Ahqaf" to "The Wind-Curved Sandhills"),
        Triple(47, "مُحَمَّد", "Muhammad" to "Muhammad"),
        Triple(48, "الفَتْح", "Al-Fath" to "The Victory"),
        Triple(49, "الحُجُرَات", "Al-Hujurat" to "The Dwellings"),
        Triple(50, "ق", "Qaf" to "The Letter Qaf"),
        Triple(51, "الذَّارِيَات", "Adh-Dhariyat" to "The Winnowing Winds"),
        Triple(52, "الطُّور", "At-Tur" to "The Mount"),
        Triple(53, "النَّجْم", "An-Najm" to "The Star"),
        Triple(54, "القَمَر", "Al-Qamar" to "The Moon"),
        Triple(55, "الرَّحْمَن", "Ar-Rahman" to "The Beneficent"),
        Triple(56, "الوَاقِعَة", "Al-Waqi'ah" to "The Inevitable"),
        Triple(57, "الحَدِيد", "Al-Hadid" to "The Iron"),
        Triple(58, "المُجَادِلَة", "Al-Mujadilah" to "The Pleading Woman"),
        Triple(59, "الحَشْر", "Al-Hashr" to "The Exile"),
        Triple(60, "المُمْتَحَنَة", "Al-Mumtahanah" to "She that is to be examined"),
        Triple(61, "الصَّفّ", "As-Saff" to "The Ranks"),
        Triple(62, "الجُمُعَة", "Al-Jumu'ah" to "The Congregation"),
        Triple(63, "المُنَافِقُونَ", "Al-Munafiqun" to "The Hypocrites"),
        Triple(64, "التَّغَابُن", "At-Taghabun" to "The Mutual Disillusion"),
        Triple(65, "الطَّلَاق", "At-Talaq" to "The Divorce"),
        Triple(66, "التَّحْرِيم", "At-Tahrim" to "The Prohibition"),
        Triple(67, "المُلْك", "Al-Mulk" to "The Sovereignty"),
        Triple(68, "القَلَم", "Al-Qalam" to "The Pen"),
        Triple(69, "الحَاقَّة", "Al-Haqqah" to "The Reality"),
        Triple(70, "المَعَارِج", "Al-Ma'arij" to "The Ascending Stairways"),
        Triple(71, "نُوح", "Nuh" to "Noah"),
        Triple(72, "الجِنّ", "Al-Jinn" to "The Jinn"),
        Triple(73, "المُزَّمِّل", "Al-Muzzammil" to "The Enshrouded One"),
        Triple(74, "المُدَّثِّر", "Al-Muddaththir" to "The Cloaked One"),
        Triple(75, "القِيَامَة", "Al-Qiyamah" to "The Resurrection"),
        Triple(76, "الإِنْسَان", "Al-Insan" to "The Man"),
        Triple(77, "المُرْسَلَات", "Al-Mursalat" to "The Emissaries"),
        Triple(78, "النَّبَأ", "An-Naba'" to "The Tidings"),
        Triple(79, "النَّازِعَات", "An-Nazi'at" to "Those who drag forth"),
        Triple(80, "عَبَسَ", "Abasa" to "He Frowned"),
        Triple(81, "التَّكْوِير", "At-Takwir" to "The Overthrowing"),
        Triple(82, "الإِنْفِطَار", "Al-Infitar" to "The Cleaving"),
        Triple(83, "المُطَفِّفِينَ", "Al-Mutaffifin" to "The Defrauders"),
        Triple(84, "الإِنْشِقَاق", "Al-Inshiqaq" to "The Sundering"),
        Triple(85, "البُرُوج", "Al-Buruj" to "The Mansions of the Stars"),
        Triple(86, "الطَّارِق", "At-Tariq" to "The Night-Comer"),
        Triple(87, "الأَعْلَى", "Al-A'la" to "The Most High"),
        Triple(88, "الغَاشِيَة", "Al-Ghashiyah" to "The Overwhelming"),
        Triple(89, "الفَجْر", "Al-Fajr" to "The Dawn"),
        Triple(90, "البَلَد", "Al-Balad" to "The City"),
        Triple(91, "الشَّمْس", "Ash-Shams" to "The Sun"),
        Triple(92, "اللَّيْل", "Al-Layl" to "The Night"),
        Triple(93, "الضُّحَى", "Ad-Duha" to "The Morning Hours"),
        Triple(94, "الشَّرْح", "Ash-Sharh" to "The Relief"),
        Triple(95, "التِّين", "At-Tin" to "The Fig"),
        Triple(96, "العَلَق", "Al-Alaq" to "The Clot"),
        Triple(97, "القَدْر", "Al-Qadr" to "The Power"),
        Triple(98, "البَيِّنَة", "Al-Bayyinah" to "The Clear Proof"),
        Triple(99, "الزَّلْزَلَة", "Az-Zalzalah" to "The Earthquake"),
        Triple(100, "العَادِيَات", "Al-Adiyat" to "The Courser"),
        Triple(101, "القَارِعَة", "Al-Qari'ah" to "The Calamity"),
        Triple(102, "التَّكَاثُر", "At-Takathur" to "The Rivalry in world increase"),
        Triple(103, "العَصْر", "Al-Asr" to "The Declining Day"),
        Triple(104, "الهُمَزَة", "Al-Humazah" to "The Traducer"),
        Triple(105, "الفِيل", "Al-Fil" to "The Elephant"),
        Triple(106, "قُرَيْش", "Quraysh" to "Quraysh"),
        Triple(107, "المَاعُون", "Al-Ma'un" to "The Small Kindnesses"),
        Triple(108, "الكَوْثَر", "Al-Kawthar" to "The Abundance"),
        Triple(109, "الكَافِرُونَ", "Al-Kafirun" to "The Disbelievers"),
        Triple(110, "النَّصْر", "An-Nasr" to "The Divine Support"),
        Triple(111, "المَسَد", "Al-Masad" to "The Palm Fiber"),
        Triple(112, "الإِخْلَاص", "Al-Ikhlas" to "The Sincerity"),
        Triple(113, "الفَلَق", "Al-Falaq" to "The Daybreak"),
        Triple(114, "النَّاس", "An-Nas" to "Mankind")
    )

    return list.map { triple ->
        val id = triple.first
        val nameAr = triple.second
        val nameEn = triple.third.first
        val translation = triple.third.second
        val type = if (medinanSurahIds.contains(id)) "مدنية" else "مكية"
        val count = verseCounts[id] ?: 7

        // All 114 Surahs load their actual Arabic verses dynamically via AlQuran Cloud REST API.
        // We supply an empty list initially, with the exception of Surah Al-Fatihah, which has authentic offline verses.
        val verses = if (id == 1) {
            listOf(
                Verse(1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful."),
                Verse(2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "All praise is due to Allah, Lord of all the worlds."),
                Verse(3, "الرَّحْمَنِ الرَّحِيمِ", "The Most Gracious, the Most Merciful."),
                Verse(4, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense."),
                Verse(5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help."),
                Verse(6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path -"),
                Verse(7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor...")
            )
        } else {
            emptyList()
        }

        Surah(
            id = id,
            nameAr = nameAr,
            nameEn = nameEn,
            translation = translation,
            type = type,
            verseCount = count,
            verses = verses
        )
    }
}

fun getMockMorningAthkar(): List<Thiker> {
    return listOf(
        Thiker(
            id = 1,
            text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لَا شَرِيكَ لَهُ.",
            translation = "We have entered a new day and with it all dominion belongs to Allah...",
            source = "رواه مسلم والسُنّة الكبرى",
            target = 1
        ),
        Thiker(
            id = 2,
            text = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
            translation = "O Allah, by Your leave we have entered the morning...",
            source = "رواه الترمذي وأبو داود",
            target = 3
        )
    )
}

fun getMockEveningAthkar(): List<Thiker> {
    return listOf(
        Thiker(
            id = 3,
            text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَهَ إِلَّا اللهُ وَحْدَهُ لا شريكَ له.",
            translation = "We have entered the evening and with it all dominion belongs to Allah...",
            source = "رواه مسلم",
            target = 1
        ),
        Thiker(
            id = 4,
            text = "اللَّهُمَّ عافِني في بَدَني، اللَّهُمَّ عافِني في سَمْعي، اللَّهُمَّ عافِني في بَصَري، لا إلهَ إلَّا أنتَ.",
            translation = "O Allah, make me healthy in my body and my hearing...",
            source = "رواه أحمد وأبي داود",
            target = 3
        )
    )
}

fun getMockPraises(): List<Thiker> {
    return listOf(
        Thiker(
            id = 5,
            text = "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
            translation = "I seek Allah's forgiveness and repent to Him.",
            source = "تكرار مستحب لمحو الخطايا والذنوب",
            target = 33
        ),
        Thiker(
            id = 6,
            text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            translation = "Glory be to Allah and His praise, Glory be to Allah the Great.",
            source = "كلمتان خفيفتان على اللسان ثقيلتان في الميزان",
            target = 33
        ),
        Thiker(
            id = 7,
            text = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            translation = "O Allah, send blessings and peace upon our Prophet Muhammad.",
            source = "من صلى عليّ صلاة صلى الله عليه بها عشراً",
            target = 10
        )
    )
}

// ==========================================
// PREVIEWS FOR HOVER DESIGN & VIEW PORT TESTS
// ==========================================

@Preview(showBackground = true, name = "App Dashboard View")
@Composable
fun AppDashboardPreview() {
    MyApplicationTheme {
        QuranCompanionApp(isDarkMode = false, onDarkModeChange = {})
    }
}

@Preview(showBackground = true, name = "Quran Surah List View")
@Composable
fun QuranListPreview() {
    MyApplicationTheme {
        val surahs = getMockSurahs().take(5)
        val colors = AppColors(
            bg = BgIvory,
            surface = SurfaceWhite,
            card = CardSand,
            textPrimary = TextObsidian,
            textSecondary = TextSandGray,
            border = BorderCream,
            primary = PrimaryPine,
            secondary = SecondaryBronze,
            accent = AccentEmerald
        )
        Box(modifier = Modifier.background(BgIvory).padding(16.dp)) {
            QuranIndexScreen(
                surahs = surahs,
                lastReadId = 18,
                lastReadNameAr = "الكَهْف",
                lastReadVerseIndex = 1,
                colors = colors,
                isDarkMode = false,
                onSurahClick = {}
            )
        }
    }
}



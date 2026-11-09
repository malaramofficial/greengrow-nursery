package com.malaramofficial.greengrow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.launch
import java.math.BigDecimal

private val Forest = Color(0xFF174B35)
private val Leaf = Color(0xFF4F8B52)
private val Canvas = Color(0xFFF4F7F1)
private val Ink = Color(0xFF1D2B22)

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val variety: String,
    val quantity: Int,
    val pricePaise: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val delta: Int,
    val reason: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface NurseryDao {
    @Query("SELECT * FROM plants ORDER BY name COLLATE NOCASE")
    suspend fun getPlants(): List<Plant>

    @Insert
    suspend fun insertPlant(plant: Plant): Long

    @Insert
    suspend fun insertMovement(movement: StockMovement)
}

@Database(entities = [Plant::class, StockMovement::class], version = 1, exportSchema = false)
abstract class NurseryDatabase : RoomDatabase() {
    abstract fun dao(): NurseryDao
    companion object {
        @Volatile private var instance: NurseryDatabase? = null
        fun get(context: android.content.Context): NurseryDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, NurseryDatabase::class.java, "greengrow.db"
                ).build().also { instance = it }
            }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GreenGrowApp(NurseryDatabase.get(this).dao()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreenGrowApp(dao: NurseryDao) {
    val scope = rememberCoroutineScope()
    var plants by remember { mutableStateOf<List<Plant>>(emptyList()) }
    var tab by remember { mutableStateOf("होम") }
    var showAdd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(dao) { plants = dao.getPlants() }

    MaterialTheme(colorScheme = lightColorScheme(
        primary = Forest, secondary = Leaf, background = Canvas,
        surface = Color.White, onPrimary = Color.White, onBackground = Ink
    )) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("GreenGrow", color = Forest, fontWeight = FontWeight.Bold)
                            Text("नर्सरी मैनेजमेंट", color = Leaf, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAdd = true }, containerColor = Forest, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "पौधा जोड़ें")
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    listOf("होम", "पौधे", "ऑर्डर", "अधिक").forEach { label ->
                        val icon = when (label) {
                            "होम" -> Icons.Default.Home
                            "पौधे" -> Icons.Default.Inventory2
                            "ऑर्डर" -> Icons.Default.ReceiptLong
                            else -> Icons.Default.MoreHoriz
                        }
                        NavigationBarItem(
                            selected = tab == label,
                            onClick = { tab = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (tab) {
                "होम" -> Dashboard(plants, Modifier.padding(padding)) { tab = "पौधे" }
                "पौधे" -> PlantList(plants, Modifier.padding(padding))
                "ऑर्डर" -> InfoPage("ऑर्डर", "ऑर्डर और बिक्री मॉड्यूल अगले चरण में बनेगा।", Modifier.padding(padding))
                else -> InfoPage("अन्य सुविधाएँ", "रिपोर्ट, खर्च, कर्मचारी और सेटिंग्स आगे जोड़ी जाएँगी।", Modifier.padding(padding))
            }
        }

        if (showAdd) {
            AddPlantDialog(
                onDismiss = { showAdd = false },
                onSave = { name, variety, qty, price ->
                    scope.launch {
                        try {
                            dao.insertPlant(Plant(name = name, variety = variety, quantity = qty, pricePaise = price))
                            plants = dao.getPlants()
                            showAdd = false
                        } catch (e: Exception) {
                            error = e.message ?: "डेटा सेव नहीं हुआ"
                        }
                    }
                }
            )
        }
        error?.let { message ->
            AlertDialog(
                onDismissRequest = { error = null },
                confirmButton = { TextButton(onClick = { error = null }) { Text("ठीक है") } },
                title = { Text("त्रुटि") },
                text = { Text(message) }
            )
        }
    }
}

@Composable
private fun Dashboard(plants: List<Plant>, modifier: Modifier = Modifier, onAllPlants: () -> Unit) {
    val units = plants.sumOf { it.quantity }
    val low = plants.count { it.quantity in 1..10 }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = Forest, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("आपकी नर्सरी, एक नज़र में", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("पौधों का स्टॉक व्यवस्थित रखें", color = Color(0xFFD7EBDD))
                Spacer(Modifier.height(4.dp))
                Text("कुल उपलब्ध पौधे", color = Color(0xFFD7EBDD))
                Text(units.toString(), color = Color.White, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("कुल किस्में", plants.size.toString(), Modifier.weight(1f))
            MetricCard("कम स्टॉक", low.toString(), Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("हाल के पौधे", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAllPlants) { Text("सभी देखें") }
        }
        if (plants.isEmpty()) EmptyCard("अभी कोई पौधा नहीं जोड़ा गया", "पहली पौध दर्ज करने के लिए + बटन दबाएँ।")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plants.take(5), key = { it.id }) { PlantRow(it) }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color.White, tonalElevation = 1.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, color = Leaf, style = MaterialTheme.typography.labelLarge)
            Text(value, color = Forest, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlantList(plants: List<Plant>, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("पौधों का स्टॉक", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Forest)
        if (plants.isEmpty()) EmptyCard("स्टॉक खाली है", "पौधे जोड़ने के लिए + दबाएँ।")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(plants, key = { it.id }) { PlantRow(it) }
        }
    }
}

@Composable
private fun PlantRow(plant: Plant) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFFE6F1E5), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text("🌱")
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(plant.name, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(plant.variety.ifBlank { "किस्म दर्ज नहीं" }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plant.quantity.toString() + " पौधे", color = Forest, fontWeight = FontWeight.Bold)
                Text("₹" + String.format("%.2f", plant.pricePaise / 100.0), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🪴", style = MaterialTheme.typography.headlineLarge)
            Text(title, fontWeight = FontWeight.Bold, color = Forest)
            Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InfoPage(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Forest, fontWeight = FontWeight.Bold)
        EmptyCard("यह मॉड्यूल निर्माणाधीन है", message)
    }
}

@Composable
private fun AddPlantDialog(onDismiss: () -> Unit, onSave: (String, String, Int, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var variety by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नया पौधा जोड़ें", color = Forest, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("पौधे का नाम *") }, singleLine = true)
                OutlinedTextField(variety, { variety = it }, label = { Text("किस्म / वैरायटी") }, singleLine = true)
                OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("मात्रा *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(price, { price = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("प्रति पौधा कीमत (₹) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                validation?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val q = quantity.toIntOrNull()
                val p = price.toBigDecimalOrNull()
                when {
                    name.isBlank() -> validation = "पौधे का नाम जरूरी है।"
                    q == null || q < 0 -> validation = "सही मात्रा दर्ज करें।"
                    p == null || p < BigDecimal.ZERO -> validation = "सही कीमत दर्ज करें।"
                    p.scale() > 2 -> validation = "कीमत में अधिकतम दो दशमलव रखें।"
                    else -> onSave(name.trim(), variety.trim(), q, p.movePointRight(2).longValueExact())
                }
            }) { Text("सेव करें") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द करें") } }
    )
}

package ir.bayad.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = BayadStore(this)
        setContent { BayadApp(store) }
    }
}

data class Step(val title: String, val detail: String, val done: Boolean = false)
data class BayadState(val goal: String = "", val steps: List<Step> = emptyList(), val paused: Boolean = false, val theme: String = "navy", val safePath: Boolean = false)


class BayadStore(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("bayad_local", android.content.Context.MODE_PRIVATE)
    fun load(): BayadState {
        val raw = prefs.getString("state", null) ?: return BayadState()
        return try {
            val o = JSONObject(raw)
            val a = o.optJSONArray("steps") ?: JSONArray()
            val list = (0 until a.length()).map { i -> val x = a.getJSONObject(i); Step(x.optString("title"), x.optString("detail"), x.optBoolean("done")) }
            BayadState(o.optString("goal"), list, o.optBoolean("paused"), o.optString("theme", "navy"), o.optBoolean("safePath"))
        } catch (_: Exception) { BayadState() }
    }
    fun save(s: BayadState) {
        val arr = JSONArray()
        s.steps.forEach { arr.put(JSONObject().put("title", it.title).put("detail", it.detail).put("done", it.done)) }
        val o = JSONObject().put("goal", s.goal).put("steps", arr).put("paused", s.paused).put("theme", s.theme).put("safePath", s.safePath)
        prefs.edit().putString("state", o.toString()).apply()
    }
}

private data class Palette(val primary: Color, val deep: Color, val tint: Color, val name: String)
private val palettes = mapOf(
    "navy" to Palette(Color(0xFF34466B), Color(0xFF233452), Color(0xFFE9EDF5), "سرمه‌ای"),
    "blue" to Palette(Color(0xFF3174C7), Color(0xFF205391), Color(0xFFE8F2FC), "آبی"),
    "teal" to Palette(Color(0xFF23877F), Color(0xFF17655F), Color(0xFFE5F3F1), "سبز کله‌غازی"),
    "pink" to Palette(Color(0xFFC45F86), Color(0xFF98415F), Color(0xFFF9EAF0), "صورتی"),
    "peach" to Palette(Color(0xFFC8795A), Color(0xFF98573F), Color(0xFFFAEEE8), "گلبهی")
)

@Composable
fun BayadApp(store: BayadStore) {
    var state by remember { mutableStateOf(store.load()) }
    var tab by remember { mutableIntStateOf(0) }
    var showGoal by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    val p = palettes[state.theme] ?: palettes.getValue("navy")
    fun update(next: BayadState) { state = next; store.save(next) }
    CompositionLocalProvider(LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = lightColorScheme(primary = p.primary, secondary = p.deep, background = Color(0xFFF8F8FA), surface = Color.White)) {
            Scaffold(containerColor = Color(0xFFF8F8FA), bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 2.dp) {
                    listOf("خانه" to Icons.Default.Home, "مسیر من" to Icons.Default.Route, "ظاهر" to Icons.Default.Palette).forEachIndexed { i, item ->
                        NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(item.second, contentDescription = item.first) }, label = { Text(item.first, fontSize = 11.sp) })
                    }
                }
            }) { pad ->
                when (tab) {
                    0 -> HomeScreen(state, p, Modifier.padding(pad), onNew = { showGoal = true }, onTab = { tab = it }, onToggle = { index ->
                        val changed = state.steps.toMutableList(); changed[index] = changed[index].copy(done = !changed[index].done); update(state.copy(steps = changed))
                    }, onPause = { update(state.copy(paused = !state.paused)) }, onHelp = { showHelp = true })
                    1 -> PathScreen(state, p, Modifier.padding(pad), onNew = { showGoal = true }, onToggle = { index ->
                        val changed = state.steps.toMutableList(); changed[index] = changed[index].copy(done = !changed[index].done); update(state.copy(steps = changed))
                    })
                    else -> ThemeScreen(state.theme, Modifier.padding(pad), onSelect = { update(state.copy(theme = it)) })
                }
            }
            if (showGoal) GoalDialog(state.goal, onDismiss = { showGoal = false }, onSubmit = { goal ->
                val (steps, safe) = LocalPlanner.create(goal)
                update(BayadState(goal = goal, steps = steps, paused = false, theme = state.theme, safePath = safe)); showGoal = false; tab = 0
            })
            if (showHelp) HelpDialog(onDismiss = { showHelp = false })
        }
    }
}

@Composable
private fun HomeScreen(state: BayadState, p: Palette, modifier: Modifier, onNew: () -> Unit, onTab: (Int) -> Unit, onToggle: (Int) -> Unit, onPause: () -> Unit, onHelp: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 18.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(p.primary).padding(horizontal = 22.dp, vertical = 25.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("باید", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("قدم‌های کوچک، تغییرهای بزرگ", color = Color.White.copy(alpha = .86f), fontSize = 13.sp)
                    }
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = .17f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Spa, null, tint = Color.White) }
                }
                Spacer(Modifier.height(22.dp))
                if (state.goal.isBlank()) {
                    Text("سلام، خوش اومدی 🌱", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp)); Text("هر مسیر بزرگی با یک قدم مهربانانه شروع می‌شود.", color = Color.White.copy(alpha = .88f), fontSize = 14.sp)
                } else {
                    Text(if (state.paused) "مسیرت فعلاً مکث کرده" else "قدم‌به‌قدم پیش می‌ریم", color = Color.White.copy(alpha = .9f), fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp)); Text(state.goal, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(15.dp)); ProgressVisual(state.steps, Color.White)
                }
            }
        }
        item { Spacer(Modifier.height(18.dp)) }
        if (state.goal.isBlank()) {
            item { WelcomeCard(p, onNew) }
            item { Spacer(Modifier.height(14.dp)) }
            item { SampleCard(p) }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Icon(if (state.paused) Icons.Default.PlayArrow else Icons.Default.Pause, null); Spacer(Modifier.width(6.dp)); Text(if (state.paused) "ادامه مسیر" else "مکث")
                    }
                    OutlinedButton(onClick = onHelp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.FavoriteBorder, null); Spacer(Modifier.width(6.dp)); Text("کمک می‌خوام") }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            if (state.safePath) item { SafeNotice(p) }
            item { SectionTitle("قدم بعدی", "با قدم‌های کوچک جلو می‌ریم") }
            val current = state.steps.indexOfFirst { !it.done }.let { if (it < 0) state.steps.lastIndex else it }
            itemsIndexed(state.steps.take(2)) { index, step -> StepCard(index, step, isLocked(index, state.steps), index == current, p, onToggle) }
            item { TextButton(onClick = { onTab(1) }, modifier = Modifier.fillMaxWidth()) { Text("دیدن همه‌ی مسیر  ←") } }
            item { TextButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("تغییر هدف یا شروع دوباره") } }
        }
    }
}

@Composable
private fun ProgressVisual(steps: List<Step>, color: Color) {
    val count = steps.count { it.done }; val fraction = if (steps.isEmpty()) 0f else count.toFloat() / steps.size
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = .28f))) {
            Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(CircleShape).background(Color.White))
        }
        Spacer(Modifier.width(10.dp)); Text("$count از ${steps.size}", color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun WelcomeCard(p: Palette, onNew: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AutoAwesome, null, tint = p.primary); Spacer(Modifier.width(9.dp)); Text("از کجا شروع کنیم؟", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            Spacer(Modifier.height(8.dp)); Text("یک خواسته یا هدف شخصی بنویس. با هم آن را به قدم‌های ساده و شدنی تبدیل می‌کنیم.", color = Color(0xFF666A73), fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp)); Button(onClick = onNew, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(7.dp)); Text("ساختن مسیر من") }
        }
    }
}

@Composable
private fun SampleCard(p: Palette) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = p.tint)) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Lightbulb, null, tint = p.deep); Spacer(Modifier.width(12.dp))
            Column { Text("یک نمونه‌ی کوچک", fontWeight = FontWeight.Bold, color = p.deep); Spacer(Modifier.height(4.dp)); Text("«می‌خوام هر روز کمی مطالعه کنم»؛ شروع کوتاه هم ارزشمنده.", color = Color(0xFF505563), fontSize = 13.sp, lineHeight = 20.sp) }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) { Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Color(0xFF777B84), fontSize = 12.sp) }
}

private fun isLocked(index: Int, steps: List<Step>) = index > 0 && (0 until index).any { !steps[it].done }

@Composable
private fun StepCard(index: Int, step: Step, locked: Boolean, active: Boolean, p: Palette, onToggle: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(if (active && !locked) 2.dp else 0.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 4.dp).size(29.dp).clip(CircleShape).background(if (step.done) p.primary else if (locked) Color(0xFFF0F1F3) else p.tint), contentAlignment = Alignment.Center) {
                if (step.done) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(17.dp))
                else if (locked) Icon(Icons.Default.Lock, null, tint = Color(0xFF9296A0), modifier = Modifier.size(15.dp))
                else Text("${index + 1}", color = p.deep, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(step.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f)); if (locked && step.done) Text("نیاز به مرور", color = p.deep, fontSize = 10.sp) }
                Spacer(Modifier.height(4.dp)); Text(step.detail, color = Color(0xFF6F737C), fontSize = 12.sp, lineHeight = 19.sp)
            }
            Spacer(Modifier.width(4.dp)); Checkbox(checked = step.done, onCheckedChange = { if (!locked) onToggle(index) }, enabled = !locked, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun SafeNotice(p: Palette) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F7))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Favorite, null, tint = p.deep); Spacer(Modifier.width(9.dp)); Text("این مسیر برای مکث، دیدن پیامدهای احتمالی و پیدا کردن راه‌های قانونی و امن طراحی شده؛ هیچ پیامد حقوقی را قطعی فرض نمی‌کند.", fontSize = 12.sp, color = Color(0xFF515866), lineHeight = 19.sp) }
    }
}

@Composable
private fun PathScreen(state: BayadState, p: Palette, modifier: Modifier, onNew: () -> Unit, onToggle: (Int) -> Unit) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(p.primary).padding(22.dp)) { Text("مسیر من", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(state.goal.ifBlank { "قدم‌هایت را اینجا می‌بینی" }, color = Color.White.copy(alpha = .9f), fontSize = 14.sp) }
        if (state.steps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Route, null, tint = p.primary, modifier = Modifier.size(46.dp)); Spacer(Modifier.height(12.dp)); Text("هنوز مسیری نساختی", fontWeight = FontWeight.Bold); Text("یک هدف بنویس تا قدم‌هایت را بچینیم.", color = Color.Gray); Spacer(Modifier.height(12.dp)); Button(onClick = onNew) { Text("ساختن مسیر") } } }
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 12.dp, bottom = 20.dp)) {
                item { Card(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Column(Modifier.padding(16.dp)) { Text(if (state.paused) "مسیر در مکث است" else "پیشرفت تو", fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp)); val done = state.steps.count { it.done }; LinearProgressIndicator(progress = { done.toFloat() / state.steps.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = p.primary, trackColor = p.tint); Spacer(Modifier.height(6.dp)); Text("$done از ${state.steps.size} قدم انجام شده", fontSize = 12.sp, color = Color.Gray) } } }
                if (state.safePath) item { SafeNotice(p) }
                itemsIndexed(state.steps) { index, step -> StepCard(index, step, isLocked(index, state.steps), !isLocked(index, state.steps), p, onToggle) }
                item { TextButton(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Text("تغییر هدف یا شروع دوباره") } }
            }
        }
    }
}

@Composable
private fun ThemeScreen(selected: String, modifier: Modifier, onSelect: (String) -> Unit) {
    val choices = listOf("navy", "blue", "teal", "pink", "peach")
    Column(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(palettes.getValue(selected).primary).padding(22.dp)) { Text("ظاهر برنامه", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text("رنگی انتخاب کن که حال خوبی بهت می‌دهد.", color = Color.White.copy(alpha = .9f), fontSize = 13.sp) }
        Column(Modifier.padding(18.dp)) {
            Text("تم‌های رنگی", fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
            choices.forEach { key ->
                val c = palettes.getValue(key)
                Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onSelect(key) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(c.primary))
                        Spacer(Modifier.width(13.dp)); Text(c.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        if (selected == key) Icon(Icons.Default.CheckCircle, null, tint = c.primary)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = palettes.getValue(selected).tint)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.Palette, null, tint = palettes.getValue(selected).deep); Spacer(Modifier.width(10.dp)); Text("رنگ منتخب در بخش معرفی و سربرگ‌ها حضور پررنگ دارد؛ سطح‌های سفید و روشن، خوانایی و تمرکز روی قدم‌ها را نگه می‌دارند.", fontSize = 12.sp, lineHeight = 19.sp) }
            }
        }
    }
}

@Composable
private fun GoalDialog(current: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("همه‌ی سنین") }
    var category by remember { mutableStateOf("همه‌ی دسته‌ها") }
    var categoriesOpen by remember { mutableStateOf(false) }
    val ages = listOf("همه‌ی سنین") + GoalCatalog.all.map { it.ageRange }.distinct()
    val filtered = GoalCatalog.all.filter {
        (age == "همه‌ی سنین" || it.ageRange == age) &&
        (category == "همه‌ی دسته‌ها" || it.category == category) &&
        (query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) || it.category.contains(query.trim(), ignoreCase = true))
    }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(24.dp),
        title = { Text(if (current.isBlank()) "هدفت را انتخاب کن" else "یک مسیر تازه بساز", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.heightIn(max = 560.dp)) {
                Text("از پیشنهادهای محلی جست‌وجو کن یا هدف خودت را بنویس.", fontSize = 13.sp, color = Color(0xFF666A73))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("جست‌وجوی پیشنهادها") }, shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ages.forEach { stage -> FilterChip(selected = age == stage, onClick = { age = stage }, label = { Text(stage, fontSize = 11.sp) }) }
                }
                Box {
                    OutlinedButton(onClick = { categoriesOpen = true }) { Text(category); Icon(Icons.Default.ArrowDropDown, null) }
                    DropdownMenu(expanded = categoriesOpen, onDismissRequest = { categoriesOpen = false }) {
                        (listOf("همه‌ی دسته‌ها") + GoalCatalog.categories).forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { category = c; categoriesOpen = false }) }
                    }
                }
                Text("${filtered.size} پیشنهاد", color = Color.Gray, fontSize = 11.sp)
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    itemsIndexed(filtered) { _, suggestion ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { onSubmit(suggestion.title) }, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8))) {
                            Column(Modifier.padding(10.dp)) {
                                Text(suggestion.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${suggestion.category} • ${suggestion.ageRange}", color = Color(0xFF6F737C), fontSize = 10.sp)
                                Text("گام‌ها: ${suggestion.steps.take(2).joinToString("؛ ") { it.first }}", color = Color(0xFF777B84), fontSize = 10.sp, maxLines = 2)
                            }
                        }
                    }
                    if (filtered.isEmpty()) item { Text("پیشنهادی پیدا نشد؛ عبارت دیگری جست‌وجو کن.", Modifier.padding(12.dp), color = Color.Gray) }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("یا هدف خودت را بنویس") }, minLines = 1, maxLines = 2, shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = { Button(onClick = { if (text.isNotBlank()) onSubmit(text.trim()) }, enabled = text.isNotBlank(), shape = RoundedCornerShape(12.dp)) { Text("ساختن مسیر من") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("فعلاً نه") } })
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(24.dp), icon = { Icon(Icons.Default.Favorite, null, tint = Color(0xFFC45F86)) }, title = { Text("کمک خواستن یعنی شجاعت", fontWeight = FontWeight.Bold) }, text = { Text("""لازم نیست همه‌چیز را یک‌باره حل کنی. چه چیزی الان سخت شده؟

می‌توانی قدم را کوچک‌تر کنی، کمی استراحت کنی یا با یک آدم امن و مورداعتماد حرف بزنی. اگر خطر فوری وجود دارد، از خدمات اضطراری محل کمک بگیر.""", fontSize = 14.sp, lineHeight = 22.sp) }, confirmButton = { TextButton(onClick = onDismiss) { Text("ممنونم") } })
}

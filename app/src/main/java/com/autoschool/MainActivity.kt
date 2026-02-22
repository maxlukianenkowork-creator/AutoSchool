package com.autoschool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoschool.data.AppDatabase
import com.autoschool.data.LessonEntity
import com.autoschool.data.StudentEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.getInstance(this)
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.Factory(db.dao()))
            AppRoot(vm)
        }
    }
}

@Composable
private fun AppRoot(vm: MainViewModel) {
    var authorized by remember { mutableStateOf(false) }
    if (!authorized) {
        LoginScreen { authorized = true }
    } else {
        MainTabs(vm)
    }
}

@Composable
private fun LoginScreen(onSuccess: () -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Вход", style = MaterialTheme.typography.headlineSmall)
        Text("MVP-авторизация (локальная): используйте любой логин и пароль")
        OutlinedTextField(value = login, onValueChange = { login = it }, label = { Text("Логин / телефон") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль / SMS-код") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = onSuccess, modifier = Modifier.fillMaxWidth()) {
            Text("Войти")
        }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ученики", "Занятия", "Календарь", "Финансы", "Отчёт")

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = {}, label = { Text(label) })
                }
            }
        }
    ) { innerPadding ->
        when (tab) {
            0 -> StudentsScreen(vm, Modifier.padding(innerPadding))
            1 -> LessonsScreen(vm, Modifier.padding(innerPadding))
            2 -> CalendarScreen(vm, Modifier.padding(innerPadding))
            3 -> PaymentsScreen(vm, Modifier.padding(innerPadding))
            else -> ReportScreen(vm, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun StudentsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val students by vm.students.collectAsStateWithLifecycle()
    val progress by vm.studentProgress.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ученики", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(name, { name = it }, label = { Text("ФИО") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            vm.addStudent(name, phone, "2026-01-01", "B", 20.0, 1500.0, "")
            name = ""
            phone = ""
        }, modifier = Modifier.fillMaxWidth()) { Text("Добавить ученика") }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(students, key = { it.id }) { student ->
                val p = progress[student.id]
                var openEditDialog by remember(student.id) { mutableStateOf(false) }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(student.fullName)
                        Text("${student.phone} • Категория ${student.licenseCategory}")
                        Text("Откатанные часы: ${"%.1f".format(p?.completedHours ?: 0.0)}")
                        Text("Проплаченные часы (всего): ${"%.1f".format(student.prepaidHours)}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { openEditDialog = true }) {
                                Text("Редактировать")
                            }
                            Button(onClick = {
                                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${student.phone}"))
                                context.startActivity(dialIntent)
                            }) {
                                Text("Позвонить")
                            }
                        }
                    }
                }

                if (openEditDialog) {
                    EditStudentDialog(
                        student = student,
                        onDismiss = { openEditDialog = false },
                        onSave = { updated ->
                            vm.updateStudent(
                                studentId = updated.id,
                                fullName = updated.fullName,
                                phone = updated.phone,
                                startDate = updated.startDate,
                                licenseCategory = updated.licenseCategory,
                                prepaidHours = updated.prepaidHours,
                                hourlyRate = updated.hourlyRate,
                                notes = updated.notes
                            )
                            openEditDialog = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun EditStudentDialog(
    student: StudentEntity,
    onDismiss: () -> Unit,
    onSave: (StudentEntity) -> Unit
) {
    var fullName by remember(student.id) { mutableStateOf(student.fullName) }
    var phone by remember(student.id) { mutableStateOf(student.phone) }
    var startDate by remember(student.id) { mutableStateOf(student.startDate) }
    var category by remember(student.id) { mutableStateOf(student.licenseCategory) }
    var prepaid by remember(student.id) { mutableStateOf(student.prepaidHours.toString()) }
    var rate by remember(student.id) { mutableStateOf(student.hourlyRate.toString()) }
    var notes by remember(student.id) { mutableStateOf(student.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование ученика") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(fullName, { fullName = it }, label = { Text("ФИО") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Телефон") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(startDate, { startDate = it }, label = { Text("Дата начала") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Категория") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(prepaid, { prepaid = it }, label = { Text("Проплаченные часы") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rate, { rate = it }, label = { Text("Стоимость часа") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Примечания") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    student.copy(
                        fullName = fullName,
                        phone = phone,
                        startDate = startDate,
                        licenseCategory = category,
                        prepaidHours = prepaid.toDoubleOrNull() ?: student.prepaidHours,
                        hourlyRate = rate.toDoubleOrNull() ?: student.hourlyRate,
                        notes = notes
                    )
                )
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun LessonsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val students by vm.students.collectAsStateWithLifecycle()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val monthFormatter = DateTimeFormatter.ofPattern("MM.yyyy")
    val hours = (6..21).toList()

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Занятия: почасовой график на месяц", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) { Text("<") }
            Text("Месяц: ${selectedMonth.format(monthFormatter)}", modifier = Modifier.padding(top = 12.dp))
            Button(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) { Text(">") }
        }

        val lessonsInMonth = lessons.filter {
            runCatching { YearMonth.from(LocalDate.parse(it.date)) }.getOrNull() == selectedMonth
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((1..selectedMonth.lengthOfMonth()).toList()) { day ->
                val date = selectedMonth.atDay(day)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${date.dayOfMonth}.${date.monthValue}.${date.year}")
                        hours.forEach { hour ->
                            val slot = String.format("%02d:00", hour)
                            val lessonAtSlot = lessonsInMonth.firstOrNull {
                                it.date == date.toString() && lessonCoversHour(it.startTime, it.durationHours, hour)
                            }
                            if (lessonAtSlot == null) {
                                Text("$slot: свободно")
                            } else {
                                val student = students.firstOrNull { it.id == lessonAtSlot.studentId }
                                val studentName = student?.fullName ?: "Ученик"
                                val completedNow = completedHoursAtMoment(
                                    studentId = lessonAtSlot.studentId,
                                    lessons = lessons,
                                    date = date,
                                    hour = hour
                                )
                                val prepaid = student?.prepaidHours ?: 0.0
                                Text("$slot: $studentName • ${"%.1f".format(completedNow)}/${"%.1f".format(prepaid)} ч")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val students by vm.students.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()

    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var openLessonDialog by remember { mutableStateOf(false) }
    var editingLesson by remember { mutableStateOf<LessonEntity?>(null) }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Календарь", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Дата (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(onClick = { openLessonDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Открыть окно выбора времени")
        }

        val lessonsByDate = lessons.filter { it.date == date }.sortedBy { it.startTime }
        Text("Занятия на дату: ${lessonsByDate.size}")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lessonsByDate, key = { it.id }) { lesson ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val studentName = students.firstOrNull { it.id == lesson.studentId }?.fullName ?: "Ученик"
                        Text("${lesson.startTime}-${lesson.endTime} • $studentName")
                        Text("Тема: ${lesson.topics}")
                        Text("Оценка: ${lesson.rating}")
                        Text("Длительность: ${lesson.durationHours} ч")
                        Button(onClick = {
                            editingLesson = lesson
                            openLessonDialog = true
                        }) {
                            Text("Редактировать")
                        }
                    }
                }
            }
        }
    }

    if (openLessonDialog) {
        LessonDialog(
            date = date,
            students = students,
            editingLesson = editingLesson,
            onDismiss = {
                openLessonDialog = false
                editingLesson = null
            },
            onSave = { selectedStudentId, selectedDate, startTime, duration, topic, rating ->
                if (editingLesson == null) {
                    val end = endTimeFrom(startTime, duration)
                    vm.addLesson(
                        studentId = selectedStudentId,
                        date = selectedDate,
                        startTime = startTime,
                        endTime = end,
                        durationHours = duration,
                        type = "Город",
                        topics = topic,
                        rating = rating,
                        comment = "Добавлено из календаря",
                        isPaid = true
                    )
                } else {
                    vm.updateLesson(
                        lessonId = editingLesson!!.id,
                        date = selectedDate,
                        startTime = startTime,
                        durationHours = duration,
                        topics = topic,
                        rating = rating,
                        studentId = selectedStudentId
                    )
                }
                openLessonDialog = false
                editingLesson = null
            }
        )
    }
}

@Composable
private fun LessonDialog(
    date: String,
    students: List<StudentEntity>,
    editingLesson: LessonEntity?,
    onDismiss: () -> Unit,
    onSave: (studentId: Long, date: String, startTime: String, duration: Double, topic: String, rating: Int) -> Unit
) {
    var selectedStudentId by remember { mutableLongStateOf(editingLesson?.studentId ?: students.firstOrNull()?.id ?: 0L) }
    var selectedDate by remember { mutableStateOf(editingLesson?.date ?: date) }
    var startTime by remember { mutableStateOf(editingLesson?.startTime ?: "10:00") }
    var duration by remember { mutableStateOf((editingLesson?.durationHours ?: 1.0).toString()) }
    var topic by remember { mutableStateOf(editingLesson?.topics ?: "") }
    var rating by remember { mutableStateOf((editingLesson?.rating ?: 5).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingLesson == null) "Добавить урок" else "Редактировать урок") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(selectedDate, { selectedDate = it }, label = { Text("Дата YYYY-MM-DD") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(startTime, { startTime = it }, label = { Text("Время начала HH:MM") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(duration, { duration = it }, label = { Text("Длительность (ч)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(topic, { topic = it }, label = { Text("Тема") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(rating, { rating = it }, label = { Text("Оценка (1-5)") }, modifier = Modifier.fillMaxWidth())

                Text("Ученик")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    students.forEach { student ->
                        FilterChip(selected = selectedStudentId == student.id, onClick = { selectedStudentId = student.id }, label = { Text(student.fullName) })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    selectedStudentId,
                    selectedDate,
                    startTime,
                    duration.toDoubleOrNull() ?: 1.0,
                    topic,
                    (rating.toIntOrNull() ?: 5).coerceIn(1, 5)
                )
            }) { Text("Сохранить") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Отмена") }
        }
    )
}



private fun completedHoursAtMoment(
    studentId: Long,
    lessons: List<LessonEntity>,
    date: LocalDate,
    hour: Int
): Double {
    val momentMinutes = hour * 60
    return lessons
        .filter { it.studentId == studentId }
        .sumOf { lesson ->
            val lessonDate = runCatching { LocalDate.parse(lesson.date) }.getOrNull() ?: return@sumOf 0.0
            if (lessonDate.isBefore(date)) return@sumOf lesson.durationHours
            if (lessonDate.isAfter(date)) return@sumOf 0.0

            val parts = lesson.startTime.split(":")
            val startH = parts.getOrNull(0)?.toIntOrNull() ?: return@sumOf 0.0
            val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val startMinutes = startH * 60 + startM
            val endMinutes = startMinutes + (lesson.durationHours * 60).toInt()

            when {
                endMinutes <= momentMinutes -> lesson.durationHours
                startMinutes >= momentMinutes -> 0.0
                else -> (momentMinutes - startMinutes).coerceAtLeast(0) / 60.0
            }
        }
}

private fun lessonCoversHour(startTime: String, durationHours: Double, hour: Int): Boolean {
    val parts = startTime.split(":")
    val startH = parts.getOrNull(0)?.toIntOrNull() ?: return false
    val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val startMinutes = startH * 60 + startM
    val endMinutes = startMinutes + (durationHours * 60).toInt()
    val slotStart = hour * 60
    return slotStart >= startMinutes && slotStart < endMinutes
}

private fun endTimeFrom(startTime: String, durationHours: Double): String {
    val parts = startTime.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val totalMinutes = h * 60 + m + (durationHours * 60).toInt()
    val endH = (totalMinutes / 60).coerceAtMost(23)
    val endM = totalMinutes % 60
    return String.format("%02d:%02d", endH, endM)
}

@Composable
private fun PaymentsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val students by vm.students.collectAsStateWithLifecycle()
    val payments by vm.payments.collectAsStateWithLifecycle()
    var amount by remember { mutableStateOf("1500") }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Финансы", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(amount, { amount = it }, label = { Text("Сумма") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val first = students.firstOrNull() ?: return@Button
            vm.addPayment(first.id, LocalDate.now().toString(), amount.toDoubleOrNull() ?: 0.0, "Наличные")
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Добавить оплату")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(payments, key = { it.id }) { payment ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${payment.paymentDate} • ${payment.amount} ₽")
                        Text("Метод: ${payment.paymentMethod}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val dashboard by vm.dashboard.collectAsStateWithLifecycle()
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Базовый отчёт (MVP)", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Всего учеников: ${dashboard.studentsCount}")
                Text("Активных учеников: ${dashboard.activeStudents}")
                Text("Проведено занятий: ${dashboard.lessonsCount}")
                Text("Общий доход: ${dashboard.totalIncome} ₽")
            }
        }
        Text("Приложение работает оффлайн: данные хранятся в Room и доступны без интернета.")
    }
}

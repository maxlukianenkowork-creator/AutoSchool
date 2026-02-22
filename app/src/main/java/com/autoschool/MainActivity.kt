package com.autoschool

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoschool.data.AppDatabase

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
    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf("Ученики", "Занятия", "Финансы", "Отчёт").forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = {}, label = { Text(label) })
                }
            }
        }
    ) { innerPadding ->
        when (tab) {
            0 -> StudentsScreen(vm, Modifier.padding(innerPadding))
            1 -> LessonsScreen(vm, Modifier.padding(innerPadding))
            2 -> PaymentsScreen(vm, Modifier.padding(innerPadding))
            else -> ReportScreen(vm, Modifier.padding(innerPadding))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val students by vm.students.collectAsStateWithLifecycle()
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
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(student.fullName)
                        Text("${student.phone} • Категория ${student.licenseCategory}")
                        Text("Предоплачено: ${student.prepaidHours} ч")
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonsScreen(vm: MainViewModel, modifier: Modifier = Modifier) {
    val students by vm.students.collectAsStateWithLifecycle()
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    var duration by remember { mutableStateOf("1.5") }
    var paid by remember { mutableStateOf(false) }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Занятия", style = MaterialTheme.typography.headlineSmall)
        Text("Создание занятия для первого ученика в списке")
        OutlinedTextField(duration, onValueChange = { duration = it }, label = { Text("Длительность (часы)") }, modifier = Modifier.fillMaxWidth())
        Row {
            Checkbox(checked = paid, onCheckedChange = { paid = it })
            Text("Отметить как оплачено")
        }
        Button(onClick = {
            val first = students.firstOrNull() ?: return@Button
            vm.addLesson(first.id, "2026-01-02", "10:00", "11:30", duration.toDoubleOrNull() ?: 1.0, "Город", "Манёвры", 4, "Хороший прогресс", paid)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Добавить занятие")
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lessons, key = { it.id }) { lesson ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${lesson.date} • ${lesson.lessonType}")
                        Text("${lesson.startTime}-${lesson.endTime}, ${lesson.durationHours} ч")
                        Text("Тема: ${lesson.topics}, Оценка: ${lesson.rating}")
                    }
                }
            }
        }
    }
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
            vm.addPayment(first.id, "2026-01-03", amount.toDoubleOrNull() ?: 0.0, "Наличные")
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

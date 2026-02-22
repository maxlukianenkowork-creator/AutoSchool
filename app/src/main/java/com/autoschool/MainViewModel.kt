package com.autoschool

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.autoschool.data.AppDao
import com.autoschool.data.LessonEntity
import com.autoschool.data.PaymentEntity
import com.autoschool.data.StudentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

data class DashboardState(
    val studentsCount: Int = 0,
    val activeStudents: Int = 0,
    val lessonsCount: Int = 0,
    val totalIncome: Double = 0.0
)

data class StudentProgress(
    val studentId: Long,
    val completedLessons: Int,
    val completedHours: Double,
    val remainingPaidHours: Double
)

class MainViewModel(private val dao: AppDao) : ViewModel() {
    val students = dao.observeStudents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val lessons = dao.observeLessons().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val payments = dao.observePayments().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val totalIncome = MutableStateFlow(0.0)

    val dashboard: StateFlow<DashboardState> = combine(students, lessons, totalIncome) { studentsList, lessonsList, income ->
        DashboardState(
            studentsCount = studentsList.size,
            activeStudents = studentsList.count { it.isActive },
            lessonsCount = lessonsList.size,
            totalIncome = income
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    val studentProgress: StateFlow<Map<Long, StudentProgress>> = combine(students, lessons) { studentsList, lessonsList ->
        studentsList.associate { student ->
            val studentLessons = lessonsList.filter { it.studentId == student.id }
            val completedHours = studentLessons.sumOf { it.durationHours }
            student.id to StudentProgress(
                studentId = student.id,
                completedLessons = studentLessons.size,
                completedHours = completedHours,
                remainingPaidHours = max(0.0, student.prepaidHours - completedHours)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        refreshIncome()
    }

    fun addStudent(
        fullName: String,
        phone: String,
        startDate: String,
        category: String,
        prepaidHours: Double,
        hourlyRate: Double,
        notes: String
    ) {
        if (fullName.isBlank()) return
        viewModelScope.launch {
            dao.insertStudent(
                StudentEntity(
                    fullName = fullName,
                    phone = phone,
                    startDate = startDate,
                    licenseCategory = category,
                    prepaidHours = prepaidHours,
                    hourlyRate = hourlyRate,
                    notes = notes
                )
            )
        }
    }

    fun updateStudent(
        studentId: Long,
        fullName: String,
        phone: String,
        startDate: String,
        licenseCategory: String,
        prepaidHours: Double,
        hourlyRate: Double,
        notes: String
    ) {
        viewModelScope.launch {
            dao.updateStudent(
                studentId = studentId,
                fullName = fullName,
                phone = phone,
                startDate = startDate,
                licenseCategory = licenseCategory,
                prepaidHours = prepaidHours.coerceAtLeast(0.0),
                hourlyRate = hourlyRate.coerceAtLeast(0.0),
                notes = notes
            )
        }
    }

    fun addLesson(
        studentId: Long,
        date: String,
        startTime: String,
        endTime: String,
        durationHours: Double,
        type: String,
        topics: String,
        rating: Int,
        comment: String,
        isPaid: Boolean
    ) {
        viewModelScope.launch {
            dao.insertLesson(
                LessonEntity(
                    studentId = studentId,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    durationHours = durationHours,
                    lessonType = type,
                    topics = topics,
                    rating = rating,
                    instructorComment = comment,
                    isPaid = isPaid
                )
            )
        }
    }

    fun updateLesson(
        lessonId: Long,
        date: String,
        startTime: String,
        durationHours: Double,
        topics: String,
        rating: Int,
        studentId: Long
    ) {
        viewModelScope.launch {
            val safeDuration = durationHours.coerceAtLeast(0.5)
            val endTime = endTimeFrom(startTime, safeDuration)
            dao.updateLesson(
                lessonId = lessonId,
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationHours = safeDuration,
                topics = topics,
                rating = rating.coerceIn(1, 5),
                studentId = studentId
            )
        }
    }

    private fun endTimeFrom(startTime: String, durationHours: Double): String {
        val (h, m) = startTime.split(":").mapNotNull { it.toIntOrNull() }
            .let { if (it.size == 2) it else listOf(8, 0) }
        val totalMinutes = h * 60 + m + (durationHours * 60).toInt()
        val endH = (totalMinutes / 60).coerceAtMost(23)
        val endM = totalMinutes % 60
        return String.format("%02d:%02d", endH, endM)
    }

    fun addPayment(studentId: Long, date: String, amount: Double, method: String) {
        viewModelScope.launch {
            dao.insertPayment(PaymentEntity(studentId = studentId, paymentDate = date, amount = amount, paymentMethod = method))
            refreshIncome()
        }
    }

    private fun refreshIncome() {
        viewModelScope.launch {
            totalIncome.value = dao.totalIncome()
        }
    }

    class Factory(private val dao: AppDao) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(dao) as T
        }
    }
}

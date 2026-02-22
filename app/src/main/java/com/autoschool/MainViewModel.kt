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

data class DashboardState(
    val studentsCount: Int = 0,
    val activeStudents: Int = 0,
    val lessonsCount: Int = 0,
    val totalIncome: Double = 0.0
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

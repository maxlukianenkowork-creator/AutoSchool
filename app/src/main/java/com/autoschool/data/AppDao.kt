package com.autoschool.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM students ORDER BY fullName")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Long)

    @Query("SELECT * FROM lessons ORDER BY date DESC, startTime DESC")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Query("UPDATE lessons SET topics = :topics, rating = :rating WHERE id = :lessonId")
    suspend fun updateLessonDetails(lessonId: Long, topics: String, rating: Int)

    @Query("SELECT COALESCE(SUM(durationHours), 0) FROM lessons WHERE studentId = :studentId")
    suspend fun totalHoursCompleted(studentId: Long): Double

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun observePayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments")
    suspend fun totalIncome(): Double
}

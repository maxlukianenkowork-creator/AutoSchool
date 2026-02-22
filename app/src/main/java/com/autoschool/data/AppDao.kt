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

    @Query(
        """
        UPDATE students
        SET fullName = :fullName,
            phone = :phone,
            startDate = :startDate,
            licenseCategory = :licenseCategory,
            prepaidHours = :prepaidHours,
            hourlyRate = :hourlyRate,
            notes = :notes
        WHERE id = :studentId
        """
    )
    suspend fun updateStudent(
        studentId: Long,
        fullName: String,
        phone: String,
        startDate: String,
        licenseCategory: String,
        prepaidHours: Double,
        hourlyRate: Double,
        notes: String
    )

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudent(studentId: Long)

    @Query("SELECT * FROM lessons ORDER BY date DESC, startTime DESC")
    fun observeLessons(): Flow<List<LessonEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLesson(lesson: LessonEntity)

    @Query("DELETE FROM lessons WHERE id = :lessonId")
    suspend fun deleteLesson(lessonId: Long)

    @Query(
        """
        UPDATE lessons
        SET date = :date,
            startTime = :startTime,
            endTime = :endTime,
            durationHours = :durationHours,
            topics = :topics,
            rating = :rating,
            studentId = :studentId
        WHERE id = :lessonId
        """
    )
    suspend fun updateLesson(
        lessonId: Long,
        date: String,
        startTime: String,
        endTime: String,
        durationHours: Double,
        topics: String,
        rating: Int,
        studentId: Long
    )

    @Query("SELECT * FROM payments ORDER BY paymentDate DESC")
    fun observePayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payments")
    suspend fun totalIncome(): Double
}

package com.autoschool.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String,
    val startDate: String,
    val licenseCategory: String,
    val prepaidHours: Double,
    val hourlyRate: Double,
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(
    tableName = "lessons",
    foreignKeys = [ForeignKey(
        entity = StudentEntity::class,
        parentColumns = ["id"],
        childColumns = ["studentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("studentId")]
)
data class LessonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val date: String,
    val startTime: String,
    val endTime: String,
    val durationHours: Double,
    val lessonType: String,
    val topics: String,
    val rating: Int,
    val instructorComment: String,
    val isPaid: Boolean
)

@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(
        entity = StudentEntity::class,
        parentColumns = ["id"],
        childColumns = ["studentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("studentId")]
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val paymentDate: String,
    val amount: Double,
    val paymentMethod: String
)

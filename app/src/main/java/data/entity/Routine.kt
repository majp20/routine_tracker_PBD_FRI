package data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity

data class Routine(
    @PrimaryKey(autoGenerate = true) val id : Long = 0,
    val name: String,
    val type: String,
    val startTime: String,
    val endTime: String,
    val daysActive : String,
    val notificationEnabled : Boolean,
)
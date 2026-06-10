package data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE //deletes upon parent delete
        )
    ],
    indices = [Index("routineId")] //creates an index on the routineId column
)
data class RoutineExecution(
    @PrimaryKey(autoGenerate = true)
    val id : Long = 0,
    val routineId : Long,
    val completed : Boolean,
    val date: String //yyyy-MM-dd
)


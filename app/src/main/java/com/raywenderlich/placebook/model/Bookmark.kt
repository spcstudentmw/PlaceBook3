import androidx.room.Entity
import androidx.room.PrimaryKey

// Database entity class
@Entity
// Create data class with predefined args for primary constructor
data class Bookmark(
    // Define id property - auto generates incrementing numbers- for
    // unique identifier for bookmark record
    @PrimaryKey(autoGenerate = true) var id: Long? = null,
    // 4
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = ""
    )

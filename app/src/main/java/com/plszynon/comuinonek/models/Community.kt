package com.plszynon.comuinonek.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Community(
    @get:Exclude var id: String = "",
    var name: String = "",
    var description: String = "",
    var isPrivate: Boolean = false,
    var joinCode: String = "",
    var ownerId: String = "",
    var ownerName: String = "",
    var memberCount: Long = 0,
    @ServerTimestamp var createdAt: Date? = null
) {
    // No-arg constructor required by Firestore is provided by defaults above.
}

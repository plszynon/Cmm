package com.plszynon.comuinonek.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @get:Exclude var id: String = "",
    var authorId: String = "",
    var authorName: String = "",
    var text: String = "",
    @ServerTimestamp var createdAt: Date? = null
)

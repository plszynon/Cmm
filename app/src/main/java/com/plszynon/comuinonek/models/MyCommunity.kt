package com.plszynon.comuinonek.models

import com.google.firebase.firestore.Exclude

data class MyCommunity(
    @get:Exclude var communityId: String = "",
    var name: String = "",
    var isPrivate: Boolean = false,
    var role: String = "member" // "owner" or "member"
)

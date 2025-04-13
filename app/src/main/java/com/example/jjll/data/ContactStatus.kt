package com.example.jjll.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContactStatus {
    @SerialName("pending") PENDING,  //待辦的
    @SerialName("accepted") ACCEPTED,
    @SerialName("rejected") REJECTED,
    // @SerialName("blocked") BLOCKED // 如果需要
}

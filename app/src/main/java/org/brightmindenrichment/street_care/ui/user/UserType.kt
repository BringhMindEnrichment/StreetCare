package org.brightmindenrichment.street_care.ui.user

import android.view.View
import android.widget.ImageView
import org.brightmindenrichment.street_care.R

enum class UserType {
    INTERNAL_MEMBER,
    CHAPTER_LEADER,
    CHAPTER_MEMBER,
    REGISTERED_USER
}

fun getUserType(type: String): UserType {
    return when (type) {
        "Street Care Hub Leader" -> UserType.INTERNAL_MEMBER
        "Chapter Leader" -> UserType.CHAPTER_LEADER
        "Chapter Member" -> UserType.CHAPTER_MEMBER
        else -> UserType.REGISTERED_USER
    }
}

fun verificationMark(type: UserType, bsImageViewVerification: ImageView){
    when (type) {
        UserType.INTERNAL_MEMBER -> {
            bsImageViewVerification.setImageResource(R.drawable.ic_verified_blue)
        }

        UserType.CHAPTER_LEADER -> {
            bsImageViewVerification.setImageResource(R.drawable.ic_verified_green)
        }

        UserType.CHAPTER_MEMBER -> {
            bsImageViewVerification.setImageResource(R.drawable.ic_verified_purple)
        }

        UserType.REGISTERED_USER -> {
            bsImageViewVerification.setImageResource(R.drawable.ic_verified_yellow)
        }
    }
    bsImageViewVerification.visibility = View.VISIBLE
}
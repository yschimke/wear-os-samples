package com.example.android.wearable.wear.common.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.app.Person
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto
import com.example.android.wearable.wear.wearnotifications.proto.contact


public fun NotificationsProto.Contact.toPerson(): Person = Person.Builder()
    .setName(name)
    .apply {
        if (this@toPerson.hasUri()) {
            setUri(uri)
        }
        if (this@toPerson.hasKey()) {
            setKey(key)
        }
    }
    .build()

val Me = contact {
    name = "Me MacDonald"
    key = "1234567890"
    uri = "tel:1234567890"
    icon = R.drawable.me_macdonald
}

fun resourceToUri(context: Context, resId: Int): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(context.packageName)
        .path(resId.toString())
        .build()
}

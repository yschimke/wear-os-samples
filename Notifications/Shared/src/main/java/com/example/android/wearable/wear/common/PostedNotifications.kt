package com.example.android.wearable.wear.common

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto
import com.example.android.wearable.wear.wearnotifications.proto.NotificationsProto.*
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object PostedNotificationsSerializer : Serializer<PostedNotifications> {
    override val defaultValue: PostedNotifications = PostedNotifications.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PostedNotifications {
        try {
            return PostedNotifications.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: PostedNotifications, output: OutputStream) = t.writeTo(output)
}

val Context.postedNotificationsStore: DataStore<PostedNotifications> by dataStore(
    fileName = "postedNotifications.pb",
    serializer = PostedNotificationsSerializer
)

package com.example.android.wearable.wear.wearnotifications.main

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.wearable.wear.common.domain.Me
import com.example.android.wearable.wear.common.domain.resourceToUri
import com.example.android.wearable.wear.wearnotifications.MobileNotificationApplication
import com.example.android.wearable.wear.wearnotifications.common.R
import com.example.android.wearable.wear.wearnotifications.proto.contact
import com.example.android.wearable.wear.wearnotifications.proto.email
import com.example.android.wearable.wear.wearnotifications.proto.image
import com.example.android.wearable.wear.wearnotifications.proto.inboxNotification
import com.example.android.wearable.wear.wearnotifications.proto.message
import com.example.android.wearable.wear.wearnotifications.proto.messagingNotification
import com.example.android.wearable.wear.wearnotifications.proto.pictureNotification
import com.example.android.wearable.wear.wearnotifications.proto.textNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val notificationCentre = (application as MobileNotificationApplication).notificationCentre
    private val mNotificationManagerCompat =
        (application as MobileNotificationApplication).notificationManager

    val uiState: StateFlow<MainUiState> = MutableStateFlow(
        MainUiState(
            mNotificationManagerCompat.areNotificationsEnabled()
        )
    )

    fun generateBigTextStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postTextNotification(textNotification {
                title = "Don't forget to..."
                body =
                    "... feed the dogs before you leave for work, and check the garage to make sure the door is closed."
                summary = "Feed Dogs and Garage"
            })
        }
    }

    fun generateBigPictureStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postPictureNotification(pictureNotification {
                title = "Bob's Post"
                body =
                    "[Picture] Like my shot of Earth?"
                summary = "Like my shot of Earth?"
                postReplies.addAll(listOf("Yes", "No", "Maybe?"))
                participant.add(
                    contact {
                        name = "Bob Smith"
                    }
                )
            })
        }
    }

    fun generateInboxStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postInboxNotification(inboxNotification {
                title = "5 new emails"
                bigContent = "5 new emails from Jane, Jay, Alex +2"
                summary = "New emails"

                email.add(email {
                    summary = "Launch Party is here..."
                    this.participant.add(contact {
                        name = "Jane Faab"
                    })
                })

                email.add(email {
                    summary = "There's a turtle on the server!"
                    this.participant.add(contact {
                        name = "Jay Walker"
                    })
                })

                email.add(email {
                    summary = "Check this out..."
                    this.participant.add(contact {
                        name = "Alex Chang"
                    })
                })

                email.add(email {
                    summary = "Check in code?"
                    this.participant.add(contact {
                        name = "Jane Johns"
                    })
                })

                email.add(email {
                    summary = "Movies later...."
                    this.participant.add(contact {
                        name = "John Smith"
                    })
                })
            })
        }
    }

    fun generateMessagingStyleNotification(activity: Activity) {
        viewModelScope.launch {
            notificationCentre.postMessagingNotification(messagingNotification {
                me = Me

                title = "3 Messages w/ Famous, Wendy"
                body = "HEY, I see my house! :)"

                postReplies.addAll(listOf("Me too!", "How's the weather?", "You have good eyesight."))

                message.add(message {
                    this.text = ""
                    this.image = image {
                        this.uri = resourceToUri(activity, R.drawable.earth).toString()
                    }
                    this.timestamp = 1528490641998L
                    this.sender = contact {
                        this.name = "Famous Frank"
                        this.key = "9876543210"
                        this.uri = "tel:9876543210"
                    }
                })
                message.add(message {
                    this.text = "Visiting the moon again? :P"
                    this.timestamp = 1528490643998L
                    this.sender = Me
                })
                message.add(message {
                    this.text = "HEY, I see my house!"
                    this.timestamp = 1528490645998L
                    this.sender = contact {
                        this.name = "Wendy Weather"
                        this.key = "2233221122"
                        this.uri = "tel:2233221122"
                    }
                })
            })
        }
    }
}

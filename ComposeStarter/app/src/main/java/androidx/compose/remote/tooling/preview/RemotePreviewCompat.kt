package androidx.compose.remote.tooling.preview

import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable

@Composable
fun RemotePreview(
    @Suppress("UNUSED_PARAMETER") profile: Profile? = null,
    content: @Composable () -> Unit,
) {
    content()
}

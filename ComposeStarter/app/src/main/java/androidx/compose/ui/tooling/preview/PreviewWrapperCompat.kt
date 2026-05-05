package androidx.compose.ui.tooling.preview

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PreviewWrapper(val wrapper: KClass<out PreviewWrapperProvider>)

interface PreviewWrapperProvider {
    @Composable fun Wrap(content: @Composable () -> Unit)
}

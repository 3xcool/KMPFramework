package com.tekmoon.designsystem.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import platform.UIKit.UIApplication
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityMedium
import platform.UIKit.UIContentSizeCategoryDidChangeNotification
import platform.UIKit.UIContentSizeCategoryExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraSmall
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategoryMedium
import platform.UIKit.UIContentSizeCategorySmall
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

actual val platformDensityScale: Float = 1.0f

// Compose `.sp` on iOS does NOT respect UIContentSizeCategory (Dynamic Type).
// Reading it here is the only way to honor the user's accessibility text-size setting.
// Recomposes when the system notification fires.
@Composable
actual fun platformOsFontScale(): Float {
    val category by produceState(
        initialValue = UIApplication.sharedApplication.preferredContentSizeCategory
    ) {
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIContentSizeCategoryDidChangeNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue
        ) { _ ->
            value = UIApplication.sharedApplication.preferredContentSizeCategory
        }
        awaitDispose { NSNotificationCenter.defaultCenter.removeObserver(observer) }
    }
    return contentSizeCategoryToScale(category)
}

private fun contentSizeCategoryToScale(category: String?): Float = when (category) {
    UIContentSizeCategoryExtraSmall                            -> 0.823f
    UIContentSizeCategorySmall                                 -> 0.882f
    UIContentSizeCategoryMedium                                -> 0.941f
    UIContentSizeCategoryLarge                                 -> 1.000f
    UIContentSizeCategoryExtraLarge                            -> 1.118f
    UIContentSizeCategoryExtraExtraLarge                       -> 1.235f
    UIContentSizeCategoryExtraExtraExtraLarge                  -> 1.353f
    UIContentSizeCategoryAccessibilityMedium                   -> 1.643f
    UIContentSizeCategoryAccessibilityLarge                    -> 1.953f
    UIContentSizeCategoryAccessibilityExtraLarge               -> 2.353f
    UIContentSizeCategoryAccessibilityExtraExtraLarge          -> 2.764f
    UIContentSizeCategoryAccessibilityExtraExtraExtraLarge     -> 3.118f
    else                                                       -> 1.000f
}

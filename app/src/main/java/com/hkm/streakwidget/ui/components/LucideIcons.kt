package com.hkm.streakwidget.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Small, dependency-free outline icons drawn on the Lucide 24px grid.
 *
 * The vectors deliberately use a neutral black stroke. Their actual tint is supplied by
 * [androidx.compose.material3.Icon] through LocalContentColor, just like the Lucide library.
 */
object LucideIcons {
    val Settings: ImageVector
        get() = settingsVector
    val ListChecks: ImageVector
        get() = listChecksVector
    val Check: ImageVector
        get() = checkVector
    val Plus: ImageVector
        get() = plusVector
    val Trash2: ImageVector
        get() = trash2Vector
    val Archive: ImageVector
        get() = archiveVector
    val RotateCcw: ImageVector
        get() = rotateCcwVector
    val Download: ImageVector
        get() = downloadVector
    val Upload: ImageVector
        get() = uploadVector
    val Palette: ImageVector
        get() = paletteVector
    val Bell: ImageVector
        get() = bellVector
    val Smartphone: ImageVector
        get() = smartphoneVector
    val Moon: ImageVector
        get() = moonVector
    val Sun: ImageVector
        get() = sunVector
    val Monitor: ImageVector
        get() = monitorVector
    val X: ImageVector
        get() = xVector
    val ChevronRight: ImageVector
        get() = chevronRightVector
    val Ellipsis: ImageVector
        get() = ellipsisVector

    private val settingsVector: ImageVector by lazy {
        buildIcon("Settings") {
            lucidePath {
                moveTo(12f, 15f)
                curveTo(13.657f, 15f, 15f, 13.657f, 15f, 12f)
                curveTo(15f, 10.343f, 13.657f, 9f, 12f, 9f)
                curveTo(10.343f, 9f, 9f, 10.343f, 9f, 12f)
                curveTo(9f, 13.657f, 10.343f, 15f, 12f, 15f)
                close()
            }
            lucidePath {
                moveTo(19.4f, 15f)
                curveTo(19.19f, 15.48f, 19.31f, 16.04f, 19.7f, 16.42f)
                lineTo(19.76f, 16.48f)
                curveTo(20.54f, 17.26f, 20.54f, 18.52f, 19.76f, 19.3f)
                curveTo(18.98f, 20.08f, 17.72f, 20.08f, 16.94f, 19.3f)
                lineTo(16.88f, 19.24f)
                curveTo(16.5f, 18.85f, 15.94f, 18.73f, 15.46f, 18.94f)
                curveTo(14.86f, 19.19f, 14.47f, 19.78f, 14.47f, 20.43f)
                verticalLineTo(20.6f)
                curveTo(14.47f, 21.7f, 13.57f, 22.6f, 12.47f, 22.6f)
                curveTo(11.37f, 22.6f, 10.47f, 21.7f, 10.47f, 20.6f)
                verticalLineTo(20.51f)
                curveTo(10.47f, 19.83f, 10.07f, 19.22f, 9.45f, 18.98f)
                curveTo(8.97f, 18.77f, 8.41f, 18.89f, 8.03f, 19.28f)
                lineTo(7.97f, 19.34f)
                curveTo(7.19f, 20.12f, 5.93f, 20.12f, 5.15f, 19.34f)
                curveTo(4.37f, 18.56f, 4.37f, 17.3f, 5.15f, 16.52f)
                lineTo(5.21f, 16.46f)
                curveTo(5.6f, 16.08f, 5.72f, 15.52f, 5.51f, 15.04f)
                curveTo(5.26f, 14.44f, 4.67f, 14.05f, 4.02f, 14.05f)
                horizontalLineTo(3.85f)
                curveTo(2.75f, 14.05f, 1.85f, 13.15f, 1.85f, 12.05f)
                curveTo(1.85f, 10.95f, 2.75f, 10.05f, 3.85f, 10.05f)
                horizontalLineTo(3.94f)
                curveTo(4.62f, 10.05f, 5.23f, 9.65f, 5.47f, 9.03f)
                curveTo(5.68f, 8.55f, 5.56f, 7.99f, 5.17f, 7.61f)
                lineTo(5.11f, 7.55f)
                curveTo(4.33f, 6.77f, 4.33f, 5.51f, 5.11f, 4.73f)
                curveTo(5.89f, 3.95f, 7.15f, 3.95f, 7.93f, 4.73f)
                lineTo(7.99f, 4.79f)
                curveTo(8.37f, 5.18f, 8.93f, 5.3f, 9.41f, 5.09f)
                curveTo(10.01f, 4.84f, 10.4f, 4.25f, 10.4f, 3.6f)
                verticalLineTo(3.43f)
                curveTo(10.4f, 2.33f, 11.3f, 1.43f, 12.4f, 1.43f)
                curveTo(13.5f, 1.43f, 14.4f, 2.33f, 14.4f, 3.43f)
                verticalLineTo(3.52f)
                curveTo(14.4f, 4.2f, 14.8f, 4.81f, 15.42f, 5.05f)
                curveTo(15.9f, 5.26f, 16.46f, 5.14f, 16.84f, 4.75f)
                lineTo(16.9f, 4.69f)
                curveTo(17.68f, 3.91f, 18.94f, 3.91f, 19.72f, 4.69f)
                curveTo(20.5f, 5.47f, 20.5f, 6.73f, 19.72f, 7.51f)
                lineTo(19.66f, 7.57f)
                curveTo(19.27f, 7.95f, 19.15f, 8.51f, 19.36f, 8.99f)
                curveTo(19.61f, 9.59f, 20.2f, 9.98f, 20.85f, 9.98f)
                horizontalLineTo(21.02f)
                curveTo(22.12f, 9.98f, 23.02f, 10.88f, 23.02f, 11.98f)
                curveTo(23.02f, 13.08f, 22.12f, 13.98f, 21.02f, 13.98f)
                horizontalLineTo(20.93f)
                curveTo(20.25f, 13.98f, 19.64f, 14.38f, 19.4f, 15f)
                close()
            }
        }
    }

    private val listChecksVector: ImageVector by lazy {
        buildIcon("ListChecks") {
            lucidePath { moveTo(3f, 7f); lineTo(5f, 9f); lineTo(9f, 5f) }
            lucidePath { moveTo(3f, 17f); lineTo(5f, 19f); lineTo(9f, 15f) }
            lucidePath { moveTo(13f, 6f); horizontalLineTo(21f) }
            lucidePath { moveTo(13f, 12f); horizontalLineTo(21f) }
            lucidePath { moveTo(13f, 18f); horizontalLineTo(21f) }
        }
    }

    private val checkVector: ImageVector by lazy {
        buildIcon("Check") {
            lucidePath { moveTo(20f, 6f); lineTo(9f, 17f); lineTo(4f, 12f) }
        }
    }

    private val plusVector: ImageVector by lazy {
        buildIcon("Plus") {
            lucidePath { moveTo(12f, 5f); verticalLineTo(19f) }
            lucidePath { moveTo(5f, 12f); horizontalLineTo(19f) }
        }
    }

    private val trash2Vector: ImageVector by lazy {
        buildIcon("Trash2") {
            lucidePath { moveTo(3f, 6f); horizontalLineTo(21f) }
            lucidePath { moveTo(8f, 6f); verticalLineTo(4f); horizontalLineTo(16f); verticalLineTo(6f) }
            lucidePath { moveTo(19f, 6f); lineTo(18f, 20f); horizontalLineTo(6f); lineTo(5f, 6f) }
            lucidePath { moveTo(10f, 11f); verticalLineTo(17f) }
            lucidePath { moveTo(14f, 11f); verticalLineTo(17f) }
        }
    }

    private val archiveVector: ImageVector by lazy {
        buildIcon("Archive") {
            lucidePath {
                moveTo(21f, 8f)
                verticalLineTo(21f)
                horizontalLineTo(3f)
                verticalLineTo(8f)
            }
            lucidePath {
                moveTo(1f, 3f)
                horizontalLineTo(23f)
                verticalLineTo(8f)
                horizontalLineTo(1f)
                close()
            }
            lucidePath { moveTo(10f, 12f); horizontalLineTo(14f) }
        }
    }

    private val rotateCcwVector: ImageVector by lazy {
        buildIcon("RotateCcw") {
            lucidePath {
                moveTo(3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(8.49f, 21f, 5.45f, 19f, 3.96f, 16.08f)
            }
            lucidePath { moveTo(3f, 4f); verticalLineTo(10f); horizontalLineTo(9f) }
        }
    }

    private val downloadVector: ImageVector by lazy {
        buildIcon("Download") {
            lucidePath {
                moveTo(21f, 15f)
                verticalLineTo(19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                verticalLineTo(15f)
            }
            lucidePath { moveTo(7f, 10f); lineTo(12f, 15f); lineTo(17f, 10f) }
            lucidePath { moveTo(12f, 15f); verticalLineTo(3f) }
        }
    }

    private val uploadVector: ImageVector by lazy {
        buildIcon("Upload") {
            lucidePath {
                moveTo(21f, 15f)
                verticalLineTo(19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                verticalLineTo(15f)
            }
            lucidePath { moveTo(17f, 8f); lineTo(12f, 3f); lineTo(7f, 8f) }
            lucidePath { moveTo(12f, 3f); verticalLineTo(15f) }
        }
    }

    private val paletteVector: ImageVector by lazy {
        buildIcon("Palette") {
            lucidePath {
                moveTo(12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.03f, 22f, 11f)
                curveTo(22f, 13.21f, 20.21f, 15f, 18f, 15f)
                horizontalLineTo(16.5f)
                curveTo(15.67f, 15f, 15f, 15.67f, 15f, 16.5f)
                curveTo(15f, 17.33f, 15.67f, 18f, 16.5f, 18f)
                curveTo(16.78f, 18f, 17f, 18.22f, 17f, 18.5f)
                curveTo(17f, 20.43f, 14.76f, 22f, 12f, 22f)
                close()
            }
            lucidePath { moveTo(7.5f, 10f); curveTo(8.05f, 10f, 8.5f, 9.55f, 8.5f, 9f); curveTo(8.5f, 8.45f, 8.05f, 8f, 7.5f, 8f); curveTo(6.95f, 8f, 6.5f, 8.45f, 6.5f, 9f); curveTo(6.5f, 9.55f, 6.95f, 10f, 7.5f, 10f); close() }
            lucidePath { moveTo(11f, 7f); curveTo(11.55f, 7f, 12f, 6.55f, 12f, 6f); curveTo(12f, 5.45f, 11.55f, 5f, 11f, 5f); curveTo(10.45f, 5f, 10f, 5.45f, 10f, 6f); curveTo(10f, 6.55f, 10.45f, 7f, 11f, 7f); close() }
            lucidePath { moveTo(16.5f, 10f); curveTo(17.05f, 10f, 17.5f, 9.55f, 17.5f, 9f); curveTo(17.5f, 8.45f, 17.05f, 8f, 16.5f, 8f); curveTo(15.95f, 8f, 15.5f, 8.45f, 15.5f, 9f); curveTo(15.5f, 9.55f, 15.95f, 10f, 16.5f, 10f); close() }
            lucidePath { moveTo(7.5f, 15f); curveTo(8.05f, 15f, 8.5f, 14.55f, 8.5f, 14f); curveTo(8.5f, 13.45f, 8.05f, 13f, 7.5f, 13f); curveTo(6.95f, 13f, 6.5f, 13.45f, 6.5f, 14f); curveTo(6.5f, 14.55f, 6.95f, 15f, 7.5f, 15f); close() }
        }
    }

    private val bellVector: ImageVector by lazy {
        buildIcon("Bell") {
            lucidePath {
                moveTo(18f, 8f)
                curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
                curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
                curveTo(6f, 15f, 3f, 15f, 3f, 17f)
                horizontalLineTo(21f)
                curveTo(21f, 15f, 18f, 15f, 18f, 8f)
                close()
            }
            lucidePath { moveTo(10f, 21f); horizontalLineTo(14f) }
        }
    }

    private val smartphoneVector: ImageVector by lazy {
        buildIcon("Smartphone") {
            lucidePath {
                moveTo(5f, 2f)
                horizontalLineTo(19f)
                curveTo(20.1f, 2f, 21f, 2.9f, 21f, 4f)
                verticalLineTo(20f)
                curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                verticalLineTo(4f)
                curveTo(3f, 2.9f, 3.9f, 2f, 5f, 2f)
                close()
            }
            lucidePath { moveTo(12f, 18f); horizontalLineTo(12.01f) }
        }
    }

    private val moonVector: ImageVector by lazy {
        buildIcon("Moon") {
            lucidePath {
                moveTo(21f, 12.79f)
                curveTo(19.72f, 13.57f, 18.21f, 14f, 16.6f, 14f)
                curveTo(11.85f, 14f, 8f, 10.15f, 8f, 5.4f)
                curveTo(8f, 3.79f, 8.43f, 2.28f, 9.21f, 1f)
                curveTo(4.42f, 2.55f, 1.82f, 7.68f, 3.37f, 12.47f)
                curveTo(4.92f, 17.26f, 10.05f, 19.86f, 14.84f, 18.31f)
                curveTo(17.67f, 17.39f, 20.08f, 15.36f, 21f, 12.79f)
                close()
            }
        }
    }

    private val sunVector: ImageVector by lazy {
        buildIcon("Sun") {
            lucidePath {
                moveTo(12f, 16f)
                curveTo(14.21f, 16f, 16f, 14.21f, 16f, 12f)
                curveTo(16f, 9.79f, 14.21f, 8f, 12f, 8f)
                curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
                curveTo(8f, 14.21f, 9.79f, 16f, 12f, 16f)
                close()
            }
            lucidePath { moveTo(12f, 2f); verticalLineTo(4f) }
            lucidePath { moveTo(12f, 20f); verticalLineTo(22f) }
            lucidePath { moveTo(4.93f, 4.93f); lineTo(6.34f, 6.34f) }
            lucidePath { moveTo(17.66f, 17.66f); lineTo(19.07f, 19.07f) }
            lucidePath { moveTo(2f, 12f); horizontalLineTo(4f) }
            lucidePath { moveTo(20f, 12f); horizontalLineTo(22f) }
            lucidePath { moveTo(4.93f, 19.07f); lineTo(6.34f, 17.66f) }
            lucidePath { moveTo(17.66f, 6.34f); lineTo(19.07f, 4.93f) }
        }
    }

    private val monitorVector: ImageVector by lazy {
        buildIcon("Monitor") {
            lucidePath {
                moveTo(4f, 3f)
                horizontalLineTo(20f)
                curveTo(21.1f, 3f, 22f, 3.9f, 22f, 5f)
                verticalLineTo(15f)
                curveTo(22f, 16.1f, 21.1f, 17f, 20f, 17f)
                horizontalLineTo(4f)
                curveTo(2.9f, 17f, 2f, 16.1f, 2f, 15f)
                verticalLineTo(5f)
                curveTo(2f, 3.9f, 2.9f, 3f, 4f, 3f)
                close()
            }
            lucidePath { moveTo(8f, 21f); horizontalLineTo(16f) }
            lucidePath { moveTo(12f, 17f); verticalLineTo(21f) }
        }
    }

    private val xVector: ImageVector by lazy {
        buildIcon("X") {
            lucidePath { moveTo(18f, 6f); lineTo(6f, 18f) }
            lucidePath { moveTo(6f, 6f); lineTo(18f, 18f) }
        }
    }

    private val chevronRightVector: ImageVector by lazy {
        buildIcon("ChevronRight") {
            lucidePath { moveTo(9f, 18f); lineTo(15f, 12f); lineTo(9f, 6f) }
        }
    }

    private val ellipsisVector: ImageVector by lazy {
        buildIcon("Ellipsis") {
            lucidePath { moveTo(5f, 12f); horizontalLineTo(5.01f) }
            lucidePath { moveTo(12f, 12f); horizontalLineTo(12.01f) }
            lucidePath { moveTo(19f, 12f); horizontalLineTo(19.01f) }
        }
    }

    private fun buildIcon(name: String, content: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(content).build()

    private fun ImageVector.Builder.lucidePath(content: PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            content()
        }
    }
}

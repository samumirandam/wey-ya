# Default ProGuard rules for ¡Wey Ya!
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# CallScreeningService + TileService
-keep class com.weyya.app.service.WeyYaScreeningService { *; }
-keep class com.weyya.app.service.WeyYaTileService { *; }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Glance widgets
-keep class com.weyya.app.widget.WeyYaWidgetReceiver { *; }
-keep class com.weyya.app.widget.WeyYaWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

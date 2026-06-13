package com.asepfiles.app

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {
    private const val NAME = "asepfiles_prefs"
    const val KEY_FONT = "font"
    const val KEY_FONT_SIZE = "font_size"
    const val KEY_DARK_MODE = "dark_mode"
    const val KEY_WALLPAPER_PATH = "wallpaper_path"
    const val KEY_SHOW_HIDDEN = "show_hidden"
    const val KEY_SORT_BY = "sort_by"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getFont(ctx: Context) = prefs(ctx).getString(KEY_FONT, "DEFAULT") ?: "DEFAULT"
    fun getFontSize(ctx: Context) = prefs(ctx).getFloat(KEY_FONT_SIZE, 15f)
    fun isDarkMode(ctx: Context) = prefs(ctx).getBoolean(KEY_DARK_MODE, true)
    fun getWallpaperPath(ctx: Context) = prefs(ctx).getString(KEY_WALLPAPER_PATH, null)
    fun showHidden(ctx: Context) = prefs(ctx).getBoolean(KEY_SHOW_HIDDEN, false)
    fun getSortBy(ctx: Context) = prefs(ctx).getString(KEY_SORT_BY, "name") ?: "name"

    fun set(ctx: Context, key: String, value: Any) {
        val editor = prefs(ctx).edit()
        when (value) {
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Float -> editor.putFloat(key, value)
            is Int -> editor.putInt(key, value)
        }
        editor.apply()
    }
}
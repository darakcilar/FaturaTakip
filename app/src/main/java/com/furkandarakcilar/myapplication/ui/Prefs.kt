package com.furkandarakcilar.myapplication.util

import android.content.Context

private const val KEY_LOGGED_IN      = "key_logged_in"
private const val KEY_USER           = "key_user"
private const val KEY_REMEMBERED_SET = "key_remembered_set"
private const val KEY_PASS_PREFIX    = "key_pass_for_"

object Prefs {
    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences("invoice_prefs", Context.MODE_PRIVATE)

    fun isLoggedIn(ctx: Context) =
        prefs(ctx).getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(ctx: Context, user: String) =
        prefs(ctx).edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER, user)
            .apply()

    fun setLoggedOut(ctx: Context) =
        prefs(ctx).edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .remove(KEY_USER)
            .apply()

    fun getCurrentUser(ctx: Context): String? =
        prefs(ctx).getString(KEY_USER, null)

    // Parolayı sakla/getir
    fun saveUserPassword(ctx: Context, user: String, pass: String) =
        prefs(ctx).edit()
            .putString(KEY_PASS_PREFIX + user, pass)
            .apply()

    fun getUserPassword(ctx: Context, user: String): String? =
        prefs(ctx).getString(KEY_PASS_PREFIX + user, null)

    // Remembered users set
    fun addRememberedUser(ctx: Context, user: String) {
        val set = prefs(ctx).getStringSet(KEY_REMEMBERED_SET, emptySet())!!.toMutableSet()
        set.add(user)
        prefs(ctx).edit().putStringSet(KEY_REMEMBERED_SET, set).apply()
    }
    fun removeRememberedUser(ctx: Context, user: String) {
        val set = prefs(ctx).getStringSet(KEY_REMEMBERED_SET, emptySet())!!.toMutableSet()
        set.remove(user)
        prefs(ctx).edit().putStringSet(KEY_REMEMBERED_SET, set).apply()
    }
    fun getRememberedUsers(ctx: Context): Set<String> =
        prefs(ctx).getStringSet(KEY_REMEMBERED_SET, emptySet()) ?: emptySet()
}

package com.dogar.geodesic.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by lester on 25.04.15.
 */
public class SharedPreferencesUtils {
    private static final String USER_GOOGLE_ACCOUNT = "user_google_account";

    public static boolean isLoggedIn(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountName = preferences.getString(USER_GOOGLE_ACCOUNT, null);
        boolean loggedIn = accountName != null;
        return loggedIn;
    }

    public static void saveLogin(Context context, String accountName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(USER_GOOGLE_ACCOUNT, accountName).apply();
    }

    public static String getLoginEmail(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(USER_GOOGLE_ACCOUNT, "");
    }

    public static void deleteLogin(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().remove(USER_GOOGLE_ACCOUNT).apply();
    }
}

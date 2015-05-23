package com.dogar.geodesic.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.api.client.googleapis.extensions.android.accounts.GoogleAccountManager;

/**
 * Created by lester on 17.05.15.
 */
public class AccountUtils {
    private static GoogleAccountManager googleAccountManager;

    /**
     * not thread-safe singleton getter of google acc manager
     * @return
     */
    public static GoogleAccountManager getGoogleAccountManager(Context context) {
        return googleAccountManager == null ? new GoogleAccountManager(context) : googleAccountManager;
    }

    /**
     * Get account
     *
     * @param context
     * @param email
     * @return account if found, null otherwise
     */
    public static Account getAccount(Context context,String email) {
      return getGoogleAccountManager(context).getAccountByName(email);
    }
}

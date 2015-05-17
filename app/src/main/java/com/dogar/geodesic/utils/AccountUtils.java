package com.dogar.geodesic.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * Created by lester on 17.05.15.
 */
public class AccountUtils {
    /**
     * Get account
     *
     * @param context
     * @param email
     * @return account if found, null otherwise
     */
    public static Account getAccount(Context context, String email) {
        Account[] accounts = AccountManager.get(context).getAccounts();
        for (Account account : accounts) {
            if (account.name.equals(email)) {
                return account;
            }
        }
        return null;
    }
}

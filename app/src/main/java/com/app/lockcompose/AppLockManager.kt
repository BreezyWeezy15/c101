package com.app.lockcompose

import android.content.Context
import android.content.SharedPreferences

class AppLockManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)

    fun getSelectedPackages(): Set<String> {
        return sharedPreferences.getStringSet("selected_package_names", emptySet()) ?: emptySet()
    }

    fun addPackage(packageName: String) {
        val packageNames = getSelectedPackages().toMutableSet()
        packageNames.add(packageName)
        with(sharedPreferences.edit()) {
            putStringSet("selected_package_names", packageNames)
            apply()
        }
    }

    fun removePackage(packageName: String) {
        val packageNames = getSelectedPackages().toMutableSet()
        packageNames.remove(packageName)
        with(sharedPreferences.edit()) {
            putStringSet("selected_package_names", packageNames)
            apply()
        }
    }
}
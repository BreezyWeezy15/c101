package com.app.lockcompose


import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat


class LockScreenActivity : AppCompatActivity() {

    private lateinit var appLockManager: AppLockManager
    private lateinit var lockUi : LinearLayout
    private lateinit var askPermissionBtn : Button
    companion object {
        private const val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_screen)

        lockUi = findViewById(R.id.lockUi)
        askPermissionBtn = findViewById(R.id.askPermission)
        askPermissionBtn.setOnClickListener {
            // show pass code ui
            Log.d("TAGZZ","LOCKED")
            if (lockUi.visibility == View.GONE){
                lockUi.visibility = View.VISIBLE
                showPassCodeUi()
            }
        }


        appLockManager = AppLockManager(this)

    }

    private fun showPassCodeUi(){


        val btn0 = findViewById<TextView>(R.id.btn0)
        val btn1 = findViewById<TextView>(R.id.btn1)
        val btn2 = findViewById<TextView>(R.id.btn2)
        val btn3 = findViewById<TextView>(R.id.btn3)
        val btn4 = findViewById<TextView>(R.id.btn4)
        val btn5 = findViewById<TextView>(R.id.btn5)
        val btn6 = findViewById<TextView>(R.id.btn6)
        val btn7 = findViewById<TextView>(R.id.btn7)
        val btn8 = findViewById<TextView>(R.id.btn8)
        val btn9 = findViewById<TextView>(R.id.btn9)
        val tick = findViewById<ImageView>(R.id.tick)
        val edit = findViewById<EditText>(R.id.passCodeEdit)

        val passcodeBuilder = StringBuilder()
        val numberButtons = listOf(btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9)

        tick.setOnClickListener {
            val passcode = passcodeBuilder.toString()
            if (passcode == "1234") {
                edit.text.clear()
                removePackage()
                finishAffinity()
            } else {
                Toast.makeText(this,"Pass code is wrong",Toast.LENGTH_LONG).show()
            }
        }

        numberButtons.forEach { button ->
            button.setOnClickListener {
                passcodeBuilder.append(button.text)
                edit.setText(passcodeBuilder.toString())
            }
        }

        addRemoveIcon(edit)
        edit.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = edit.compoundDrawablesRelative[2]
                if (drawableEnd != null && event.rawX >= edit.right - drawableEnd.bounds.width()) {
                    if (passcodeBuilder.isNotEmpty()) {
                        passcodeBuilder.deleteCharAt(passcodeBuilder.length - 1)
                        edit.setText(passcodeBuilder.toString())
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }


    }

    private fun addRemoveIcon(edit : EditText){
        val greenColor = ContextCompat.getColor(this, R.color.greenColor)
        val colorFilter = PorterDuffColorFilter(greenColor, PorterDuff.Mode.SRC_IN)
        edit.compoundDrawablesRelative[2]?.colorFilter = colorFilter
    }


//    private fun removePackage(){
//        val packageName = intent.getStringExtra("PACKAGE_NAME")
//        if (packageName != null) {
//            val lockedPackages = appLockManager.getSelectedPackages()
//            if (lockedPackages.contains(packageName)) {
//                appLockManager.removePackage(packageName)
//            }
//        }
//    }

    private fun removePackage() {
        val packageName = intent.getStringExtra("PACKAGE_NAME")
        if (packageName != null) {
            val lockedPackages = appLockManager.getSelectedPackages()
            if (lockedPackages.contains(packageName)) {
                appLockManager.removePackage(packageName)
                updateAccessList(packageName)
                sendBroadcast(Intent(packageName).apply {
                    putExtra("PACKAGE_NAME", packageName)
                })
            }
        }
    }


    private fun updateAccessList(packageName: String) {
        val sharedPreferences = getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE)
        val selectedPackageNames = sharedPreferences.getStringSet("selected_package_names", emptySet())?.toMutableSet() ?: mutableSetOf()

        if (!selectedPackageNames.contains(packageName)) {
            val accessList = sharedPreferences.getStringSet("access_list", emptySet())?.toMutableSet() ?: mutableSetOf()
            accessList.remove(packageName)

            with(sharedPreferences.edit()) {
                putStringSet("access_list", accessList)
                apply()
            }
        }
    }
}


package com.furkandarakcilar.myapplication.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.util.Prefs

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Gece modunu kapat
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        // Sistem ActionBar’dan Up okunu göster
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val etUser    = findViewById<EditText>(R.id.etNewUser)
        val etPass    = findViewById<EditText>(R.id.etNewPass)
        val etPassCon = findViewById<EditText>(R.id.etNewPassConfirm)
        val btnReg    = findViewById<Button>(R.id.btnRegister)

        btnReg.setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()
            val pass2= etPassCon.text.toString()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Kullanıcı ve şifre boş olamaz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != pass2) {
                Toast.makeText(this, "Şifreler uyuşmuyor!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (Prefs.getUserPassword(this, user) != null) {
                Toast.makeText(this, "Bu kullanıcı adı kayıtlı!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Prefs.saveUserPassword(this, user, pass)
            Toast.makeText(this, "Kayıt başarılı. Lütfen giriş yapın.", Toast.LENGTH_SHORT).show()
            goLogin()
        }
    }
    // ActionBar Up okuna tıklayınca
    override fun onSupportNavigateUp(): Boolean {
        goLogin()
        return true
    }
    // Cihazın geri tuşuna basıldığında da
    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        super.onBackPressed()
        goLogin()
    }
    private fun goLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(
            R.anim.slide_in_left,
            R.anim.slide_out_right
        )
        finish()
    }
}

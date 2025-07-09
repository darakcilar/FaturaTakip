package com.furkandarakcilar.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.util.Prefs

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_activity)

        val etUser    = findViewById<EditText>(R.id.etNewUser)
        val etPass    = findViewById<EditText>(R.id.etNewPass)
        val etPassCon = findViewById<EditText>(R.id.etNewPassConfirm)
        val btnReg    = findViewById<Button>(R.id.btnRegister)

        btnReg.setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString()
            val pass2= etPassCon.text.toString()

            // 1) Alan kontrolü
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Kullanıcı adı ve şifre boş olamaz!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != pass2) {
                Toast.makeText(this, "Şifreler uyuşmuyor!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2) Mevcut kullanıcı kontrolü
            val existing = Prefs.getUserPassword(this, user)
            if (existing != null) {
                Toast.makeText(this, "Bu kullanıcı adı zaten kayıtlı!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3) Kaydet
            Prefs.saveUserPassword(this, user, pass)
            Toast.makeText(this, "Kayıt başarılı. Lütfen giriş yapın.", Toast.LENGTH_SHORT).show()

            // 4) Login'e dön
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            finish()
        }
    }
}

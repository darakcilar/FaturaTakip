package com.furkandarakcilar.myapplication.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.furkandarakcilar.myapplication.R
import com.furkandarakcilar.myapplication.util.Prefs

class LoginActivity : AppCompatActivity() {

    private lateinit var etUser: EditText
    private lateinit var etPass: EditText
    private lateinit var cbRemember: CheckBox
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var tvAccountsLabel: TextView
    private lateinit var llAccounts: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(
            AppCompatDelegate.MODE_NIGHT_NO
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        etUser    = findViewById(R.id.etUsername)
        etPass    = findViewById(R.id.etPassword)
        cbRemember= findViewById(R.id.cbRemember)
        btnLogin  = findViewById(R.id.btnLogin)
        tvRegister= findViewById(R.id.tvRegister)
        tvAccountsLabel = findViewById(R.id.tvAccountsLabel)
        llAccounts = findViewById(R.id.llAccounts)

        // Önceki hatırlanan tekil kullanıcı için hala etUser doldurulur
        Prefs.getRememberedUsers(this).firstOrNull()?.let {
            etUser.setText(it)
            cbRemember.isChecked = true
        }

        btnLogin.setOnClickListener { attemptLogin() }
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Alt kısımda hesaplar listesi
        refreshAccountsList()
    }

    private fun attemptLogin() {
        val user = etUser.text.toString().trim()
        val pass = etPass.text.toString()
        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Eksik bilgi!", Toast.LENGTH_SHORT).show()
            return
        }
        val savedPass = Prefs.getUserPassword(this, user)
        if (savedPass == null || savedPass != pass) {
            Toast.makeText(this, "Kullanıcı/şifre hatalı!", Toast.LENGTH_SHORT).show()
            return
        }
        // Giriş başarılı
        Prefs.setLoggedIn(this, user)
        if (cbRemember.isChecked) {
            Prefs.addRememberedUser(this, user)
        }
        startMain()
    }

    private fun refreshAccountsList() {
        val users = Prefs.getRememberedUsers(this)
        llAccounts.removeAllViews()
        if (users.isEmpty()) {
            tvAccountsLabel.visibility = View.GONE
            return
        }
        tvAccountsLabel.visibility = View.VISIBLE

        val inflater = LayoutInflater.from(this)
        for (u in users) {
            val row = inflater.inflate(R.layout.item_account, llAccounts, false)
            val tv = row.findViewById<TextView>(R.id.tvAccName)
            val btn = row.findViewById<ImageButton>(R.id.btnRemoveAcc)
            tv.text = u

            // HESABA tıklayınca şifre sormadan direkt giriş
            tv.setOnClickListener {
                Prefs.setLoggedIn(this, u)
                startMain()
            }

            // Silme butonu
            btn.setOnClickListener {
                Prefs.removeRememberedUser(this, u)
                refreshAccountsList()
            }

            llAccounts.addView(row)
        }
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
}

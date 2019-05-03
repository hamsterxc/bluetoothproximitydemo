package com.lonebytesoft.hamster.bluetoothproximitydemo

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val callbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val isLoggedIn = !(AccessToken.getCurrentAccessToken()?.isExpired ?: true)
        if (isLoggedIn) {
            startActivity(Intent(this@MainActivity, PeopleActivity::class.java))
            return
        }

        login_button.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                Snackbar.make(login_button, "Logged in", Snackbar.LENGTH_SHORT)
                    .show()

                startActivity(Intent(this@MainActivity, PeopleActivity::class.java))
            }

            override fun onCancel() {
            }

            override fun onError(error: FacebookException?) {
                Snackbar.make(login_button, "Error: ${error?.localizedMessage ?: "unknown"}", Snackbar.LENGTH_LONG)
                    .show()
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

}

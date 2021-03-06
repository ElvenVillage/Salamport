package com.newpage.salamport

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.newpage.salamport.groups.NewsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await


data class UserProfile(var id: String, var name: String,
                       var surname: String,
                       var middlename: String?,
                       var birthdate: String?,
                       var phone: String?,
                       var country: String?,
                       var sex: String?,
                       var city: String?,
                       var native_city: String?,
                       var job: String?,
                       var photo: String?,
                       var study: String?,
                       var email: String?,
                       var interest: String?
)

class ClickListenerForProfile(val context: ProfileActivity, val thing:String) : View.OnClickListener{
    override fun onClick(v: View?) {
        val li = LayoutInflater.from(context)
        val promptsView: View = when {
            thing == "bdate" -> li.inflate(R.layout.prompts_calendar, null)
            thing == "sex" -> li.inflate(R.layout.prompts_radio, null)
            else -> li.inflate(R.layout.prompt, null)
        }

        val mDialogBuilder = AlertDialog.Builder(context)
        mDialogBuilder.setView(promptsView)

        if ((thing != "bdate") && (thing != "sex")) {
            val userInput = promptsView.findViewById<EditText>(R.id.input_text)

            mDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK") { dialog, which ->
                    when (thing) {
                        "name" -> context.userProfile.name = userInput.text.toString()
                        "surname" -> context.userProfile.surname = userInput.text.toString()
                        "middlename" -> context.userProfile.middlename = userInput.text.toString()
                        "sex" -> context.userProfile.sex = userInput.text.toString()
                        "city" -> context.userProfile.city = userInput.text.toString()
                        "job" -> context.userProfile.job = userInput.text.toString()
                        "mail" -> context.userProfile.email = userInput.text.toString()
                        "study" -> context.userProfile.study = userInput.text.toString()
                        "bio" -> context.userProfile.interest = userInput.text.toString()
                        "native_city" -> context.userProfile.native_city = userInput.text.toString()
                        "phone" -> context.userProfile.phone = userInput.text.toString()
                    }
                    context.applyChangesInProfile()
                    context.renderUserProfile(context.userProfile)
                }
        } else if (thing != "sex") {
            val userInput = promptsView.findViewById<DatePicker>(R.id.datePicker)
                .init(2020, 7, 12, object : DatePicker.OnDateChangedListener {
                    override fun onDateChanged(
                        view: DatePicker?,
                        year: Int,
                        monthOfYear: Int,
                        dayOfMonth: Int
                    ) {
                        context.userProfile.birthdate = "$year-$monthOfYear-$dayOfMonth"
                        context.applyChangesInProfile()
                        context.renderUserProfile(context.userProfile)
                    }

                })

        } else {
            val userInputM = promptsView.findViewById<RadioButton>(R.id.dialogM)
            userInputM.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    context.userProfile.sex = "1"
                    context.applyChangesInProfile()
                    context.renderUserProfile(context.userProfile)
                }
            }
            val userInputF = promptsView.findViewById<RadioButton>(R.id.dialogF)
            userInputF.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    context.userProfile.sex = "2"
                    context.applyChangesInProfile()
                    context.renderUserProfile(context.userProfile)
                }
            }
            val userInputN = promptsView.findViewById<RadioButton>(R.id.dialogN)
            userInputN.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    context.userProfile.sex = "0"
                    context.applyChangesInProfile()
                    context.renderUserProfile(context.userProfile)
                }
            }
        }
        mDialogBuilder.create().show()
    }

}

class ProfileActivity : AppCompatActivity() {

    private lateinit var grishaSession: String
    private lateinit var grishaToken: String

    private lateinit var client: OkHttpClient

    lateinit var userProfile: UserProfile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profile)
        val slide =
            TransitionInflater.from(this).inflateTransition(android.R.transition.slide_right)
        window.exitTransition = slide

        grishaSession = intent.getStringExtra("session")
        grishaToken = intent.getStringExtra("token")
        client = OkHttpClient.Builder().build()

        findViewById<ImageView>(R.id.goToMessages).setOnClickListener {
            ChatActivity.startFrom(
                this, token = grishaToken,
                session = grishaSession
            )
        }

        findViewById<ImageView>(R.id.goToFriends).setOnClickListener {
            FriendsActivity.startFrom(
                this, token = grishaToken, session = grishaSession
            )
        }

        findViewById<ImageView>(R.id.goToFeed).setOnClickListener {
            NewsActivity.startFrom(this, token = grishaToken, session = grishaSession)
        }

        createOnClickListeners()
        updateUserProfileFromServer()
    }

    private fun createOnClickListeners() {
        findViewById<TextView>(R.id.profileName).setOnClickListener(ClickListenerForProfile(this, "name"))
        findViewById<TextView>(R.id.profileSurname).setOnClickListener(ClickListenerForProfile(this, "surname"))
        findViewById<TextView>(R.id.profilePatronymic).setOnClickListener(ClickListenerForProfile(this, "middlename"))
        findViewById<TextView>(R.id.profilebdate).setOnClickListener(
            ClickListenerForProfile(
                this,
                "bdate"
            )
        )
        findViewById<TextView>(R.id.profilehomeTown).setOnClickListener(
            ClickListenerForProfile(
                this,
                "native_city"
            )
        )
        findViewById<TextView>(R.id.profileSex).setOnClickListener(ClickListenerForProfile(this, "sex"))
        findViewById<TextView>(R.id.profileCity).setOnClickListener(ClickListenerForProfile(this, "city"))
        findViewById<TextView>(R.id.profileWork).setOnClickListener(ClickListenerForProfile(this, "job"))
        findViewById<TextView>(R.id.profileEmail).setOnClickListener(
            ClickListenerForProfile(
                this,
                "mail"
            )
        )
        findViewById<TextView>(R.id.profilePhone).setOnClickListener(
            ClickListenerForProfile(
                this,
                "phone"
            )
        )
        findViewById<TextView>(R.id.profileStudy).setOnClickListener(ClickListenerForProfile(this, "study"))
        findViewById<TextView>(R.id.profileInterest).setOnClickListener(
            ClickListenerForProfile(
                this,
                "bio"
            )
        )
    }

    fun renderUserProfile(user: UserProfile) {
        findViewById<TextView>(R.id.profileName).text = user.name
        findViewById<TextView>(R.id.profileSurname).text = user.surname
        findViewById<TextView>(R.id.profilePatronymic).text = user.middlename
        findViewById<TextView>(R.id.profilebdate).text = user.birthdate
        findViewById<TextView>(R.id.profileCity).text = user.city
        findViewById<TextView>(R.id.profileSex).text = if (user.sex == "1") "M" else "F"
        findViewById<TextView>(R.id.profilehomeTown).text =
            if (user.native_city != "null") user.native_city else "не указано"
        findViewById<TextView>(R.id.profileCity).text = user.city
        findViewById<TextView>(R.id.profilePhone).text = user.phone
        findViewById<TextView>(R.id.profileEmail).text = user.email
        findViewById<TextView>(R.id.profileInterest).text = user.interest
    }

    private fun updateUserProfileFromServer() {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", grishaSession)
                .add("token", grishaToken)
                .build()
            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/lk.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response = withContext(Dispatchers.IO) {client.newCall(request).await().body?.string()}
            val imgView = findViewById<ImageView>(R.id.profileAvatar)
            try {
                val responseJSON = JSONObject(response)
                val avatar = responseJSON.getString("photo")
                this@ProfileActivity.runOnUiThread {
                    Glide.with(this@ProfileActivity)
                        .load(avatar)
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(250)))
                        .into(imgView)
                    val id = responseJSON.getString("id")
                    val name = responseJSON.getString("name")
                    val surname = responseJSON.getString("surname")
                    val middlename = responseJSON.getString("middle_name")
                    val birthdate = responseJSON.getString("birthdate")
                    val phone = responseJSON.getString("phone")
                    val country = responseJSON.getString("country")
                    val sex = responseJSON.getString("sex")
                    val city = responseJSON.getString("city")
                    val native_city = responseJSON.getString("native_city")
                    val study = responseJSON.getString("study")
                    val job = responseJSON.getString("job")
                    val photo = responseJSON.getString("photo")
                    val email = responseJSON.getString("email")
                    val interests = responseJSON.getString("bio")

                    userProfile = UserProfile(id, name, surname, middlename, birthdate, phone,
                        country, sex, city, native_city, job, photo, study, email, interests
                    )

                    renderUserProfile(userProfile)
                }
            } catch (j: JSONException) {
                MainActivity.startFrom(this@ProfileActivity)
            }
        }
    }

    fun applyChangesInProfile() {
        GlobalScope.launch {
            val requestBody = FormBody.Builder()
                .add("session", grishaSession)
                .add("token", grishaToken)
                .add("name", userProfile.name)
                .add("surname", userProfile.surname)
                .add("middle_name", userProfile.middlename.toString())
                .add("birthdate", userProfile.birthdate.toString())
                .add("sex", "1")
                .add("phone", userProfile.phone.toString())
                .add("country", userProfile.country.toString())
                .add("city", userProfile.city.toString())
                .add("native_city", userProfile.native_city.toString())
                .add("study", userProfile.study.toString())
                .add("job", userProfile.job.toString())
                .add("email", userProfile.email.toString())
                .add("bio", userProfile.interest.toString())
                .build()

            val request = Request.Builder()
                .url("https://salamport.newpage.xyz/api/lk_edit.php")
                .post(requestBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build()
            val response = withContext(Dispatchers.IO) {client.newCall(request).await()}
            val responseString = response.body?.string()
            Log.i("info", responseString)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        grishaToken = savedInstanceState.getString("grishaToken")!!
        grishaSession = savedInstanceState.getString("grishaSession")!!
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("grishaToken", grishaToken)
        outState.putString("grishaSession", grishaSession)
    }


    companion object {
        fun startFrom(context: Context, session: String, token: String) {
            val intent = Intent(context, ProfileActivity::class.java)
            intent.putExtra("session", session)
            intent.putExtra("token", token)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}


package org.brightmindenrichment.street_care.ui.user


import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.MainActivity
import org.brightmindenrichment.street_care.R
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ChapterMembershipFormThreeAcitivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chapter_membership_form_3)

        val guardianSignatureTextView: TextView = findViewById(R.id.guardianSignatureTextView)

        guardianSignatureTextView.text = HtmlCompat.fromHtml(
            getString(R.string.guardian_signature),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        guardianSignatureTextView.movementMethod = LinkMovementMethod.getInstance()

        val editTextSignatureDate: TextInputEditText = findViewById(R.id.editTextSignatureDate)

// Initialize a calendar instance
        val calendar = Calendar.getInstance()

// Set up a DatePickerDialog to show when the TextInputEditText is clicked
        editTextSignatureDate.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Update the calendar with the selected date
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    // Format the date and set it to the TextInputEditText
                    val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
                    editTextSignatureDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Show the DatePickerDialog
            datePickerDialog.show()
        }

        // Initialize views
        val editTextSignatureName = findViewById<TextInputEditText>(R.id.edit_text_signature_name)
        val editTextFullName = findViewById<TextInputEditText>(R.id.editTextFullName)
        val editTextComments = findViewById<TextInputEditText>(R.id.editTextComments)
        val btnSaveChanges = findViewById<Button>(R.id.btn_save_changes)
        val btnNext = findViewById<Button>(R.id.btn_cancel)

        val db = Firebase.firestore

        val previousFormData = intent.extras

        btnNext.setOnClickListener {
            finish() // Close the current activity and go back to the previous step
        }

        // Handle Save button click
        btnSaveChanges.setOnClickListener {

            val user = Firebase.auth.currentUser
            if (user == null) {
                Toast.makeText(this, getString(R.string.please_login_before_event), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            } else {

                val name = previousFormData?.getString("name").toString().trim()
                val email = previousFormData?.getString("email").toString().trim()
                val phone = previousFormData?.getString("phone").toString().trim()
                val address = previousFormData?.getString("address").toString().trim()
                val daysAvailable = previousFormData?.getString("daysAvailable").toString().trim()
                val hoursAvailable = previousFormData?.getString("hoursAvailable").toString().trim()
                val consent = previousFormData?.getString("consent").toString().trim()
                val source = previousFormData?.getString("source").toString().trim()
                val whyVolunteer = previousFormData?.getString("whyVolunteer").toString().trim()

                val signatureName = editTextSignatureName.text.toString().trim()
                val fullName = editTextFullName.text.toString().trim()
                val signatureDate = editTextSignatureDate.text.toString().trim()
                val comments = editTextComments.text.toString().trim()

                val currentDateInMillis = System.currentTimeMillis()
                val currentDateTimestamp = Timestamp(Date(currentDateInMillis))

                // Validate input
                if (signatureName.isEmpty() || fullName.isEmpty() || signatureDate.isEmpty()) {
                    Toast.makeText(this, "Please fill in all mandatory fields.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                addEvent(
                    name = name,
                    email = email,
                    phone = phone,
                    address = address,
                    daysAvailable= daysAvailable,
                    hoursAvailable = hoursAvailable,
                    consent = consent,
                    source = source,
                    whyVolunteer = whyVolunteer,
                    signatureName = signatureName,
                    fullName = fullName,
                    signatureDate = signatureDate,
                    comments = comments,
                    currentDateTimestamp = currentDateTimestamp
                )
            }


        }

    }

    private fun onCancel() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun addEvent(
        name: String,
        email: String,
        phone: String,
        address: String,
        daysAvailable: String,
        hoursAvailable: String,
        consent: String,
        source: String,
        whyVolunteer: String,
        signatureName: String,
        fullName: String,
        signatureDate: String,
        comments: String,
        currentDateTimestamp: Timestamp,
    ) {

        val db = Firebase.firestore

        val randomId = java.util.UUID.randomUUID().toString()

        // Create a map to store data
        val data = hashMapOf(
            "Address" to address,
            "Comments. You can add more information on how you heard about us and share any other relevant information." to comments,
            "Date of Signature" to signatureDate,
            "Days of the week available to volunteer" to daysAvailable,
            "Email" to email,
            "How did you hear about us?" to source,
            "If under 18, please provide written consent from a parent or guardian." to consent,
            "Name" to name,
            "Number of hours available to volunteer (weekly)" to hoursAvailable,
            "Phone Number" to phone,
            "Signature (If minor, Guardian's signature)" to signatureName,
            "Submission Create Date" to currentDateTimestamp,
            "Submission ID" to randomId,
            "Submission Status" to "unread",
            "Why do you want to volunteer at Street Care?" to whyVolunteer
        )

        // Save to Firebase
        db.collection("SCChapterMembershipForm")
            .add(data)
            .addOnSuccessListener {
                val user = Firebase.auth.currentUser
                if (user != null) {
                    val usersDocRef = db.collection("users").document(user.uid)

                    usersDocRef.update("Type", "Chapter Member")
                        .addOnSuccessListener {
                            showSuccessDialog()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ChapterForm", "Error updating user type", e)
                            Toast.makeText(
                                this,
                                "Failed to update user type: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ChapterForm", "Error saving data", e)
                Toast.makeText(this, "Failed to save data: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.chapter_membership_registered, null)
        dialogView.findViewById<TextView>(R.id.textViewMessage).text =
            getString(R.string.thank_you_registered)
        dialogView.findViewById<TextView>(R.id.learnMoreLinkTextView).text =
            getString(R.string.chapter_membership_back_home)
        dialogView.findViewById<TextView>(R.id.learnMoreLinkTextView).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .show()

//        Toast.makeText(this, "You are a Chapter member!", Toast.LENGTH_SHORT).show()
    }
}

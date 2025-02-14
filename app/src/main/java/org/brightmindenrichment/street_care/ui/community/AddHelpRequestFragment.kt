package org.brightmindenrichment.street_care.ui.community

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.data.HelpRequestStatus
import org.brightmindenrichment.street_care.ui.user.ChapterMembershipFormOneAcitivity
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.requiredSkills
import java.time.LocalDateTime
import java.util.*

class AddHelpRequestFragment : Fragment() {
    private lateinit var edtTitle: EditText
    private lateinit var edtDesc: EditText
    private lateinit var edtStreet: EditText
    private lateinit var edtState: EditText
    private lateinit var edtCity: EditText
    private lateinit var edtZipcode: EditText
    private lateinit var edtIdentification: EditText
    private lateinit var checkedItems: BooleanArray

    private val selectedItems = mutableListOf<String>()

    //private var isPastEvents = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //isPastEvents = it.getBoolean("isPastEvents")
            checkedItems = it.getBooleanArray("skillsBooleanArray") ?: BooleanArray(requiredSkills.size)
        }

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                //activity!!.onBackPressedDispatcher.onBackPressed()

                val pageTitle = context?.getString(R.string.help_request)
                findNavController().popBackStack()
                findNavController().navigate(R.id.communityHelpRequestFragment, Bundle().apply {
                    //putBoolean("isPastEvents", isPastEvents)
                    putString("pageTitle", pageTitle)
                })


            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_help_request, container, false)
     }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                // the action of back icon
                //activity!!.onBackPressedDispatcher.onBackPressed()
                val pageTitle = context?.getString(R.string.help_request)
                findNavController().popBackStack()
                findNavController().navigate(R.id.communityHelpRequestFragment, Bundle().apply {
                    //putBoolean("isPastEvents", isPastEvents)
                    Log.d("debug", "pageTitle: $pageTitle")
                    putString("pageTitle", pageTitle)
                })
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val current = LocalDateTime.now()
        val currentDateInMillis = System.currentTimeMillis()

        edtTitle = view.findViewById<EditText>(R.id.edtTitle)
        edtDesc = view.findViewById<EditText>(R.id.edtDesc)
        edtStreet = view.findViewById<EditText>(R.id.edtStreet)
        edtState = view.findViewById<EditText>(R.id.edtState)
        edtCity = view.findViewById<EditText>(R.id.edtCity)
        edtZipcode = view.findViewById<EditText>(R.id.edtZipcode)
        edtIdentification = view.findViewById(R.id.edtIdentification)

        val btnRequiredSkills = view.findViewById<Button>(R.id.btnRequiredSkills)
        val tvRequiredSkills = view.findViewById<TextView>(R.id.tvRequiredSkills)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnDiscard = view.findViewById<Button>(R.id.buttonDiscard)

//        val initialDateTimeStamp = Timestamp(Date(myCalendar.timeInMillis))
//        Log.d("date", "initialDateTimeStamp: $initialDateTimeStamp")

        //val checkedItems = BooleanArray(requiredSkills.size)

        // handle the Open Alert Dialog button
        btnRequiredSkills.setOnClickListener {
            val alertDialog = createRequiredSkillsDialog(
                tvRequiredSkills,
                checkedItems,
                requiredSkills,
            )
            alertDialog.show()
        }

        btnSubmit.setOnClickListener {
            if (Firebase.auth.currentUser == null) {
                context?.let { context ->
                    Extensions.showDialog(
                        context,
                        context.getString(R.string.alert),
                        context.getString(R.string.please_login_before_event),
                        context.getString(R.string.ok),
                        context.getString(R.string.cancel)
                    )
                }
            } else {
                val title = edtTitle.text.toString()
                val desc = edtDesc.text.toString()
                val street = edtStreet.text.toString()
                val state = edtState.text.toString()
                val city = edtCity.text.toString()
                val zipcode = edtZipcode.text.toString()
                val identification = edtIdentification.text.toString()
                val currentDateTimestamp = Timestamp(Date(currentDateInMillis))

                if (TextUtils.isEmpty(title)) {
                    edtTitle.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(street)) {
                    edtStreet.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(state)) {
                    edtState.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(city)) {
                    edtCity.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(zipcode)) {
                    edtZipcode.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(identification)) {
                    edtIdentification.error = it.context.getString(R.string.required)
                }
                else if (selectedItems.isEmpty()) {
                    btnRequiredSkills.error = it.context.getString(R.string.required)
                }
                else {
                    addEvent(
                        title = title,
                        description = desc,
                        street = street,
                        state = state,
                        city = city,
                        zipcode = zipcode,
                        identification = identification,
                        currentDateTimestamp = currentDateTimestamp
                    )
                }
            }
        }
        btnDiscard.setOnClickListener{
            clearAllFields()
            requireActivity().onBackPressedDispatcher.onBackPressed()
            //findNavController().popBackStack()
            //findNavController().navigate(R.id.nav_community)
        }
    }

    private fun createRequiredSkillsDialog(
        tvRequiredSkills: TextView,
        checkedItems:     BooleanArray,
        requiredSkills:   Array<String>,
    ): AlertDialog {
        // initialise the alert dialog builder
        val builder = AlertDialog.Builder(this.context).apply {
            // set the title for the alert dialog
            setTitle(context.getString(R.string.select_required_skills))

            // set the icon for the alert dialog
            setIcon(R.drawable.streetcare_logo)

            // now this is the function which sets the alert dialog for multiple item selection ready
            setMultiChoiceItems(requiredSkills, checkedItems) { dialog, which, isChecked ->
                checkedItems[which] = isChecked
                val currentItem = requiredSkills[which]
            }

            // alert dialog shouldn't be cancellable
            setCancelable(false)

            // handle the positive button of the dialog
            setPositiveButton(context.getString(R.string.done)) { dialog, which ->
                tvRequiredSkills.text = ""
                selectedItems.clear()
                for (i in checkedItems.indices) {
                    if (checkedItems[i]) {
                        tvRequiredSkills.text = "${tvRequiredSkills.text}${requiredSkills[i]}, "
                        selectedItems.add(requiredSkills[i])
                    }
                }
                if(tvRequiredSkills.text == "") tvRequiredSkills.visibility = View.GONE
                else {
                    tvRequiredSkills.visibility = View.VISIBLE
                    tvRequiredSkills.text = tvRequiredSkills.text.substring(0, tvRequiredSkills.text.length - 2)
                }
            }

            // handle the negative button of the alert dialog
            setNegativeButton("CANCEL") { dialog, which -> }

            // handle the neutral button of the dialog to clear the selected items boolean checkedItem
            setNeutralButton(context.getString(R.string.clear_all)) { dialog: DialogInterface?, which: Int ->
                Arrays.fill(checkedItems, false)
                tvRequiredSkills.text = null
                tvRequiredSkills.visibility = View.GONE
            }

        }

        // create the alert dialog with the alert dialog builder instance
        return builder.create()
    }

    private fun clearAllFields() {
        edtTitle.text.clear()
        edtDesc.text.clear()
        edtStreet.text.clear()
        edtState.text.clear()
        edtCity.text.clear()
        edtZipcode.text.clear()
        edtIdentification.text.clear()
    }

    private fun addEvent(
        title: String,
        description: String,
        street: String,
        state: String,
        city: String,
        zipcode: String,
        identification: String,
        currentDateTimestamp: Timestamp,
    ) {
        // make sure somebody is logged in
        val user = Firebase.auth.currentUser ?: return
        // create a map of help request data so we can add to firebase
        val helpRequestData = hashMapOf(
            "createdAt" to currentDateTimestamp,
            "description" to description,
            "location" to mapOf(
                "street" to street,
                "state" to state,
                "city" to city,
                "zipcode" to zipcode
            ), // map: {city: String, state: String, street: String, zipcode: String
            "skills" to selectedItems, // array
            "title" to title,
            "uid" to user.uid,
            "identification" to identification,
            "outreachEvent" to emptyList<String>(),
            "status" to HelpRequestStatus.NeedHelp.status
        )
        // save to firebase
        val db = Firebase.firestore
        db.collection("helpRequests")
            .add(helpRequestData)
            .addOnSuccessListener { documentReference ->
                Log.d("BME", "Saved with id ${documentReference.id}")
//                Extensions.showDialog(
//                    requireContext(),
//                    requireContext().getString(R.string.alert),
//                    requireContext().getString(R.string.event_registered_for_approval),
//                    requireContext().getString(R.string.ok),
//                    requireContext().getString(R.string.cancel)
//                )
                // Assuming you have the necessary resources defined in your strings.xml

                val message = getString(R.string.thank_you_post)
                val approvalMessage = getString(R.string.approval_pending)
                val learnMoreText = getString(R.string.streamline_experience)
                val learnMoreLink = getString(R.string.learn_more)
                val alreadyChapterMember = getString(R.string.already_chapter_member)

// Create a custom layout for the dialog
                val dialogView = LayoutInflater.from(context).inflate(R.layout.chapter_membership_signup, null) // Assuming you have a custom_dialog_layout.xml
                dialogView.findViewById<TextView>(R.id.textViewMessage).text = message
                dialogView.findViewById<TextView>(R.id.approvalTextView).text = approvalMessage
                dialogView.findViewById<TextView>(R.id.learnMoreTextView).text = learnMoreText
                val usersDocRef = Firebase.firestore.collection("users").document(user.uid)

                usersDocRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val userType = document.getString("Type")
                            val learnMoreTextView = dialogView.findViewById<TextView>(R.id.learnMoreLinkTextView)

                            if (userType == "Chapter Member") {
                                learnMoreTextView.text = alreadyChapterMember
                                learnMoreTextView.isClickable = false
                                learnMoreTextView.isFocusable = false
                                val grayColor = ContextCompat.getColor(dialogView.context, R.color.gray)
                                learnMoreTextView.setTextColor(grayColor)
                            } else {
                                learnMoreTextView.text = learnMoreLink
                                learnMoreTextView.isClickable = true
                                learnMoreTextView.isFocusable = true
                                learnMoreTextView.setOnClickListener{
                                    try {
                                        val activityContext = dialogView.context as? Activity ?: return@setOnClickListener
                                        val intent = Intent(activityContext, ChapterMembershipFormOneAcitivity::class.java)
                                        activityContext.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(dialogView.context, "Error opening sign-up page. Please try again.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(dialogView.context, "Error opening sign-up page. Please try again.", Toast.LENGTH_LONG).show()

                    }
                val builder = AlertDialog.Builder(context)
                builder.setView(dialogView)
                builder.setCancelable(true) // Set to false if you don't want the dialog to be dismissed by tapping outside

                val dialog = builder.create()
                dialog.show()
                dialogView.findViewById<ImageView>(R.id.closeIcon).setOnClickListener {
                    dialog.dismiss() // Dismiss the dialog
                }

                clearAllFields()
                Toast.makeText(context, context?.getString(R.string.successfully_registered), Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                findNavController().navigate(R.id.nav_community)
            }
            .addOnFailureListener { exception ->
                Log.w("BMR", "Error in addEvent ${exception.toString()}")
                Toast.makeText(context, context?.getString(R.string.failed), Toast.LENGTH_LONG).show()
            }
    }
}
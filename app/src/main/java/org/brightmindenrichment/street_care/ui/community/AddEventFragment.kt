package org.brightmindenrichment.street_care.ui.community

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.ui.community.model.CommunityPageName
import org.brightmindenrichment.street_care.ui.user.ChapterMembershipFormOneAcitivity
import org.brightmindenrichment.street_care.util.Extensions
import org.brightmindenrichment.street_care.util.Extensions.Companion.customGetSerializable
import org.brightmindenrichment.street_care.util.Extensions.Companion.requiredSkills
import java.time.LocalDateTime
import java.util.*

class AddEventFragment : Fragment() {
    private lateinit var edtTitle: EditText
    private lateinit var edtEventStartDate: EditText
    private lateinit var edtEventEndDate: EditText
    private lateinit var edtEventStartTime: EditText
    private lateinit var edtEventEndTime: EditText
    private lateinit var edtHelpTypeRequired: EditText
    private lateinit var edtDesc: EditText
    private lateinit var edtStreet: EditText
    private lateinit var edtState: EditText
    private lateinit var edtCity: EditText
    private lateinit var edtZipcode: EditText
    private lateinit var edtMaxCapacity: EditText
    private lateinit var checkedItems: BooleanArray

    private val selectedItems = mutableListOf<String>()

    //private var isPastEvents = true
    private var communityPageName: CommunityPageName? = null
    private var edtTitleText: String? = null
    private var street: String? = null
    private var city: String? = null
    private var state: String? = null
    private var zipcode: String? = null
    private var edtDescriptionText: String? = null
    private var helpRequestId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            //isPastEvents = it.getBoolean("isPastEvents")
            communityPageName = it.customGetSerializable<CommunityPageName>("communityPageName")
            checkedItems = it.getBooleanArray("skillsBooleanArray") ?: BooleanArray(requiredSkills.size)
            edtTitleText = it.getString("title")
            street = it.getString("street")
            city = it.getString("city")
            state = it.getString("state")
            zipcode = it.getString("zipcode")
            edtDescriptionText = it.getString("description")
            helpRequestId = it.getString("helpRequestId")
        }

        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(communityPageName == null || communityPageName == CommunityPageName.HELP_REQUESTS) {
                    //activity!!.onBackPressedDispatcher.onBackPressed()
                    val pageTitle = createPageTitle()
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.communityHelpRequestFragment, Bundle().apply {
                        putString("pageTitle", pageTitle)
                    })
                }
                else {
                    //val pageTitle = if(isPastEvents) "Past Events" else "Upcoming Events"
                    val pageTitle = createPageTitle()
                    Log.d("debug", "communityPageName: $communityPageName")
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.communityEventFragment, Bundle().apply {
                        //putBoolean("isPastEvents", isPastEvents)
                        putString("pageTitle", pageTitle)
                        putSerializable("communityPageName", communityPageName)
                    })
                }
            }
        })

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add_event, container, false)
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
                Log.d("menuItem.isVisible", "menuItem.isVisible: " + menuItem.itemId)
                Log.d("syncWebApp", "Selected Menu Item: ${menuItem.title}, id: ${menuItem.itemId}")
                Log.d("debug", "communityPageName: $communityPageName")
                if(communityPageName == null || communityPageName == CommunityPageName.HELP_REQUESTS) activity!!.onBackPressedDispatcher.onBackPressed()
                else {
                    //val pageTitle = if(isPastEvents) "Past Events" else "Upcoming Events"
                    val pageTitle = createPageTitle()
                    findNavController().popBackStack()
                    findNavController().navigate(R.id.communityEventFragment, Bundle().apply {
                        //putBoolean("isPastEvents", isPastEvents)
                        putString("pageTitle", pageTitle)
                        putSerializable("communityPageName", communityPageName)
                    })
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val current = LocalDateTime.now()
        val currentDateInMillis = System.currentTimeMillis()

        edtTitle = view.findViewById<EditText>(R.id.edtTitle)
        edtEventStartDate = view.findViewById<EditText>(R.id.edtEventStartDate)
        edtEventEndDate = view.findViewById<EditText>(R.id.edtEventEndDate)
        edtEventStartTime = view.findViewById<EditText>(R.id.edtEventStartTime)
        edtEventEndTime = view.findViewById<EditText>(R.id.edtEventEndTime)
        edtDesc = view.findViewById<EditText>(R.id.edtDesc)
        edtStreet = view.findViewById<EditText>(R.id.edtStreet)
        edtState = view.findViewById<EditText>(R.id.edtState)
        edtCity = view.findViewById<EditText>(R.id.edtCity)
        edtZipcode = view.findViewById<EditText>(R.id.edtZipcode)
        edtHelpTypeRequired = view.findViewById<EditText>(R.id.edtHelpTypeRequired)
        edtMaxCapacity = view.findViewById<EditText>(R.id.edtMaxCapacity)

        val btnRequiredSkills = view.findViewById<Button>(R.id.btnRequiredSkills)
        val tvRequiredSkills = view.findViewById<TextView>(R.id.tvRequiredSkills)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val btnDiscard = view.findViewById<Button>(R.id.buttonDiscard)

        val startCalendar = Calendar.getInstance()
        val startHour = startCalendar.get(Calendar.HOUR)
        val startMinute = startCalendar.get(Calendar.MINUTE)
        val startYear = startCalendar.get(Calendar.YEAR)
        val startMonth = startCalendar.get(Calendar.MONTH)
        val startDay = startCalendar.get(Calendar.DAY_OF_MONTH)

        val endCalendar = Calendar.getInstance()
        val endHour = startCalendar.get(Calendar.HOUR)
        val endMinute = startCalendar.get(Calendar.MINUTE)
        val endYear = startCalendar.get(Calendar.YEAR)
        val endMonth = startCalendar.get(Calendar.MONTH)
        val endDay = startCalendar.get(Calendar.DAY_OF_MONTH)

        edtTitleText?.let{ edtTitle.setText(it) }
        street?.let { edtStreet.setText(it) }
        city?.let { edtCity.setText(it) }
        state?.let {edtState.setText(it)}
        zipcode?.let { edtZipcode.setText(it) }
        edtDescriptionText?.let{ edtDesc.setText(it) }

        // show skills
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

        edtEventStartTime.setOnClickListener {
            val timePickerDialog = context?.let { it1 ->
                TimePickerDialog(context,
                    R.style.MyDatePickerDialogTheme,
                    { view, hourOfDay, minute ->
                        val minuteStr = if(minute < 10) "0$minute" else "$minute"
                        val hourStr = if(hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
                        edtEventStartTime.setText("$hourStr:$minuteStr")
                        startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        startCalendar.set(Calendar.MINUTE, minute)
                    },
                    startHour,
                    startMinute,
                    false
                )
            }
            timePickerDialog?.show()
        }

        edtEventEndTime.setOnClickListener {
            val timePickerDialog = context?.let { it1 ->
                TimePickerDialog(context,
                    R.style.MyDatePickerDialogTheme,
                    { view, hourOfDay, minute ->
                        val minuteStr = if(minute < 10) "0$minute" else "$minute"
                        val hourStr = if(hourOfDay < 10) "0$hourOfDay" else "$hourOfDay"
                        edtEventEndTime.setText("$hourStr:$minuteStr")
                        endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        endCalendar.set(Calendar.MINUTE, minute)
                    },
                    endHour,
                    endMinute,
                    false
                )
            }
            timePickerDialog?.show()
        }

        edtEventStartDate.setOnClickListener {
            val datePickerDialog = context?.let { it1 ->
                DatePickerDialog(
                    it1,
                    R.style.MyDatePickerDialogTheme,
                    { view, year, monthOfYear, dayOfMonth ->
                        val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                        Log.d("Event Month", "Event Month"+monthOfYear)
                        edtEventStartDate.setText(dat)
                        startCalendar.set(Calendar.YEAR, year)
                        startCalendar.set(Calendar.MONTH, monthOfYear)
                        startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    },
                    startYear,
                    startMonth,
                    startDay
                )
            }
            datePickerDialog?.show()
        }

        edtEventEndDate.setOnClickListener {
            val datePickerDialog = context?.let { it1 ->
                DatePickerDialog(
                    it1,
                    R.style.MyDatePickerDialogTheme,
                    { view, year, monthOfYear, dayOfMonth ->
                        val dat = (dayOfMonth.toString() + "-" + (monthOfYear + 1) + "-" + year)
                        Log.d("Event Month", "Event Month"+monthOfYear)
                        edtEventEndDate.setText(dat)
                        endCalendar.set(Calendar.YEAR, year)
                        endCalendar.set(Calendar.MONTH, monthOfYear)
                        endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    },
                    endYear,
                    endMonth,
                    endDay
                )
            }
            datePickerDialog?.show()
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
                val startDate = edtEventStartDate.text.toString()
                val startDateTimeStamp = Timestamp(Date(startCalendar.timeInMillis))
                Log.d("date", "submitted dateTimeStamp: $startDateTimeStamp")
                val startTime = edtEventStartTime.text.toString()
                val desc = edtDesc.text.toString()
                val street = edtStreet.text.toString()
                val endDate = edtEventEndDate.text.toString()
                val endDateTimeStamp = Timestamp(Date(endCalendar.timeInMillis))
                val endTime =  edtEventEndTime.text.toString()
                val helpTypeRequired = edtHelpTypeRequired.text.toString()
                val state = edtState.text.toString()
                val city = edtCity.text.toString()
                val zipcode = edtZipcode.text.toString()
                val currentDateTimestamp = Timestamp(Date(currentDateInMillis))
                val maxCapacity = edtMaxCapacity.text.toString()

                if (TextUtils.isEmpty(title)) {
                    edtTitle.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(startDate)) {
                    edtEventStartDate.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(endDate)) {
                    edtEventEndDate.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(startTime)) {
                    edtEventStartTime.error = it.context.getString(R.string.required)
                }
                else if (TextUtils.isEmpty(endTime)) {
                    edtEventEndTime.error = it.context.getString(R.string.required)
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
                else if (TextUtils.isEmpty(helpTypeRequired)) {
                    edtHelpTypeRequired.error = it.context.getString(R.string.required)
                }
                else if (!TextUtils.isDigitsOnly(maxCapacity)) {
                    edtMaxCapacity.error = it.context.getString(R.string.digits_only)
                }
                else if (TextUtils.isEmpty(maxCapacity)) {
                    edtMaxCapacity.error = it.context.getString(R.string.required)
                }
                else {
                    addEvent(
                        title = title,
                        description = desc,
                        startDate = startDateTimeStamp,
                        endDate = endDateTimeStamp,
                        street = street,
                        state = state,
                        city = city,
                        zipcode = zipcode,
                        helpTypeRequired = helpTypeRequired,
                        currentDateTimestamp = currentDateTimestamp,
                        maxCapacity = maxCapacity,
                        helpRequestId = helpRequestId
                    )
                }
            }
        }
        btnDiscard.setOnClickListener{
            clearAllFields()
            findNavController().popBackStack()
            findNavController().navigate(R.id.nav_community)
        }
    }

    private fun createPageTitle(): String {
        return when(communityPageName) {
            CommunityPageName.UPCOMING_EVENTS -> view?.context?.getString(R.string.upcoming_events) ?: "Upcoming Events"
            CommunityPageName.PAST_EVENTS -> context?.getString(R.string.past_events) ?: "Past Events"
            CommunityPageName.HELP_REQUESTS -> view?.context?.getString(R.string.help_request) ?: "Help Requests"
            else -> ""
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
        edtEventStartDate.text.clear()
        edtEventEndDate.text.clear()
        edtEventStartTime.text.clear()
        edtEventEndTime.text.clear()
        edtDesc.text.clear()
        edtStreet.text.clear()
        edtState.text.clear()
        edtCity.text.clear()
        edtZipcode.text.clear()
        edtHelpTypeRequired.text.clear()
        edtMaxCapacity.text.clear()
    }

    private fun addEvent(
        title: String,
        description: String,
        startDate: Timestamp,
        endDate: Timestamp,
        street: String,
        state: String,
        city: String,
        zipcode: String,
        helpTypeRequired: String,
        currentDateTimestamp: Timestamp,
        maxCapacity: String,
        helpRequestId: String?
    ) {
        // make sure somebody is logged in
        val user = Firebase.auth.currentUser ?: return
        val totalSlots = (maxCapacity.ifBlank { "-1" }).toInt()
        val helpRequest = if(helpRequestId == null) listOf() else listOf(helpRequestId)
        // create a map of event data so we can add to firebase
        val eventData = hashMapOf(
            "approved" to false,
            "createdAt" to currentDateTimestamp,
            "description" to description,
            "eventDate" to startDate,
            "eventEndTime" to endDate,
            "eventStartTime" to startDate,
            "helpRequest" to helpRequest, // array
            "helpType" to helpTypeRequired, // string
            "interests" to 1, // int
            "location" to mapOf(
                "street" to street,
                "state" to state,
                "city" to city,
                "zipcode" to zipcode
            ), // map: {city: String, state: String, street: String, zipcode: String
            "participants" to listOf<String>(user.uid), // array
            "skills" to selectedItems, // array
            "status" to "pending",
            "title" to title,
            "totalSlots" to totalSlots,
            "uid" to user.uid,
        )
        // save to firebase
        val db = Firebase.firestore
        db.collection("outreachEventsDev")
            .add(eventData)
            .addOnSuccessListener { documentReference ->
                Log.d("BME", "Saved with id ${documentReference.id}")
//                Extensions.showDialog(
//                    requireContext(),
//                    requireContext().getString(R.string.alert),
//                    requireContext().getString(R.string.event_registered_for_approval),
//                    requireContext().getString(R.string.ok),
//                    requireContext().getString(R.string.cancel)
//                )
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
                val usersDocRef1 = Firebase.firestore.collection("users").document(user.uid)

                usersDocRef1.get()
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
// Create and show the dialog
                val builder = AlertDialog.Builder(context)
                builder.setView(dialogView)
                builder.setCancelable(true) // Set to false if you don't want the dialog to be dismissed by tapping outside

                val dialog = builder.create()
                dialog.show()
                dialogView.findViewById<ImageView>(R.id.closeIcon).setOnClickListener {
                    dialog.dismiss() // Dismiss the dialog
                }
                clearAllFields()
                val usersDocRef = db.collection("users").document(user.uid)
                usersDocRef
                    .update("outreachEvents", FieldValue.arrayUnion(documentReference.id))
                    .addOnSuccessListener { Log.d("syncWebApp", "successfully updated!") }
                    .addOnFailureListener { e -> Log.w("syncWebApp", "Error updateOutreachEvents", e) }
                Toast.makeText(context, context?.getString(R.string.successfully_registered), Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                findNavController().navigate(R.id.nav_community)
            }
            .addOnFailureListener { exception ->
                Log.w("BMR", "Error in addEvent ${exception.toString()}")
                Toast.makeText(context, context?.getString(R.string.failed), Toast.LENGTH_LONG).show()
            }
    }
    /*
    private suspend fun addEventAndLikedEvent(title: String, description: String, date: Timestamp, time: String, location: String) {
        // make sure somebody is logged in
        val user = Firebase.auth.currentUser ?: return
        // create a map of event data so we can add to firebase
        val eventData = hashMapOf(
            "title" to title,
            "description" to description,
            "date" to date,
            "interest" to 1,
            "time" to time,
            "location" to location,
            "uid" to user.uid,
            "status" to "pending")
        // save to firebase
        val db = Firebase.firestore

        val userDocRef = db.collection("users").document(user.uid).get().await()
        var profileImageUrl = "null"
        if(userDocRef.exists()) {
            profileImageUrl = userDocRef.get("profileImageUrl").toString()
        }
        val likedData = hashMapOf(
            "uid" to user.uid,
            "profileImageUrl" to profileImageUrl
        )

        db.collection("events").add(eventData).addOnSuccessListener { documentReference ->
            Log.d("addEvent", "Saved with id ${documentReference.id}")
            Extensions.showDialog(requireContext(), "Alert","Event registered for Approval", "Ok","Cancel")
            edtDate.text.clear()
            edtTime.text.clear()
            edtLocation.text.clear()
            edtDesc.text.clear()
            edtTitle.text.clear()

            likedData["eventId"] = documentReference.id
            db.collection("likedEvents").document()
                .set(likedData)
                .addOnSuccessListener {
                    Log.d("addEvent", "saved liked event")
                }
            Toast.makeText(context, "Successfully Registered", Toast.LENGTH_LONG).show()
            findNavController().navigate(R.id.nav_community)
        }.addOnFailureListener { exception ->
            Log.w("BMR", "Error in addEvent ${exception.toString()}")
            Toast.makeText(context, context?.getString(R.string.failed), Toast.LENGTH_LONG).show()
        }
    }

     */
}
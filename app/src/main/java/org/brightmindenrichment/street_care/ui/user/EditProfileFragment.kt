package org.brightmindenrichment.street_care.ui.user

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import org.brightmindenrichment.street_care.MainActivity
import org.brightmindenrichment.street_care.R
import org.brightmindenrichment.street_care.databinding.FragmentEditProfileBinding
import org.brightmindenrichment.street_care.util.Extensions

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EditProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private var isUserImageChanged = false
    private val storage = Firebase.storage
    private val storageRef = storage.reference
    private var userName: String = ""
    private var email: String = ""
    private var prevUserName: String = ""
    private var prevEmail: String = ""
    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher!!.launch(intent)
            } else {
                context?.let {
                    Extensions.showDialog(
                        it,
                        getString(R.string.warning),
                        getString(R.string.cannot_select_profile_picture),
                        getString(R.string.ok),
                        getString(R.string.cancel)
                    )
                }
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
            }
        }
    private val currentUser: FirebaseUser? get() = UserSingleton.userModel.currentUser

    private val CAMERA_PERMISSION_REQ_CODE = 100
    private val MEDIA_PERMISSION_REQ_CODE = 101

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("attach", "on attach run")

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                Log.d("Heya", "Message is this: ")
                Log.d("image", result.data?.data.toString())

                val imgUri: Uri? = result.data?.data
                if(imgUri != null) {
                    binding.profileimage.setImageURI(imgUri)
                    val fileName = "profile.jpg"
                    val imageRef = storageRef.child("users").child(currentUser?.uid ?: "??").child(fileName)
                    var uploadTask = imageRef.putFile(imgUri)
                    uploadTask.addOnSuccessListener { taskSnapshot ->
                        // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
                        // ...
                        imageRef.downloadUrl.addOnSuccessListener {uri->
                            val db = FirebaseFirestore.getInstance()
                            val userRef = db.collection("users").document(currentUser?.uid ?: "??")
                            userRef.update(mapOf(
                                "profileImageUrl" to uri.toString()
                            ),).addOnCompleteListener { task ->
                                if(task.isSuccessful){
                                    Toast.makeText(activity,
                                        getString(R.string.profile_image_url_add_success), Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Log.d(ContentValues.TAG, "Profile Image url add: fail")
                                }
                            }
                        }
                        Log.d(ContentValues.TAG, "image upload to firebase: success")
                    }.addOnFailureListener {
                        // Handle unsuccessful uploads
                        Log.d(ContentValues.TAG, "image upload to firebase: fail")
                    }
                } else {
                    Toast.makeText(requireContext(), "No Image was selected", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    //permType = android.permission.READ_MEDIA_IMAGES
    private fun checkMediaPermission(permType: String, reqCode: Int) {
        if(ActivityCompat.checkSelfPermission(requireContext(), permType) != PackageManager.PERMISSION_GRANTED) {
            //ask for permission(s)
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permType), reqCode)
        } else {
            //Permission is already granted
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//            startActivity(intent)
            resultLauncher.launch(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        activityResultLauncherImpl()
        getUserInfo()
        binding.btnSaveChanges.setOnClickListener{
            onSaveChanges()
        }
        binding.btnCancel.setOnClickListener{
            onCancel()
        }
        binding.txteditphoto.setOnClickListener{
            checkMediaPermission(Manifest.permission.READ_MEDIA_IMAGES, MEDIA_PERMISSION_REQ_CODE)

//            when {
//                ContextCompat.checkSelfPermission(
//                    requireContext(),
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                ) == PackageManager.PERMISSION_GRANTED -> {
//                    val intent =
//                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//                    activityResultLauncher!!.launch(intent)
//                    // You can use the API that requires the permission.
//                }
//                shouldShowRequestPermissionRationale() -> {
//                // In an educational UI, explain to the user why your app requires this
//                // permission for a specific feature to behave as expected, and what
//                // features are disabled if it's declined. In this UI, include a
//                // "cancel" or "no thanks" button that lets the user continue
//                // using your app without granting the permission.
//
//            }
//                else -> {
//                    // You can directly ask for the permission.
//                    // The registered ActivityResultCallback gets the result of this request.
//                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
//                }
//            }
        }
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
            return false;
    }

//    private fun activityResultLauncherImpl() {
//        activityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
//            ActivityResultContracts.StartActivityForResult()
//        ) { result ->
//            val resultCode = result.resultCode
//            val data = result.data
//            Log.d("test2", "The data is this: ")
//            if (data != null) {
//                Log.d("Image information", data.data.toString())
//            }
//            if (resultCode == Activity.RESULT_OK && data != null) {
    ///////////////////////////SET THIS VARIABLE TO TRUE/////////////////////////////////////////////////
//                isUserImageChanged = true;
//                val selectedImageUri: Uri? = data.data
//                if (selectedImageUri != null) {
//                    // update the preview image in the layout
//                    binding.profileimage.setImageURI(selectedImageUri)
//                    val fileName = "profile.jpg"
//                    val imageRef = storageRef.child("users").child(currentUser?.uid ?: "??").child(fileName)
//                    var uploadTask = imageRef.putFile(selectedImageUri)
//                    uploadTask.addOnSuccessListener { taskSnapshot ->
//                        // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
//                        // ...
//                        imageRef.downloadUrl.addOnSuccessListener {uri->
//                            val db = FirebaseFirestore.getInstance()
//                            val userRef = db.collection("users").document(currentUser?.uid ?: "??")
//                            userRef.update(mapOf(
//                                "profileImageUrl" to uri.toString()
//                            ),).addOnCompleteListener { task ->
//                                if(task.isSuccessful){
//                                    Toast.makeText(activity,
//                                        getString(R.string.profile_image_url_add_success), Toast.LENGTH_SHORT).show();
//                                }
//                                else{
//                                    Log.d(ContentValues.TAG, "Profile Image url add: fail")
//                                }
//                            }
//                        }
//                        Log.d(ContentValues.TAG, "image upload to firebase: success")
//                    }.addOnFailureListener {
//                        // Handle unsuccessful uploads
//                        Log.d(ContentValues.TAG, "image upload to firebase: fail")
//                    }
//
//                }
//
//            }
//            else{
//                Log.e("select picture","picture not selected from gallery")
//            }
//        }
//    }
//

    private fun getUserInfo(){
        val db = FirebaseFirestore.getInstance()
        val docRef = db.collection("users").document(currentUser?.uid ?: "??")
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.data
                    if (user != null) {
                        Log.d(ContentValues.TAG, "DocumentSnapshot data:"+ currentUser?.uid)
                        Log.d(ContentValues.TAG, "DocumentSnapshot data:"+ user["username"])
                        //event.title = document.get("title")?.toString() ?: "Unknown"
                        prevUserName = user["username"]?.toString() ?: "Unknown"
                        prevEmail = user["email"]?.toString() ?: "Unknown"
                        binding.editTextSignUpUserName.setText(prevUserName)
                        binding.editTextSignUpEmail.setText(prevEmail)
                    }

                } else {
                    Log.d(ContentValues.TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "get failed with ", exception)
            }
        val fileName = "profile.jpg"
        val imageRef = storageRef.child("users").child(currentUser?.uid ?: "??").child(fileName)
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            if(uri!=null){
                Picasso.get().load(uri).into(binding.profileimage)
            }
            else{
                binding.profileimage.setImageResource(R.drawable.ic_profile)
            }

            // Got the download URL for 'users/me/profile.png'
        }.addOnFailureListener {
            // Handle any errors
        }
    }

    private fun onSaveChanges(){
        userName = binding.editTextSignUpUserName.text.toString()
        email = binding.editTextSignUpEmail.text.toString()

        if (TextUtils.isEmpty(userName)) {
            binding.editTextSignUpUserName.setError(getString(R.string.mandatory))
        } else if (TextUtils.isEmpty(email)  ) {
            binding.editTextSignUpEmail.setError(getString(R.string.mandatory))
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextSignUpEmail.setError(getString(R.string.enter_valid_email_address))
        } else {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(currentUser?.uid ?: "??")
            userRef.update(mapOf(
                "username" to userName,
                "email" to email,
            ),).addOnCompleteListener { task ->
                if(task.isSuccessful){
                    Toast.makeText(activity, getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                }
                else{
                    Log.d(ContentValues.TAG, "Profile update: fail" +task.exception)
                    Toast.makeText(activity,
                        getString(R.string.profile_update_failed), Toast.LENGTH_SHORT).show();
                }
                findNavController().popBackStack()
            }
        }
    }

    private fun onCancel(){
        binding.editTextSignUpUserName.setText(prevUserName)
        binding.editTextSignUpEmail.setText(prevEmail)
        findNavController().popBackStack()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EditProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EditProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
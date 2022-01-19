package com.akhilasdeveloper.marsroverphotos.ui.fragments.saved

import android.app.Activity
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.akhilasdeveloper.marsroverphotos.R
import com.akhilasdeveloper.marsroverphotos.data.RoverMaster
import com.akhilasdeveloper.marsroverphotos.db.table.photo.MarsRoverPhotoTable
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.akhilasdeveloper.marsroverphotos.utilities.*

import com.akhilasdeveloper.marsroverphotos.databinding.FragmentSavedBinding
import com.akhilasdeveloper.marsroverphotos.ui.fragments.BaseFragment
import com.akhilasdeveloper.marsroverphotos.ui.fragments.home.recyclerview.RecyclerClickListener
import com.bumptech.glide.RequestManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import kotlinx.coroutines.launch
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import timber.log.Timber


@AndroidEntryPoint
class SavedFragment : BaseFragment(R.layout.fragment_saved), RecyclerClickListener {


    private var _binding: FragmentSavedBinding? = null
    internal val binding get() = _binding!!

    @Inject
    lateinit var utilities: Utilities
    private var master: RoverMaster? = null

    @Inject
    lateinit var requestManager: RequestManager
    private var adapter: MarsRoverSavedPhotoAdapter? = null
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSavedBinding.bind(view)

        init()
        initSignIn()
        sync()
        subscribeObservers()
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                initSignIn()
            }
        }

    private fun initSignIn() {

        val signInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (signInAccount != null) {

            Timber.d("Signed in as ${signInAccount.email}")

            authenticate(
                signInAccount
            )
        } else {

            Timber.d("Not Signed In")

            getSignInClient().let {
                resultLauncher.launch(it.signInIntent)
            }

        }

    }

    private fun authenticate(
        signInAccount: GoogleSignInAccount
    ) {
        var users : FirebaseUser? = firebaseAuth.currentUser
        if (users == null) {
            val credential: AuthCredential = GoogleAuthProvider.getCredential(signInAccount.idToken, null)
            firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    users = firebaseAuth.currentUser
                    users?.let{
                        signInSuccess(it)
                    }
                } else {
                    uiCommunicationListener.showSnackBarMessage(getString(R.string.sign_in_failed))
                }
            }
        }
    }

    private fun signInSuccess(user: FirebaseUser) {
        sync()
    }

    private fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
        return GoogleSignIn.getClient(requireActivity(), signInOptions.build())
    }


    private fun sync() {
        lifecycleScope.launch {
            if (utilities.isLikesInSync() == Constants.DATASTORE_FALSE)
                viewModel.syncLikedPhotos()
        }
    }

    private fun logout(){
        firebaseAuth.signOut()
        CoroutineScope(Dispatchers.IO).launch {
            utilities.setLikesSync(Constants.DATASTORE_FALSE)
        }
    }

    private fun init() {
        adapter = MarsRoverSavedPhotoAdapter(this, requestManager, utilities = utilities)
        val layoutManager = GridLayoutManager(
            requireContext(),
            Constants.GALLERY_SPAN,
            GridLayoutManager.VERTICAL,
            false
        )
        binding.apply {
            photoRecycler.setHasFixedSize(true)
            photoRecycler.layoutManager = layoutManager
            photoRecycler.adapter = adapter
        }
    }

    private fun subscribeObservers() {
        viewModel.dataStateRoverMaster.observe(viewLifecycleOwner, {
            val isHandled = it.hasBeenHandled()
            it.peekContent?.let { rover ->
                it.setAsHandled()
                master = rover
                setData()
                if (!isHandled) {
                    getData()
                }
            }
        })

        viewModel.dataStateLikedPhotos.observe(viewLifecycleOwner, {
            adapter?.submitData(viewLifecycleOwner.lifecycle, it)
        })
    }

    private fun getData() {
        master?.let { master ->
            viewModel.getLikedPhotos(master)
        }
    }

    private fun setData() {
        master?.let {
            setTitle()
        }
    }

    private fun setTitle() {
        binding.topAppbar.homeToolbarTop.title = master!!.name + " Rover (Liked Photos)"
        binding.topAppbar.homeCollapsingToolbarTop.title = master!!.name + " Rover (Liked Photos)"
    }

    override fun onItemSelected(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int
    ) {

    }


    override fun onItemLongClick(
        marsRoverPhoto: MarsRoverPhotoTable,
        position: Int,
        view: View,
        x: Float,
        y: Float
    ): Boolean {
        return true
    }

}
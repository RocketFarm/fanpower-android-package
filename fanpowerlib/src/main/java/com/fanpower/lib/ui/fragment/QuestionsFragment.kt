package com.fanpower.lib.ui.fragment

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fanpower.lib.R


import com.fanpower.lib.adapter.AnswerListAdapter
import com.fanpower.lib.api.ApiManager
import com.fanpower.lib.api.model.*
import com.fanpower.lib.databinding.FragmentQuestionsBinding
import com.fanpower.lib.interfaces.*
import com.fanpower.lib.utils.Constants.Extra.PropExtra
import com.fanpower.lib.utils.Constants.Generic.PostPickAdId
import com.fanpower.lib.utils.Constants.Generic.PrePickAdId
import com.fanpower.lib.utils.Constants.Generic.VerifyAdId
import com.fanpower.lib.utils.SharedPrefs
import com.fanpower.lib.utils.Utilities
import com.squareup.picasso.Picasso

class QuestionsFragment(onsucessCallback : VerificationPopUpShownCallback) : Fragment() {

    private var emailForVerification = ""
    private var phoneNumberForVerification = ""
    private val TAG = "QuestionsFragment"
    private var onsucessCallback : VerificationPopUpShownCallback

    private lateinit var binding: FragmentQuestionsBinding

    var view_: View? = null

    lateinit var timer: CountDownTimer

    lateinit var prop: Prop

    var enterCodeMode = false
    var isEmailMode = false

    lateinit var adapter : AnswerListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    init {
        this.onsucessCallback = onsucessCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentQuestionsBinding.inflate(inflater, container, false)
        view_ = binding.root

//        requireActivity().getWindow()
//            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        val bundle = arguments
        if (bundle != null) {
            prop = bundle.getSerializable(PropExtra) as Prop
            Log.i(TAG, "onCreateView: prop id " + prop.id)
            setUpView()
        }

        return view_
    }

    private fun setUpView() {
        getProps()

        if (!SharedPrefs.Utils.getFanId(requireActivity()).isNullOrEmpty()) {
            getFanPicks()
        }

        binding.title.setText(prop.proposition)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireActivity())

        binding.countryCodePicker.registerCarrierNumberEditText(binding.editTextCarrierNumber)

        binding.countryCodePicker.setDefaultCountryUsingNameCode("US")

        binding.editTextCarrierNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(editable: Editable?) {
                if (editable.isNullOrEmpty()) {
                    binding.sendBtn.setBackgroundResource(R.drawable.send_disabled)
                } else {
                    binding.sendBtn.setBackgroundResource(R.drawable.send_selected)
                }
            }
        })

     //   Log.i(TAG, "setUpView: answers " + prop.picks.size)

        if(prop.picks != null) {
             adapter = AnswerListAdapter(prop.picks, object : OnClickCallBack {
                override fun onClick(pick: Pick) {

                    if (SharedPrefs.Utils.getFanId(requireActivity()).isNullOrEmpty()) {
                        userChooseAPick(pick.id, VerifyAdId)
                        onsucessCallback.disableScroll()
                        binding.verifyIdentityLayout.visibility = View.VISIBLE

                    }else{
                        userChooseAPick(pick.id, PostPickAdId)
                        adapter.setAnswerModeView(true)
                    }
                    Log.i(TAG, "onClick: ")
                  //  adapter.notifyDataSetChanged()
                }
             },requireActivity())
            // Setting the Adapter with the recyclerview
            binding.recyclerView.adapter = adapter
        }

        binding.sendBtn.setOnClickListener {
            if (binding.editTextCarrierNumber.text.toString().isNotEmpty()) {
                if (enterCodeMode) {
                    binding.progressBar.visibility = View.VISIBLE
                    sendVerifyFanApi()
                } else {
                    Log.i(TAG, "setUpView: is not empty")
                    sendAuthenticateFanApi()
                }
            } else {
                Log.i(TAG, "setUpView: is empty")
            }
        }

        binding.resendCodeText.setOnClickListener {
            if (enterCodeMode) {
                resetTimer()
                sendAuthenticateFanApi()
            } else {
                switchToOtherModeToVerify()
            }
        }
    }

    private fun getFanPicks(){
        ApiManager.getFanPicks(requireActivity(),prop.id, object : FanPickCallback{
            override fun onSuccess(list: List<FanPickItem>?) {
                if (list != null) {
                    Log.i(TAG, "onSuccess: list size " + list.size)
                    for (x in list) {
                        for ((index, value) in prop.picks.withIndex()) {
                       //     Log.i(TAG, "onSuccess: prop id to compare "+ value.id)
                            if(x.pick_id.toString().equals(value.id)){
                                Log.i(TAG, "onSuccess: found at index " + index)
                                adapter.selectedIndex = index
                                adapter.setAnswerModeView(true)
                                adapter.notifyDataSetChanged()
                                break
                            }
                        }
                    }
                }
            }

            override fun onFailure(messageResponse: MessageResponse) {

            }
        })
    }

    private fun userChooseAPick(pickId : String,verifyAdId : Int) {
        createFanPick(pickId)
        getPublicAdsApi(verifyAdId)
    }

   private fun createFanPick(pickIdStr: String){
        ApiManager.createFanPick(requireActivity(),object : SuccessFailureCallback{
            override fun onSuccess() {

            }

            override fun onFailure(messageResponse: MessageResponse) {

            }
        },pickIdStr)
    }

    private fun hideVerificationPopUp(){
        resetTimer()
        onsucessCallback.enableScroll()
        binding.verifyIdentityLayout.visibility = View.GONE
        adapter.setAnswerModeView(true)
        getPublicAdsApi(PrePickAdId)
        adapter.notifyDataSetChanged()

    }

    private fun switchToOtherModeToVerify() {
        if (isEmailMode) { // change to phone mode
            binding.countryCodePicker.visibility = View.VISIBLE

            binding.editTextCarrierNumber.setHint("enter phone number")
            binding.editTextCarrierNumber.setText("")
            binding.resendCodeText.setText("or use your email address")
            binding.editTextCarrierNumber.inputType = InputType.TYPE_CLASS_PHONE
        } else { // change to email view

            binding.countryCodePicker.visibility = View.GONE
            binding.editTextCarrierNumber.setHint("johnsmith@gmail.com")
            binding.editTextCarrierNumber.setText("")
            binding.resendCodeText.setText("or use your phone number")
            binding.editTextCarrierNumber.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        isEmailMode = !isEmailMode
    }




    private fun changeVerifyUI() {
        binding.verifyTitle.setText(R.string.enter_the_code_you_received)
        binding.countryCodePicker.visibility = View.GONE
        binding.editTextCarrierNumber.setHint("- - - - - -")
        binding.editTextCarrierNumber.setText("")
        binding.resendCodeText.setText("Didn't get the code? Click to resend it")
    }

    private fun setUpTimer() {
        binding.timerText.visibility = View.VISIBLE
        binding.timerText.setText(R.string.enter_the_code_within_2_minutes_to_verify_your_pick)
        binding.timerText.setTextColor(ContextCompat.getColor(requireActivity(),R.color.grey_light))
        binding.resendCodeText.visibility = View.VISIBLE
        timer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

                val seconds = (millisUntilFinished / 1000) % 60
                val minutes = (millisUntilFinished / (1000 * 60) % 60)
                //         val text = (minutes == 0) ? "" : minutes
                binding.timerText.setText("Enter the code within " + minutes.toInt() + " min " + seconds.toInt() + " sec to verify your pick")
            }

            override fun onFinish() {

            }
        }
        timer.start()


        binding.bannerTitle.setOnClickListener {
            resetTimer()
        }
    }

   private fun resetTimer() {
        if (timer != null) {
            timer.cancel()
        }

        binding.timerText.setText(R.string.enter_the_code_within_2_minutes_to_verify_your_pick)
    }

    private fun sendAuthenticateFanApi() {
        binding.progressBar.visibility = View.VISIBLE
        val authenticateBody = AuthenticateBody()

        Log.i(TAG, "sendVerifyFanApi: " + binding.countryCodePicker.isValidFullNumber)

        //   authenticateBody.code = Constants.Generic.TwillioCode
        if (isEmailMode) {
            if(emailForVerification.isNullOrEmpty() ) {
                Log.i(TAG, "sendAuthenticateFanApi: going to get data from edittext")
                var email = binding.editTextCarrierNumber.text.toString().trim()
                if (!Utilities.isValidEmail(email)) {
                    Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                    return
                }
                emailForVerification = email
               
            }
            if(enterCodeMode) { // resending code
                authenticateBody.email = emailForVerification
            }else{
                authenticateBody.email = binding.editTextCarrierNumber.text.toString().trim()
            }
        } else {
            if(phoneNumberForVerification.isNullOrEmpty()) {
                phoneNumberForVerification = binding.countryCodePicker.fullNumberWithPlus
            //    phoneNumberForVerification = binding.editTextCarrierNumber.text.toString()
            }
            if(enterCodeMode) { // resending code
                authenticateBody.phoneNumber = phoneNumberForVerification
            }else{
               authenticateBody.phoneNumber =  binding.countryCodePicker.fullNumberWithPlus
             //   authenticateBody.phoneNumber =  binding.editTextCarrierNumber.text.toString()
            }
        }

        ApiManager.authenticateFan(requireActivity(), authenticateBody, object : SuccessFailureCallback {
                override fun onSuccess() {
                    if(!isAdded){
                        return
                    }
                    binding.progressBar.visibility = View.GONE
                    changeVerifyUI()
                    enterCodeMode = true
                    setUpTimer()
                }

                override fun onFailure(messageResponse: MessageResponse) {
                    binding.progressBar.visibility = View.GONE
                    if(!isAdded){
                        return
                    }
                    if(messageResponse != null){
                        activity?.runOnUiThread {
                            Toast.makeText(context,messageResponse.message,Toast.LENGTH_SHORT).show()
                        }
                    }

//                    changeVerifyUI()
//                    enterCodeMode = true
//                    setUpTimer()
                }
            })
    }

    private fun sendVerifyFanApi() {
        val verifyFanBody = VerifyFanBody()

        verifyFanBody.code = binding.editTextCarrierNumber.text.toString().trim()
        if (isEmailMode) {

            verifyFanBody.email = emailForVerification
        } else {
            verifyFanBody.phoneNumber =phoneNumberForVerification
        }

        ApiManager.verifyFan(requireActivity(), verifyFanBody, object : SuccessFailureCallback {
            override fun onSuccess() {
                if(!isAdded){
                    return
                }
                binding.progressBar.visibility = View.GONE
                enterCodeMode = false
                activity?.runOnUiThread {
                    Toast.makeText(context,"Verified !",Toast.LENGTH_SHORT).show()
                }

           //     SharedPrefs.Utils.saveIsUserVerified(requireActivity(),true)

                hideVerificationPopUp()

                val imm = context?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
                imm?.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)

                adapter.setAnswerModeView(true)
            }

            override fun onFailure(messageResponse: MessageResponse) {
                binding.progressBar.visibility = View.GONE
                if(!isAdded){
                    return
                }
                if(messageResponse != null){
                    activity?.runOnUiThread {
                        if(messageResponse.message.contains("invalid") || messageResponse.message.contains("Incorrect")){
                            resetTimer()
                            binding.timerText.setText(messageResponse.message)
                            binding.timerText.setTextColor(requireActivity().resources.getColor(R.color.red))
                        }
                        Toast.makeText(context,messageResponse.message,Toast.LENGTH_SHORT).show()
                    }
                }
//                    changeVerifyUI()
//                    enterCodeMode = true
//                    setUpTimer()
            }
        })
    }

    private fun getProps(){
        getPublicAdsApi(PrePickAdId)

      //  ApiManager.getProps(requireActivity())
    }

    private fun getPublicAdsApi( adId: Int) {
        ApiManager.getPublicAds(requireActivity(),adId,object : AdsCallback{
            override fun onSuccess(list: List<AdsResponseItem>?) {
                if(list != null && list.size>0){
                    var ad : AdsResponseItem  = list.get(0)
                    Log.i(TAG, "onSuccess: add to load " + ad)
                    Picasso.get().load(ad.ad_image).into(binding.adImage)


                }
            }

            override fun onFailure(messageResponse: MessageResponse) {

            }
        })
    }


}
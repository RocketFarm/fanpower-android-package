package com.fanpower.lib.ui.views


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.viewpager.widget.ViewPager.OnPageChangeListener

import com.fanpower.lib.R


import com.fanpower.lib.adapter.ViewPagerAdapter
import com.fanpower.lib.api.ApiFactory
import com.fanpower.lib.api.ApiManager
import com.fanpower.lib.api.model.MessageResponse
import com.fanpower.lib.api.model.Prop
import com.fanpower.lib.api.model.Publisher
import com.fanpower.lib.api.model.ReferralResponse
import com.fanpower.lib.databinding.FanPowerViewLibBinding
import com.fanpower.lib.interfaces.SuccessFailureCallback
import com.fanpower.lib.interfaces.VerificationPopUpShownCallback
import com.fanpower.lib.ui.fragment.QuestionsFragment
import com.fanpower.lib.utils.Constants
import com.fanpower.lib.utils.SharedPrefs
import com.fanpower.lib.utils.Utilities
import com.squareup.picasso.Picasso
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class FanPowerView : RelativeLayout {

    private val TAG = "FanPowerView"

    private var binding: FanPowerViewLibBinding

    private lateinit var activity: FragmentActivity

    private lateinit var propIds: Array<Int>
    private var publisherId: Int = 0
    private var publisherToken: String = ""
    private var publisher: Publisher? = null
    private var tokenForJwtRequest: String = ""
    private var shareUrl: String = ""

    private var urlToShare = ""


    private var props = ArrayList<Prop>()
    private var dots: List<ImageView>? = null

    private var currentAlpha = 0f
    //    private var compassNeedle: ImageView? = null

//    constructor(context: Context) : super(context) {
//        Log.d(TAG, "CompassView(context) called")
//
//        binding = ActivityMainBinding.inflate(LayoutInflater.from(context), this, true)
//
////            val inflater =
////                context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
////            binding = ActivityMainBinding.inflate(inflater);
//        //  this.activity = activity
//        Log.d(TAG, "Inflation started from constructor.")
//    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        Log.d(TAG, "CompassView(context, attrs) called")
//        binding = ActivityMainBinding.inflate(LayoutInflater.from(context), this, true)
        Log.d(TAG, "Inflation started from constructor.")
    }

    fun initView(
        tokenForJwtRequest: String,
        propIds: Array<Int>,
        publisherId: Int,
        publisherToken: String,
        shareUrl: String,
        activity: FragmentActivity
    ) {
        this.activity = activity
        this.tokenForJwtRequest = tokenForJwtRequest
        this.propIds = propIds
        this.publisherId = publisherId
        this.shareUrl = shareUrl
        this.publisherToken = publisherToken

        SharedPrefs.Utils.saveAdminToken(context, publisherToken)
        SharedPrefs.Utils.savePublisherId(context, publisherId)
        SharedPrefs.Utils.savePublisherToken(context, publisherToken)
        //   SharedPrefs.Utils.savePropId(context,propId)
        SharedPrefs.Utils.saveTokenForJWTRequest(context, tokenForJwtRequest)
        SharedPrefs.Utils.saveSourceUrl(context, this.shareUrl)

        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)


        if(SharedPrefs.Utils.getIsFirstRun(activity)){
            binding.popupBg.visibility = VISIBLE
        }else{
            binding.popupBg.visibility = GONE
        }

        setUpBtns()
        //    setupViewPager()
        setUpApis()


    }

    private fun setUpBtns() {
        binding.go.setOnClickListener {
            SharedPrefs.Utils.saveIsFirstRun(activity,false)
            Log.i(TAG, "setupViewPager: onclick")
            val fadeOutAnim = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
            binding.popupBg.startAnimation(fadeOutAnim)

            fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
                // All the other override functions
                override fun onAnimationStart(p0: Animation?) {

                }

                override fun onAnimationEnd(p0: Animation?) {
                    binding.popupBg.visibility = View.GONE
                }

                override fun onAnimationRepeat(p0: Animation?) {

                }
            })
        }

        binding.shareBtn.setOnClickListener {
            binding.sharePopup.visibility = View.VISIBLE
            val slideUpAnim = AnimationUtils.loadAnimation(activity, R.anim.slide_up_animaton)
            binding.sharePopUpCard.startAnimation(slideUpAnim)

            val fadeInAnim = AnimationUtils.loadAnimation(activity, R.anim.fade_in)
            binding.bgTintShare.visibility = VISIBLE
            binding.bgTintShare.startAnimation(fadeInAnim)


        }
        getReferrals()

        binding.termsAndConditionBtn.setOnClickListener {
            Utilities.openUrl(activity, "https://fanpower.io/terms/")
        }

        binding.learnMoreBtn.setOnClickListener {
            Utilities.openUrl(activity, "https://fanpower.io")
        }

        binding.facebookBtn.setOnClickListener {

            var title = ""
            Log.i(TAG, "setUpBtns: twiter link " + urlToShare)
            if (props != null && props.size >= binding.viewPager.currentItem && props.size != 0) {
                title = props.get(binding.viewPager.currentItem).proposition
            }

          //  title = title + "&hashtags=makeyourpick "

            var urlToShare = SharedPrefs.Utils.getReferralUrl(context)

            Log.i(TAG, "setUpBtns: url to share " + urlToShare)

            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, urlToShare)
            var facebookAppFound = false
            val matches: List<ResolveInfo> = activity.getPackageManager().queryIntentActivities(intent, 0)
            for (info in matches) {
                if (info.activityInfo.packageName.lowercase(Locale.getDefault())
                        .startsWith("com.facebook.katana")
                ) {
                    intent.setPackage(info.activityInfo.packageName)
                    facebookAppFound = true
                    break
                }
            }
            if (!facebookAppFound) {
                val sharerUrl = "https://www.facebook.com/sharer/sharer.php?u=$urlToShare"
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(sharerUrl))
            }

            activity.startActivity(intent)
        }

        binding.twitterBtn.setOnClickListener {
            var title = ""
            Log.i(TAG, "setUpBtns: twiter link " + urlToShare)
            if (props != null && props.size >= binding.viewPager.currentItem && props.size != 0 ) {
                title = props.get(binding.viewPager.currentItem).proposition
            }

            title = title + "&hashtags=makeyourpick "

            var url = SharedPrefs.Utils.getReferralUrl(context)

            Log.i(TAG, "setUpBtns: url is " + url)

            try {
                val tweetIntent = Intent(Intent.ACTION_SEND)
                tweetIntent.setPackage("com.twitter.android")
                tweetIntent.data =
                    Uri.parse("https://twitter.com/intent/tweet?text=" + title + " &url= " + url)
                tweetIntent.putExtra(Intent.EXTRA_TEXT, "your message")
                activity.startActivity(tweetIntent)
            } catch (e: ActivityNotFoundException) {
                val tweetIntent = Intent(Intent.ACTION_VIEW)
                tweetIntent.data =
                    Uri.parse("https://twitter.com/intent/tweet?text=" + title + " &url= " + url)
                activity.startActivity(tweetIntent)
            }
        }

        binding.smsBtn.setOnClickListener {
            var title = ""
            Log.i(TAG, "setUpBtns: twi4ter link " + urlToShare)
            if (props != null && props.size >= binding.viewPager.currentItem && props.size != 0) {
                title = props.get(binding.viewPager.currentItem).proposition
            }

            val smsIntent = Intent(Intent.ACTION_SENDTO)
            smsIntent.data = Uri.parse("smsto:")
            smsIntent.putExtra("sms_body", title + " " + urlToShare + "#makeyourpick ")
            activity.startActivity(smsIntent)
        }

        binding.crossBtn.setOnClickListener {
            val fadeOutAnim = AnimationUtils.loadAnimation(activity, R.anim.fade_out)
            binding.sharePopup.startAnimation(fadeOutAnim)

            fadeOutAnim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(p0: Animation?) {

                }

                override fun onAnimationEnd(p0: Animation?) {
                    binding.sharePopup.visibility = GONE
                }

                override fun onAnimationRepeat(p0: Animation?) {

                }
            })
        }
    }

    private fun getReferrals() {

        var currentPropId = "";
        if(props != null && props.size>= binding.viewPager.currentItem && props.size != 0){
            currentPropId = props.get(binding.viewPager.currentItem).id
        }

        val responseVerifyFan: Call<ReferralResponse> = ApiFactory.getInstance()!!.getReferrals(
            SharedPrefs.Utils.getPublisherId(context),
            SharedPrefs.Utils.getFanId(context).toString(),
            SharedPrefs.Utils.getPublisherToken(context).toString(),
            SharedPrefs.Utils.getSourceUrl(context).toString(),
            currentPropId
        )
        responseVerifyFan.enqueue(object : Callback<ReferralResponse?> {
            override fun onResponse(
                call: Call<ReferralResponse?>,
                response: Response<ReferralResponse?>
            ) {
                var referralResponse = response.body()
                Log.i(TAG, "onResponse: referral " + referralResponse)
                if (referralResponse != null) {

                    urlToShare = referralResponse.referral_url
                    SharedPrefs.Utils.saveReferralUrl(context, referralResponse.referral_url)
                    Log.i(TAG, "onResponse: url is " + referralResponse.referral_url)
                }
            }

            override fun onFailure(call: Call<ReferralResponse?>, t: Throwable) {

            }
        })
    }


    /* Remaining constructors here */

    init {
        Log.d(TAG, "Kotlin init block called.")
        binding = FanPowerViewLibBinding.inflate(LayoutInflater.from(context))
        addView(binding.root)
        //binding = ActivityMainBinding.inflate(LayoutInflater.from(context), null, false)

    }

    private fun setupViewPager() {
        if (activity.isFinishing) {
            return
        }
        //  setUpDummyData()

        var adapter = ViewPagerAdapter(activity.supportFragmentManager)

        // LoginFragment is the name of Fragment and the Login
        // is a title of tab

        for (x in props) {
            var bundle = Bundle()
            bundle.putSerializable(Constants.Extra.PropExtra, x)
            var frag = QuestionsFragment(object : VerificationPopUpShownCallback {
                override fun enableScroll() {
                    binding.viewPager.isPagingEnabled = true
                    getReferrals()
                }

                override fun disableScroll() {
                    binding.viewPager.isPagingEnabled = false
                }
            });


            frag.arguments = bundle

            adapter.addFragment(frag)
        }

        // setting adapter to view pager.
        binding.viewPager.setAdapter(adapter)
        addDots()

        //    binding.viewPager.adapter = viewPagerAdapter
            //    binding.dotsIndicator.attachTo(binding.viewPager)


    }



    public override fun onFinishInflate() {
        super.onFinishInflate()
        Log.d(TAG, "onFinishInflate() called.")
        //    compassNeedle = findViewById(R.id.compass_needle)
    }



    companion object {
        private const val TAG = "Compass_View_Kotlin"
    }


    private fun setUpApis() {
        getIPLocationApi()
        if (SharedPrefs.Utils.getAuthToken(context) != null) {
            getPublisherDataFromServer()
        } else {
            ApiManager.getAuthToken(activity, object : SuccessFailureCallback {
                override fun onSuccess() {
                    if (activity == null) {
                        return
                    }
                    getPublisherDataFromServer()

                }

                override fun onFailure(messageResponse: MessageResponse) {
                    if (activity == null) {
                        return
                    }
                    if (messageResponse != null) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, messageResponse.message, Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            })
        }
    }

    private fun getIPLocationApi() {
        ApiManager.getIPLocation(activity, object : SuccessFailureCallback {
            override fun onSuccess() {

            }

            override fun onFailure(messageResponse: MessageResponse) {

            }
        })
    }

   private fun getPublisherDataFromServer() {
//        publisher = SharedPrefs.Utils.getPublisher(context,Publisher::class.java)
//        if(publisher != null){
//            Log.i(TAG, "getPublisherDataFromServer: publisher is not null " + publisher)
//            randerUiWithPublisher()
//            getProps()
//        }else {

        ApiManager.getPublisher(activity, object : SuccessFailureCallback {
            override fun onSuccess() {
                if (activity == null) {
                    return
                }
                publisher = SharedPrefs.Utils.getPublisher(context, Publisher::class.java)
                if (publisher != null) {
                    randerUiWithPublisher()
                    getProps()
                    if (SharedPrefs.Utils.isLoggedIn(context)) {
                        ApiManager.getFanProfile(activity)
                    }
                }
            }

            override fun onFailure(messageResponse: MessageResponse) {

            }
        })
        //  }
    }


    private fun getProps() {
        if (!Utilities.isOnline(activity)) {
            return
        }

        binding.progressbar.visibility = View.VISIBLE

        for (x in propIds) {

            val responseVerifyFan: Call<ArrayList<Prop>> =
                ApiFactory.getInstance()!!.getProps(x.toString())
            //     val responseVerifyFan: Call<String> = ApiFactory.getInstance()!!.getProps(5,1)
            responseVerifyFan.enqueue(object : Callback<ArrayList<Prop>?> {
                override fun onResponse(
                    call: Call<ArrayList<Prop>?>,
                    response: Response<ArrayList<Prop>?>
                ) {
                    binding.progressbar.visibility = View.GONE
                    Log.i(TAG, "onResponse: " + response)
                    Log.i(TAG, "onResponse: code " + response.code())

                    if (response != null) {
                        props.add(response.body()!!.get(0))
                      //  props = response.body()!!
//                        if (response.body()!!.size > 0) {
//                            props.add(response.body()!!.get(0))
//                        }
                        setupViewPager()
                    }
                }

                override fun onFailure(call: Call<ArrayList<Prop>?>, t: Throwable) {
                    binding.progressbar.visibility = View.GONE
                    Log.i(TAG, "onFailure: " + t.message)
                    //    successFailureCallback.onFailure()

                }
            })
        }
    }

    private fun randerUiWithPublisher() {
        Log.i(TAG, "randerUiWithPublisher: going to load " + publisher?.logo_url)
        Picasso.get().load(publisher?.picker_logo_url).into(binding.headerImg)
        binding.headerImg.visibility = View.VISIBLE
    }

    private fun addDots() {
        if(props.size == 0){
            return
        }

        dots = ArrayList<ImageView>()
      //  val dotsLayout = findViewById<View>(R.id.dots) as LinearLayout

        binding.dots.removeAllViews()


      //  Log.i(TAG, "addDots: props size " + props.size)
        for (i in 0 until props.size) {
            Log.i(TAG, "addDots: in dots loop")
            val dot = ImageView(activity)
            dot.setImageDrawable(resources.getDrawable(R.drawable.dots_unselected))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.marginEnd = 20
            binding.dots.addView(dot, params)
            (dots as ArrayList<ImageView>).add(dot)
        }
        binding.viewPager.setOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                selectDot(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        if(props.size>= binding.viewPager.currentItem) {
            selectDot(binding.viewPager.currentItem)
        }
    }

    private fun selectDot(idx: Int) {
        val res = resources
        for (i in 0 until props.size) {
            val drawableId: Int =
                if (i == idx) R.drawable.dots_selected else R.drawable.dots_unselected
            val drawable = res.getDrawable(drawableId)
            dots?.get(i)?.setImageDrawable(drawable)
        }
    }

    public fun logout(){
        SharedPrefs.Utils.saveFanId(activity,null)
    }
}

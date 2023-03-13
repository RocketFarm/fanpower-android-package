package com.fanpower.lib.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.AccessController.getContext


class Utilities {

    companion object{

        fun isValidEmail(target: CharSequence?): Boolean {
            return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }

        fun pxToDp(px: Int): Int {
            return (px / Resources.getSystem().displayMetrics.density).toInt()
        }

        fun isOnline(context: Context): Boolean {
            val connectivityManager =
                context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityManager != null) {
                val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                        return true
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                        return true
                    }
                }
            }

//            context.runOnUiThread(Runnable {
//                Toast.makeText(context,"No working internet connection.",Toast.LENGTH_SHORT).show()
//            })


            return false
        }

        fun getIpv4HostAddress(): String {
            NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
                networkInterface.inetAddresses?.toList()?.find {
                    !it.isLoopbackAddress && it is Inet4Address
                }?.let { return it.hostAddress }
            }
            return ""
        }

//        fun getReferralString(context: Activity,title : String): String? {
//            var referralUrl = SharedPrefs.Utils.getReferralUrl(context)
//            if(referralUrl != null){
//                if(title != null){
//                    return "$title  $referralUrl #makeyourpick"
//                }
//            } else{
//                if(title != null){
//                    return "$title ${Constants.Generic.publisherShareUrl} #makeyourpick"
//                }
//            }
//            return ""
//        }

        fun openUrl(context: Context,url : String){
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context?.startActivity(browserIntent)
        }


        fun getActivity(view: View): Activity? {
            var context = view.context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    return context
                }
                context = context.baseContext
            }
            return null
        }

//        private fun getActivity(): Activity? {
//            var context: Context? = getContext()
//            while (context is ContextWrapper) {
//                if (context is Activity) {
//                    return context
//                }
//                context = context.baseContext
//            }
//            return null
//        }


    }
}
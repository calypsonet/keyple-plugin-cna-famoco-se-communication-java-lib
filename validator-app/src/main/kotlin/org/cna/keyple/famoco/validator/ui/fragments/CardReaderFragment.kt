package org.cna.keyple.famoco.validator.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieDrawable
import dagger.android.support.DaggerFragment
import org.cna.keyple.famoco.validator.R
import org.cna.keyple.famoco.validator.data.model.CardReaderResponse
import org.cna.keyple.famoco.validator.data.model.Status
import org.cna.keyple.famoco.validator.databinding.FragmentCardReaderBinding
import org.cna.keyple.famoco.validator.di.scopes.ActivityScoped
import org.cna.keyple.famoco.validator.ui.BaseView
import org.cna.keyple.famoco.validator.util.ActivityUtils
import org.cna.keyple.famoco.validator.viewModels.CardReaderViewModel
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException
import timber.log.Timber
import javax.inject.Inject

@ActivityScoped
class CardReaderFragment @Inject constructor() : DaggerFragment(), BaseView {
    var cardReaderViewModel: CardReaderViewModel? = null

    @JvmField
    @Inject
    var viewModelFactory: ViewModelProvider.Factory? = null
    var binding: FragmentCardReaderBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_card_reader,
            container,
            false
        )
        cardReaderViewModel =
            ViewModelProvider(this, viewModelFactory!!).get(
                CardReaderViewModel::class.java
            )
        binding?.setLifecycleOwner(this)
        binding?.setViewModel(cardReaderViewModel)
        binding?.animation?.setAnimation("card_scan.json")
        binding?.animation?.playAnimation()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
        try {
            cardReaderViewModel!!.initCardReader()
        } catch (e: KeypleBaseException) {
            Timber.e(e)
        }
        return binding?.getRoot()
    }

    override fun onResume() {
        super.onResume()
        bindViewModel()
        binding!!.animation.playAnimation()
        cardReaderViewModel!!.startNfcDetection(activity)
    }

    override fun onPause() {
        super.onPause()
        unbindViewModel()
        binding!!.animation.cancelAnimation()
        cardReaderViewModel!!.stopNfcDetection(activity)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun bindViewModel() {
        cardReaderViewModel!!.response.observe(
            this,
            Observer { cardReaderResponse: CardReaderResponse? ->
                changeDisplay(cardReaderResponse)
            }
        )
    }

    override fun unbindViewModel() {
        cardReaderViewModel!!.response.removeObservers(this)
    }

    fun changeDisplay(cardReaderResponse: CardReaderResponse?) {
        if (cardReaderResponse != null) {
            if (cardReaderResponse.status === Status.LOADING) {
                binding!!.presentCardTv.visibility = View.GONE
                binding!!.mainView.setBackgroundColor(resources.getColor(R.color.turquoise))
                (activity as AppCompatActivity?)!!.supportActionBar!!.show()
                val mTitle =
                    activity!!.findViewById<TextView>(R.id.toolbar_title)
                mTitle.setText(R.string.card_reading_title)
                binding!!.animation.playAnimation()
                binding!!.animation.repeatCount = LottieDrawable.INFINITE
            } else {
                binding!!.animation.cancelAnimation()
                val bundle = Bundle()
                bundle.putString(
                    CardSummaryFragment.STATUS_KEY,
                    cardReaderResponse.status.toString()
                )
                bundle.putInt(CardSummaryFragment.TICKETS_KEY, cardReaderResponse.ticketsNumber)
                bundle.putString(CardSummaryFragment.CONTRACT, cardReaderResponse.contract)
                bundle.putString(CardSummaryFragment.CARD_TYPE, cardReaderResponse.cardType)
                val fragment = CardSummaryFragment()
                fragment.arguments = bundle
                ActivityUtils.addFragmentToActivity(
                    fragmentManager!!,
                    fragment,
                    R.id.contentFrame
                )
            }
        } else {
            binding!!.presentCardTv.visibility = View.VISIBLE
        }
    }

    companion object {
        private const val TAG_NFC_ANDROID_FRAGMENT =
            "org.eclipse.keyple.plugin.android.nfc.AndroidNfcFragment"
    }
}
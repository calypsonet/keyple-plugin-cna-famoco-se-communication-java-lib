package org.cna.keyple.famoco.validator.ui.fragments;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.airbnb.lottie.LottieDrawable;

import org.cna.keyple.famoco.validator.R;
import org.cna.keyple.famoco.validator.data.model.CardReaderResponse;
import org.cna.keyple.famoco.validator.data.model.Status;
import org.cna.keyple.famoco.validator.databinding.FragmentCardReaderBinding;
import org.cna.keyple.famoco.validator.di.scopes.ActivityScoped;
import org.cna.keyple.famoco.validator.ui.BaseView;
import org.cna.keyple.famoco.validator.util.ActivityUtils;
import org.cna.keyple.famoco.validator.viewModels.CardReaderViewModel;
import org.eclipse.keyple.core.seproxy.exception.KeypleBaseException;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;

@ActivityScoped
public class CardReaderFragment extends DaggerFragment implements BaseView {

    CardReaderViewModel cardReaderViewModel;

    private static final String TAG_NFC_ANDROID_FRAGMENT =
            "org.eclipse.keyple.plugin.android.nfc.AndroidNfcFragment";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    FragmentCardReaderBinding binding;

    @Inject
    public CardReaderFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_card_reader, container, false);
        cardReaderViewModel = ViewModelProviders.of(this, viewModelFactory).get(CardReaderViewModel.class);
        binding.setLifecycleOwner(this);
        binding.setViewModel(cardReaderViewModel);

        binding.animation.setAnimation("card_scan.json");
        binding.animation.playAnimation();

        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();

        try {
            cardReaderViewModel.initCardReader();
        } catch (KeypleBaseException e) {
            e.printStackTrace();
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindViewModel();
        binding.animation.playAnimation();

        // Add NFC Fragment to activity in order to communicate with Android Plugin
        //getFragmentManager().beginTransaction().add(AndroidNfcFragment.newInstance(), TAG_NFC_ANDROID_FRAGMENT).commit();
    }

    @Override
    public void onPause() {
        super.onPause();
        unbindViewModel();
        binding.animation.cancelAnimation();

        // Destroy AndroidNFC fragment
//        Fragment f = getFragmentManager().findFragmentByTag(TAG_NFC_ANDROID_FRAGMENT);
//        if (f != null) {
//            getFragmentManager().beginTransaction().remove(f).commit();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void bindViewModel() {
        cardReaderViewModel.getResponse().observe(this, this::changeDisplay);
    }

    @Override
    public void unbindViewModel() {
        cardReaderViewModel.getResponse().removeObservers(this);
    }

    public void changeDisplay(CardReaderResponse cardReaderResponse) {
        if (cardReaderResponse != null) {
            if (cardReaderResponse.status == Status.LOADING) {
                binding.presentCardTv.setVisibility(View.GONE);
                binding.mainView.setBackgroundColor(getResources().getColor(R.color.turquoise));
                ((AppCompatActivity) getActivity()).getSupportActionBar().show();
                TextView mTitle = getActivity().findViewById(R.id.toolbar_title);
                mTitle.setText(R.string.card_reading_title);

                binding.animation.playAnimation();
                binding.animation.setRepeatCount(LottieDrawable.INFINITE);
            } else {
                binding.animation.cancelAnimation();
                Bundle bundle = new Bundle();
                bundle.putString(CardSummaryFragment.STATUS_KEY, cardReaderResponse.status.toString());
                bundle.putInt(CardSummaryFragment.TICKETS_KEY, cardReaderResponse.ticketsNumber);
                bundle.putString(CardSummaryFragment.CONTRACT, cardReaderResponse.contract);
                bundle.putString(CardSummaryFragment.CARD_TYPE, cardReaderResponse.cardType);
                CardSummaryFragment fragment = new CardSummaryFragment();
                fragment.setArguments(bundle);
                ActivityUtils.addFragmentToActivity(getFragmentManager(), fragment, R.id.contentFrame);
            }
        } else {
            binding.presentCardTv.setVisibility(View.VISIBLE);
        }
    }
}

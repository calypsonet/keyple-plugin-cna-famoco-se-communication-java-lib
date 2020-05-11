package org.cna.keyple.famoco.validator.ui.fragments;

import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cna.keyple.famoco.validator.R;
import org.cna.keyple.famoco.validator.data.model.Status;
import org.cna.keyple.famoco.validator.databinding.FragmentCardSummaryBinding;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CardSummaryFragment extends Fragment {

    public static final String STATUS_KEY = "status";
    public static final String TICKETS_KEY = "tickets";
    public static final String CONTRACT = "contract";
    public static final String CARD_TYPE = "cardtype";

    private static final Integer RETURN_DELAY_MS = 6000;

    FragmentCardSummaryBinding binding;

    private Timer timer = new Timer();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_card_summary, container, false);
        binding.setLifecycleOwner(this);

        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        TextView mTitle = getActivity().findViewById(R.id.toolbar_title);
        Toolbar toolbar = getActivity().findViewById(R.id.my_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_icon_back);
        toolbar.setNavigationOnClickListener(v -> getActivity().onBackPressed());

        Status status = Status.getStatus(getArguments().getString(STATUS_KEY));

        switch (status){
            case TICKETS_FOUND:
                DateFormat df = new SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.ENGLISH);
                String date = df.format(Calendar.getInstance().getTime());

                binding.mainView.setBackgroundColor(getResources().getColor(R.color.green));
                binding.animation.setAnimation("tick_white.json");
                mTitle.setText(R.string.valid_title);
                binding.bigText.setText(R.string.valid_main_desc);
                int nbTickets = getArguments().getInt(TICKETS_KEY);
                if(nbTickets != 0) {
                    binding.smallDesc.setText(String.format(getString(R.string.valid_small_desc), date, nbTickets));
                } else {
                    binding.smallDesc.setText(String.format(getString(R.string.valid_season_ticket_small_desc), date, getArguments().getString(CONTRACT).trim()));
                }
                binding.mediumText.setText(R.string.valid_last_desc);
                binding.mediumText.setVisibility(View.VISIBLE);
                break;
            case INVALID_CARD:
                binding.mainView.setBackgroundColor(getResources().getColor(R.color.orange));
                binding.animation.setAnimation("error_white.json");
                mTitle.setText(R.string.card_invalid_title);
                binding.bigText.setText(R.string.card_invalid_main_desc);
                binding.smallDesc.setText(String.format(getString(R.string.card_invalid_small_desc), getArguments().getString(CARD_TYPE).trim()));
                binding.mediumText.setVisibility(View.GONE);
                break;
            case EMPTY_CARD:
                binding.mainView.setBackgroundColor(getResources().getColor(R.color.red));
                binding.animation.setAnimation("error_white.json");
                mTitle.setText(R.string.no_tickets_title);
                binding.bigText.setText(R.string.no_tickets_main_desc);
                binding.smallDesc.setText(R.string.no_tickets_small_desc);
                binding.mediumText.setVisibility(View.GONE);
                break;
            default:
                binding.mainView.setBackgroundColor(getResources().getColor(R.color.red));
                binding.animation.setAnimation("error_white.json");
                mTitle.setText(R.string.error_title);
                binding.bigText.setText(R.string.error_main_desc);
                binding.smallDesc.setText(R.string.error_small_desc);
                binding.mediumText.setVisibility(View.GONE);
                break;
        }

        binding.animation.playAnimation();

        //Play sound
        final MediaPlayer mp = MediaPlayer.create(getActivity(), R.raw.reading_sound);
        mp.start();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(() -> getActivity().onBackPressed());
            }
        }, RETURN_DELAY_MS);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        timer.cancel();
    }
}

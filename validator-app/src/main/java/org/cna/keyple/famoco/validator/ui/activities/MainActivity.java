package org.cna.keyple.famoco.validator.ui.activities;

import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.Toolbar;

import org.cna.keyple.famoco.validator.R;
import org.cna.keyple.famoco.validator.ui.fragments.CardReaderFragment;
import org.cna.keyple.famoco.validator.util.ActivityUtils;

import javax.inject.Inject;

import dagger.android.support.DaggerAppCompatActivity;

@VisibleForTesting
public class MainActivity extends DaggerAppCompatActivity {
    @Inject
    CardReaderFragment mInjectedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup custom toolbar as main action bar
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        myToolbar.setTitle("");
        setSupportActionBar(myToolbar);

        // Set up fragment
        CardReaderFragment fragment =
                (CardReaderFragment) getSupportFragmentManager().findFragmentById(R.id.contentFrame);
        if (fragment == null) {
            fragment = mInjectedFragment;
            ActivityUtils.addFragmentToActivity(getSupportFragmentManager(), fragment, R.id.contentFrame);
        }
    }
}

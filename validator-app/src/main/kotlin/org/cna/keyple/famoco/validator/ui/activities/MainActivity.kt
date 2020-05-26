package org.cna.keyple.famoco.validator.ui.activities

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.appcompat.widget.Toolbar
import dagger.android.support.DaggerAppCompatActivity
import org.cna.keyple.famoco.validator.R
import org.cna.keyple.famoco.validator.ui.fragments.CardReaderFragment
import org.cna.keyple.famoco.validator.util.ActivityUtils.addFragmentToActivity
import javax.inject.Inject

@VisibleForTesting
class MainActivity : DaggerAppCompatActivity() {
    @JvmField
    @Inject
    var mInjectedFragment: CardReaderFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup custom toolbar as main action bar
        val myToolbar =
            findViewById<Toolbar>(R.id.my_toolbar)
        myToolbar.title = ""
        setSupportActionBar(myToolbar)

        // Set up fragment
        var fragment =
            supportFragmentManager.findFragmentById(R.id.contentFrame) as CardReaderFragment?
        if (fragment == null) {
            fragment = mInjectedFragment
            addFragmentToActivity(
                supportFragmentManager,
                fragment!!,
                R.id.contentFrame
            )
        }
    }
}
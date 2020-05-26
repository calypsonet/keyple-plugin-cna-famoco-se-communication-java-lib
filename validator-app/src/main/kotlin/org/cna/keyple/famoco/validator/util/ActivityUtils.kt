package org.cna.keyple.famoco.validator.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import dagger.internal.Preconditions

object ActivityUtils {
    /**
     * The fragment is added to the container view with id frameId. The operation is
     * performed by the fragmentManager.
     */
    @JvmStatic
    fun addFragmentToActivity(
        fragmentManager: FragmentManager,
        fragment: Fragment, frameId: Int
    ) {
        Preconditions.checkNotNull(
            fragmentManager
        )
        Preconditions.checkNotNull(fragment)
        val transaction =
            fragmentManager.beginTransaction()
        transaction.replace(frameId, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
}
package org.cna.keyple.famoco.validator.rx

import io.reactivex.Scheduler
import javax.inject.Inject

class SchedulerProvider @Inject constructor(
    private val backScheduler: Scheduler,
    private val foreScheduler: Scheduler
) {

    /**
     * IO thread pool scheduler
     */
    fun io(): Scheduler {
        return backScheduler
    }

    /**
     * Main Thread scheduler
     */
    fun ui(): Scheduler {
        return foreScheduler
    }

}
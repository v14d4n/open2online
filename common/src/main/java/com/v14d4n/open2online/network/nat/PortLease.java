package com.v14d4n.open2online.network.nat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Keeps a port mapping alive by re-asking the router for it on a schedule, until told to stop.
 *
 * <p>Both backends that need this used to own a thread looping on {@link Thread#sleep}, stopped with
 * an interrupt. A scheduler expresses the same thing without a thread spending its life asleep: the
 * {@link ScheduledExecutorService} owns the timing, and the {@link ScheduledFuture} it hands back is
 * the handle that calls the work off — the piece the hand-rolled loop never really had.
 */
final class PortLease {
    private static final Logger LOGGER = LoggerFactory.getLogger("Open2Online");

    private final ScheduledExecutorService scheduler;

    /** Written from the publish worker, read again when the host quits, which is the server thread. */
    private volatile ScheduledFuture<?> renewal;

    PortLease(String threadName) {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            // Daemon, so a lease outliving its owner cannot hold the game open. No thread is created
            // until something is actually scheduled, so an unused instance costs nothing.
            Thread thread = new Thread(task, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Runs {@code renew} at once and then repeatedly, counting the wait from the end of the previous
     * run rather than its start: renewing talks to the router, and a fixed rate would stack calls up
     * behind one that ran long.
     */
    void renewEvery(long period, TimeUnit unit, Runnable renew) {
        renewal = scheduler.scheduleWithFixedDelay(() -> {
            try {
                renew.run();
            } catch (RuntimeException e) {
                // Without this the schedule is dropped at the first failure and says nothing about
                // it — that is what scheduleWithFixedDelay does with a task that throws. A router
                // refusing once is no reason to stop renewing.
                LOGGER.warn("Renewing the port mapping failed; trying again next time", e);
            }
        }, 0L, period, unit);
    }

    /** Stops the schedule and releases the thread. Does nothing gracefully if nothing was scheduled. */
    void cancel() {
        ScheduledFuture<?> pending = renewal;
        if (pending != null) {
            pending.cancel(true);
            renewal = null;
        }
        scheduler.shutdownNow();
    }
}

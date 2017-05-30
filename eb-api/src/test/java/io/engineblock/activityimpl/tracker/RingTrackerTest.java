package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityapi.cycletracking.Tracker;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.*;

/**
 * Created by jshook on 5/26/17.
 */

@Test
public class RingTrackerTest {

    @Test(
            expectedExceptions = {RuntimeException.class},
            expectedExceptionsMessageRegExp = ".*attempted to mark cycle:3.*")
    public void testUnderMarkingError() {
        RingTracker rt = new RingTracker(13, 31, 7, 3);
        rt.markResult(3, 5);
    }

    @Test(
            expectedExceptions = {RuntimeException.class},
            expectedExceptionsMessageRegExp = "buffsize must be .*"
    )
    public void testSizeCheck() {
        RingTracker rt = new RingTracker(13, 31, 7, 4);
    }

    @Test
    public void testPerfectFit() {
        RingTracker rt = new RingTracker(3, 6, 9, 4);
        rt.markResult(3, 3);
        rt.markResult(4, 4);
        rt.markResult(5, 5);
        rt.markResult(6, 6);
        CycleSegment segment1 = rt.getSegment(4);
        assertThat(segment1.codes).isEqualTo(new byte[]{(byte) 3, (byte) 4, (byte) 5, (byte) 6});
        assertThat(segment1.cycle).isEqualTo(3L);
        rt.markResult(7, 7);
        rt.markResult(8, 8);
        rt.markResult(9, 9);
        rt.markResult(10, 10);
        CycleSegment segment2 = rt.getSegment(4);
        assertThat(segment2.codes).isEqualTo(new byte[]{(byte) 7, (byte) 8, (byte) 9, (byte) 10});
        assertThat(segment2.cycle).isEqualTo(7L);
    }

    @Test
    public void testMarkerBlocksForPending() {
        RingTracker rt = new RingTracker(3, 6, 9, 4);

        Marker marker = new Marker(rt);
        Thread mThread = new Thread(marker);
        mThread.setName("MarkerThread");

        Reader reader = new Reader(rt);
        Thread rThread = new Thread(reader);
        rThread.setName("ReaderThread");

        rThread.start();
        mThread.start();

        marker.q.offer(3);
        marker.q.offer(7);
        reader.strideQ.offer(2);

        boolean waited=await(cs -> cs.size()>=1,reader.segments,100,10000);
        assertThat(waited).isTrue();

        assertThat(reader.segments.get(0).cycle).isEqualTo(3);
        assertThat(reader.segments.get(0).codes).hasSize(2);


        reader.stop = true;
        marker.stop = true;
        try {
            rThread.join();
            mThread.join();
        } catch (Exception ignored) {
        }

    }

    private static boolean await(Predicate<List<CycleSegment>> predicate, List<CycleSegment> list, int pollInterval, int totalTimeout) {
            long start=System.currentTimeMillis();
            long end=start;
            while ((end-start)<totalTimeout) {
                if (predicate.test(list)) {
                    return true;
                }
                try {
                    Thread.sleep(pollInterval);
                } catch (InterruptedException ignored) {
                }
                end = System.currentTimeMillis();
            }
            return false;
    }

    private static class Reader implements Runnable {
        private final Tracker t;
        private volatile boolean stop = false;

        public LinkedBlockingQueue<Integer> strideQ = new LinkedBlockingQueue<>(1);
        public List<CycleSegment> segments = new ArrayList<>();

        public Reader(Tracker t) {
            this.t = t;
        }

        @Override
        public void run() {
            while (!stop) {
                try {
                    Integer stride = strideQ.poll(10, TimeUnit.SECONDS);
                    if (stride != null) {
                        CycleSegment segment = t.getSegment(stride);
                        segments.add(segment);
                    }
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    public static class Marker implements Runnable {
        private final Tracker t;
        public volatile boolean stop = false;

        public LinkedBlockingQueue<Integer> q = new LinkedBlockingQueue<>(1);

        public Marker(Tracker t) {
            this.t = t;
        }

        @Override
        public void run() {
            while (!stop) {
                Integer value = null;
                try {
                    value = q.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                if (value != null) {
                    t.markResult(value, value);
                }
            }
        }


    }
}
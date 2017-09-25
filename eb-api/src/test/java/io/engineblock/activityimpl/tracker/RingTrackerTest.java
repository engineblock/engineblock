package io.engineblock.activityimpl.tracker;

import io.engineblock.activityapi.cycletracking.CycleSegment;
import io.engineblock.activityapi.cycletracking.CycleSinkSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class RingTrackerTest {
    private static final Logger logger = LoggerFactory.getLogger("test");

    private static boolean await(
            Predicate<List<CycleSegment>> predicate,
            List<CycleSegment> list, int pollInterval, int totalTimeout) {
        long start = System.currentTimeMillis();
        long end = start;
        while ((end - start) < totalTimeout) {
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

    @Test(
            expectedExceptions = {RuntimeException.class},
            expectedExceptionsMessageRegExp = ".*attempted to mark cycle:3.*")
    public void testUnderMarkingError() {
        RingTracker rt = new RingTracker(13, 31, 7, 3);
        rt.consumeResult(3, 5);
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
        rt.consumeResult(3, 3);
        rt.consumeResult(4, 4);
        rt.consumeResult(5, 5);
        rt.consumeResult(6, 6);
        CycleSegment segment1 = rt.getSegment(4);
        assertThat(segment1.codes).isEqualTo(new byte[]{(byte) 3, (byte) 4, (byte) 5, (byte) 6});
        assertThat(segment1.cycle).isEqualTo(3L);
        rt.consumeResult(7, 7);
        rt.consumeResult(8, 8);
        rt.consumeResult(9, 9);
        rt.consumeResult(10, 10);
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

        reader.q.offer(2);
        marker.q.offer(3);
        marker.q.offer(7);

        reader.q.offer(0);
        marker.q.offer(0);

        boolean waited = await(cs -> cs.size() >= 1, reader.segments, 100, 10000);
        assertThat(waited).isTrue();

        assertThat(reader.segments.get(0).cycle).isEqualTo(3);
        assertThat(reader.segments.get(0).codes).hasSize(2);
        assertThat(reader.segments.get(0).codes[0]).isEqualTo((byte)3);
        assertThat(reader.segments.get(0).codes[1]).isEqualTo((byte)0);

//
//        reader.stop = true;
//        marker.stop = true;
        try {
            rThread.join();
            mThread.join();
        } catch (Exception ignored) {
        }

    }

    private static class Reader implements Runnable {
        private final CycleSinkSource t;

        public LinkedBlockingQueue<Integer> q = new LinkedBlockingQueue<>(1);
        public List<CycleSegment> segments = new ArrayList<>();

        public Reader(CycleSinkSource t) {
            this.t = t;
        }

        @Override
        public void run() {
            try {
                logger.debug("test reader awaiting stride data, 0 means stop");
                Integer stride = q.poll(10, TimeUnit.SECONDS);
                logger.debug ("reading stride==" + stride);
                if (stride != null) {
                    if (stride == 0) {
                        logger.debug("read stride value 0, stopping test reader");
                        return;
                    }
                    CycleSegment segment = t.getSegment(stride);
                    segments.add(segment);
                } else {
                    throw new RuntimeException("Test error: Waiting too long for input.");
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    public static class Marker implements Runnable {
        private final CycleSinkSource t;

        public LinkedBlockingQueue<Integer> q = new LinkedBlockingQueue<>(1);

        public Marker(CycleSinkSource t) {
            this.t = t;
        }

        @Override
        public void run() {
            Integer value = null;
            try {
                logger.debug("test marker awaiting cycle data, 0 means stop");
                value = q.poll(10, TimeUnit.SECONDS);
                logger.debug ("marking cycle=" + value + " + status=" + value);
                if (value != null) {
                    if (value == 0) {
                        logger.debug("read marker value 0, stopping test marker");
                        return;
                    }
                    t.consumeResult(value, value);
                } else {
                    throw new RuntimeException("Test error: waited too long for input: 10s");
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

}
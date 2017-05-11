package io.engineblock.activityimpl.tracking;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class LongTreeTrackerTest {

//    @Test
//    public void testCoMask() {
//        LongTreeTracker t = new LongTreeTracker();
//        assertThat(t.comaskOfBit(0b1000L)).isEqualTo(0b1100L);
//        assertThat(t.comaskOfBit(0b0100L)).isEqualTo(0b1100L);
//        assertThat(t.comaskOfBit(1L<<61)).isEqualTo(1L<<61|1L<<60);
//    }

//    @Test
//    public void testParentBit() {
//        LongTreeTracker t = new LongTreeTracker();
//        assertThat(t.parentOf(0)).isEqualTo(1);
//        assertThat(t.parentOf(1)).isEqualTo(1);
//        assertThat(t.parentOf(2)).isEqualTo(2);
//        assertThat(t.parentOf(23)).isEqualTo(11);
//    }

    @Test
    public void testApply() {
        LongTreeTracker t = new LongTreeTracker(0L);
        t.applyPosition(0);
        System.out.println(t);
        t.applyPosition(1);
        System.out.println(t);
        t.applyPosition(2);
        System.out.println(t);
        t.applyPosition(5);
        System.out.println(t);
        t.applyPosition(6);
        System.out.println(t);
        t.applyPosition(3);
        System.out.println(t);
        t.applyPosition(4);
        System.out.println(t);
        t.applyPosition(7);
        System.out.println(t);
    }

    @Test
    public void testFullCycle() {
        LongTreeTracker t = new LongTreeTracker();
        for (int i = 0; i < 32 ; i++) {
            t.applyPosition(i);
        }
        System.out.println(t);
        assertThat(t.getImage()).isEqualTo(-2L);
    }

    @Test
    public void testCompletionCheck() {
        LongTreeTracker t1 = new LongTreeTracker(0);
        assertThat(t1.completed(0)).isFalse();
        t1.applyPosition(3);
        assertThat(t1.completed(0)).isFalse();
        t1.applyPosition(2);
        assertThat(t1.completed(0)).isFalse();
        t1.applyPosition(1);
        assertThat(t1.completed(0)).isFalse();
        t1.applyPosition(0);
        assertThat(t1.completed(0)).isTrue();

    }

    @Test
    public void testBitString() {
        LongTreeTracker t = new LongTreeTracker(2L);
        System.out.println(t);
    }

    /**
     * Last result on a mobile i7 CPU:
     * <pre>
     *  count: 1073741824
     *  duration ms: 13730.785213
     *  rate/ms: 78199.593639
     *  rate/s: 78199593.638928
     * </pre>
     */
    @Test
    public void speedcheck() {
//        long t1=System.nanoTime();
//        LongTreeTracker t = new LongTreeTracker();
//        int count=1024*1024*1024;
//        for(int i=0;i<count;i++) {
//            int j = i % 32;
//            t.applyPosition(j);
//        }
//
//        long t2=System.nanoTime();
//        double duration = ((double) t2 - (double) t1)/1000000.0d;
//        double rate = ((double) count) / duration;
//        System.out.format("count: %d\n",count);
//        System.out.format("duration ms: %f\n", duration);
//        System.out.format("rate/ms: %f\n", rate);
//        System.out.format("rate/s: %f\n", rate * 1000.0d);
    }
}
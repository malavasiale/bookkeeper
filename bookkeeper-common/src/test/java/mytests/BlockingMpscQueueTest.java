package mytests;

import static org.junit.Assert.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.common.collections.BlockingMpscQueue;
import org.jctools.util.Pow2;
import org.junit.BeforeClass;
import org.junit.Test;


public class BlockingMpscQueueTest{
	
	static BlockingQueue<Integer> queue;
	static int size = 13;
	
	
	@BeforeClass
	public static void init() {
		size = Pow2.roundToPowerOfTwo(size);
		queue = new BlockingMpscQueue<>(size);
	}
	
	
    @Test
    public void testOffer() throws Exception {
        for (int i = 0; i < size; i++) {
            assertTrue(queue.offer(1, 100, TimeUnit.MILLISECONDS));
        }

        assertFalse(queue.offer(1, 100, TimeUnit.MILLISECONDS));
        
    }
	

}

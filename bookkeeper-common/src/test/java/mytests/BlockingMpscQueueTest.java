package mytests;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.common.collections.BlockingMpscQueue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

public class BlockingMpscQueueTest{
	
	@InjectMocks
	BlockingMpscQueue<Integer> queue = new BlockingMpscQueue<>(3);

	
	@SuppressWarnings("unchecked")
	@Before
	public void initialize() {
		queue = Mockito.spy(BlockingMpscQueue.class);
		Mockito.when(queue.relaxedOffer(2)).thenReturn(true);
	}

	@Test
	public void offerTest() {
		try {
			boolean result = queue.offer(2, 1000, TimeUnit.SECONDS);
			assertEquals(result,true);
			//System.out.println("The result is" + result);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

package mytests;

import static org.junit.Assert.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.common.collections.BlockingMpscQueue;
import org.jctools.util.Pow2;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(JUnitParamsRunner.class)
public class BlockingMpscQueueTest{
	
	static BlockingQueue<Integer> queue;
	static int size = 7;
	
	
	/*
	 * Inizializzo la coda.
	 * ATTENZIONE : dalla documentazione e dal codice notiamo che il costruttore padre di BlockingMpscQueue<T>()
	 * 				costruisce la size della coda con la più vicina potenza di due rispetto alla size inserita! 
	 * */
	@Before
	public static void init() {
		queue = new BlockingMpscQueue<>(size);
	}
	
	
    @Test
    @Parameters({
        "1,100,MILLISECONDS",
        "0,100,MILLISECONDS",
        "-1,100,MILLISECONDS"
    })
    public void testOffer(int toAdd,long timeout,TimeUnit unit) throws Exception {
    	int realSize = Pow2.roundToPowerOfTwo(size);
    	
    	/*
    	 * Riempo la coda fino alla sua dimensione massima e mi aspetto che ritorni ogni volta true
    	 * */
        for (int i = 0; i < realSize; i++) {
            assertTrue(queue.offer(toAdd, timeout, unit));
        }

        /*
         * Provo ad inserire un altro elemento e mi aspetto che ritorni false
         * */
        assertFalse(queue.offer(toAdd,timeout,unit));
        
    }
	

}

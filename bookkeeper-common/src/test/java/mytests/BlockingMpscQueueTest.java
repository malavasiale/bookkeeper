package mytests;

import static org.junit.Assert.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.common.collections.BlockingMpscQueue;
import org.jctools.util.Pow2;
import org.junit.Assert;
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
	 * Inizializzo la coda prima di ogni test
	 * ATTENZIONE : dalla documentazione e dal codice notiamo che il costruttore padre di BlockingMpscQueue<T>()
	 * 				costruisce la size della coda con la più vicina potenza di due rispetto alla size inserita! 
	 * */
	@Before
	public static void init() {
		queue = new BlockingMpscQueue<>(size);
	}
	
	
	/*
	 * Category partition:
	 * 1. int toAdd : intero da inserire in una lista : { < 0 ; 0 ; > 0}
	 * 2. long  timeout : long da utilizzare come timeout : {< 0 ; 0 ; > 0}
	 * 3. TimeUnit unit : enumerazione che indica l'unità di misura : {SECONDS;NANOSECONDS 
	 * 		;MINUTES;MILLISECONDS;MICROSECONDS;HOURS;DAYS}
	 * ----------------------------
	 * Approccio unidimensionale : ogni input almeno una volta altrimenti tutte le combinazioni con l'enum
	 * sarebbero troppe. Oltreutto non posso mettere un timeout di giorni per un test!
	 * */
    @Test
    @Parameters({
        "1,100,MILLISECONDS",
        "0,1,SECONDS",
        "-1,100,NANOSECONDS",
        "1,0,MINUTES",
        "1,100,MICROSECONDS",
        "0,-1,HOURS",
        "0,-1,DAYS"
    })
    public void testOfferAndPoll(int toAdd,long timeout,TimeUnit unit) throws Exception {
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
        
        /*
         * Provo a svuotare la coda e mi aspetto che tirorni sempre un intero
         * */
        for(int i = 0;i < realSize;i++) {
        	Assert.assertEquals(Integer.valueOf(toAdd),queue.poll(timeout,unit));
        }
        
        /*
         * Provo a prendere un altro elemento dalla lista e mi aspetto che sia null
         * */
        Assert.assertNull(queue.poll(timeout,unit));
        
    }
	

}

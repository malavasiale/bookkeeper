package mytests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.common.collections.BlockingMpscQueue;
import org.jctools.util.Pow2;
import org.junit.Assert;
import org.junit.Before;
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
    
    /*
     * Test per verificare il lancio di InterruptedException in caso il Thread
     * sia stato interrotto.
     * */
    @Test
    public void testInterrupt() throws InterruptedException {
    	Thread.currentThread().interrupt();
    	int realSize = Pow2.roundToPowerOfTwo(size);
    	boolean t = false;
    	
    	/*
    	 * Controllo eccezione in caso di interrupt e poll da una lista vuota
    	 * */
    	try {
    		queue.poll(0,TimeUnit.DAYS);
    	} catch (Exception e) {
    		t = true;
    	}
    	assertTrue(t);
    	t = false;
    	
    	
    	/*
    	 * Controllo eccezione in caso di interrupt e offer su una lista piena.
    	 * */
    	for(int i = 0; i < realSize; i++) {
    		queue.offer(0, 0, TimeUnit.DAYS);
    	}
    	
    	Thread.currentThread().interrupt();
    	try {
    		queue.offer(0, 1, TimeUnit.DAYS);
    	} catch (InterruptedException e) {
    		t = true;
    	}
    	assertTrue(t);
    }
    
    /*
     * Test simile a testOfferAndPoll() ma in questo caso non hanno timeout. In questo caso però
     * non hanno nemmeno una valore di ritorno, di conseguenza testerò la lunghezza della coda
     * per verificare il corretto funzionamento.
     * */
    @Test
    @Parameters({
    	"0,1",
    	"1,2",
    	"-1,3"
    })
    public void testTakeAndPut(int toAdd,int howMany) throws InterruptedException {
    	
    	/*
    	 * Riempo la coda e controllo che la sua dimensione sia corretta
    	 * */
    	for(int i = 0; i < howMany ; i++) {
    		queue.put(toAdd);
    	}
    	assertEquals(howMany,queue.size());
    	
    	/*
    	 * Svuoto la coda e controllo che la d
    	 * */
    	for(int i = 0;i < howMany; i++) {
    		queue.take();
    	}
    	assertEquals(0,queue.size());
    	
    }
    
    /*
     * Test per il metodo drainTo(Collection c) che serve a trasferire dalla coda gli elementi in una lista.
     * Se la lista non è abbastanza grande arriva a riempirla. Il valore di ritorno è la dimensione della lista
     * al termine del metodo meno la sua dimensione iniziale.
     * Category partition :
     * Collection<T> c : {lista vuota, lista con qualche elemento , lista piena , null}
     * */
    @Test
    public void testDrainTo() throws InterruptedException {
    	int realSize = Pow2.roundToPowerOfTwo(size);
    	
    	/*Drain su lista vuota*/
    	List<Integer> drain = new ArrayList<>();
    	for(int i = 0 ; i < realSize ; i++ ) {
    		queue.put(0);
    	}
    	int addedToList = queue.drainTo(drain);
    	assertEquals(realSize,addedToList);
    }
    
}
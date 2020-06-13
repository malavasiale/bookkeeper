package mytests;

import static org.junit.Assert.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.bookkeeper.common.collections.BlockingMpscQueue;
import org.jctools.util.Pow2;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest(BlockingMpscQueue.class)
public class BlockingMpscQueueInterruptedTest {
	
	static BlockingQueue<Integer> queue;
	static int size = 7;
	
	
	/*
	 * Inizializzo la coda prima di ogni test
	 * ATTENZIONE : dalla documentazione e dal codice notiamo che il costruttore padre di BlockingMpscQueue<T>()
	 * 				costruisce la size della coda con la più vicina potenza di due rispetto alla size inserita! 
	 * */
	@Before
	public void init() {
		queue = new BlockingMpscQueue<>(size);
	}

	/*
	 * Test dove eseguo un mock della chiamata Thread.interrupted() per testare la capacità
	 * dei metodi nel fare il throw dell' InterruptedException.
	 * Mi serviva anche per completare la coverage dei metodi.
	 * */
	@Test
	public void testOfferAndPollInterrupted() throws InterruptedException {
	    boolean thrown = false;
	    int realSize = Pow2.roundToPowerOfTwo(size);
	    
	    /*Spy della chiamata a Thread.interrupted()*/
		PowerMockito.spy(Thread.class);
		PowerMockito.when(Thread.interrupted()).thenReturn(true);
		
		/*
		 * Provo ad estrarre un elemento dalla coda vuota senza far scadere il timeout
		 * e interrompendo il Thread. Mi aspetto che venga lanciata l'eccezione.
		 * */
		try {
	    	queue.poll(5, TimeUnit.SECONDS);
	     	} catch (InterruptedException e) {
	     		thrown = true;
	     		}
	    assertEquals(true,thrown);
	    thrown = false;
	    
		/*Riempo la coda*/
		for(int i = 0; i < realSize ; i++) {
			queue.offer(1,5, TimeUnit.SECONDS);
		}
		
		/*
		 * Provo a inserire un elemento nella coda piena senza far scadere il timeout
		 * e interrompendo il thread. Mi aspetto che venga lanciata l'eccezione.
		 * */
		try {
			queue.offer(1,5, TimeUnit.SECONDS);
		}catch(InterruptedException e){
			thrown = true;
		}
		assertEquals(true,thrown);
	}
		
}

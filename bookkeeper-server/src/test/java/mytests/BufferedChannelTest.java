package mytests;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;

import org.apache.bookkeeper.bookie.BufferedChannel;

/*
 * MUTATION 1-266 equivalente al SUT. Cambiando la condizione di buondary l'interno del blocco if rimane
 * irragiungibile come anche mostrato dalla coverage di jacoco. Infatti fileChannel.read(...) non ritorna 
 * mai un numero di bytes <= 0 poichè il parametro readBufferStartPosition o è valido, o altrimenti se invalido
 * viene lanciata una InvalidArgumentException
 * */
@RunWith(JUnitParamsRunner.class)
public class BufferedChannelTest {
	
	private BufferedChannel bufferedChannel;
	private ByteBuf buffer;
	
	/*
	 * Test per controllare la corretta scrittura nel buffer e il corretto flush
	 * una volta raggiunto il limite massimo consentito di byte non persistenti(unpersistedBytesBound)
	 * o la capacità massima.
	 * Category partition :
	 * 1. capacity: {<= 0 ; > 0}
	 * 2. unpersistedBytesBound : {<= 0 ;0 < x <= capacity ; > capacity}
	 * 3. lenght : {<= 0 ; = capacity && = unpersistedBytesBound ; < unpersistedBytesBound && < capacity ;
	 * 			unpersistedBytesBound < x < capacity ; capacity < x < unpersistedBytesBound ;
	 * 			 > capacity && > unpersistedBytesBound} 
	 * Metodo unidimensionale
	 * */
	//@Test(timeout = 5000)
	@Parameters({
        "40,40,40,0,40", // capacity > 0 ; Bound <= capacity : length = capacity && length = unpersistedBytesBound
		"40,40,30,30,0", // capacity > 0 ; Bound <= capacity : length < capacity && length < unpersistedBytesBound
		"40,41,30,30,0", // capacity > 0 ; Bound > capacity : length < capacity && length < unpersistedBytesBound
		"40,30,35,0,35", // capacity > 0 ; Bound <= capacity ; Bound < length < capacity
		"40,50,45,5,40", // capacity > 0 ; Bound > capacity ; capacity < length < Bound
		"40,10,51,0,51", // capacity > 0 ; Bound <= capacity ; length > capacity && length > Bound
		"40,0,10,10,0", //  capacity > 0 ; Bound <= 0; Bound < length < capacity
		//"0,0,1,0,0", //  capacity <= 0 ; Bound <= 0; Bound < length < capacity ----> BUG con capacità nulla
		"40,10,0,0,0" //  capacity > 0 ; Bound <= capacity; length <= 0		
    })
	public void testWrite(int capacity,long unpersistedBytesBound,int length, int expectedInBuffer,int expectedInFile) throws Exception {
		
		bufferedChannel = createBuffer(capacity,unpersistedBytesBound);
		buffer = generateEntry(length);
		bufferedChannel.write(buffer);
		
		assertEquals(expectedInBuffer,bufferedChannel.getNumOfBytesInWriteBuffer());
		assertEquals(expectedInFile,bufferedChannel.size());
	}
	
	/*
	 * MUTATION 126 KILLED
	 * Controlla la posizione a cui arriva il writeBufferIndex dopo le scritture. La mutazione sostituiva
	 * un incremento del puntatore con un decremento e con una singola scrittura non era individuabile.
	 * */
	@Test
	public void killMutation126() throws Exception {
		bufferedChannel = createBuffer(20,0);
		buffer = generateEntry(10);
		bufferedChannel.write(buffer);
		
		assertEquals(10,bufferedChannel.position());
		
		buffer = generateEntry(5);
		bufferedChannel.write(buffer);
		assertEquals(15,bufferedChannel.position());
	}
	
	/*
	 * MUTATION 129 KILLED
	 * Scrivo esattamente il numero di bytes pary all' unpersistedBytesBound e mi aspetto il flush sul file.
	 * La mutazione sostituisce il >= con un > e quindi non avviene il flush sul file. Questo metodo controlla
	 * questo caso particolare.
	 * */
	@Test
	public void killMutation129() throws Exception {
		bufferedChannel = createBuffer(20,10);
		buffer = generateEntry(10);
		bufferedChannel.write(buffer);
		
		assertEquals(0,bufferedChannel.getNumOfBytesInWriteBuffer());
	}
	
	/*
	 * MUTATION 135 KILLED
	 * Scrivo più bytes di unpersistedBytesBound e la forceWrite() dovrebbe aggiornare il numero di
	 * unpersistedBytes rimasti. La mutazione non fa eseguire la forceWrite() quindi il test fallisce.
	 * */
	@Test
	public void killMutation135() throws Exception {
		bufferedChannel = createBuffer(20,10);
		buffer = generateEntry(20);
		bufferedChannel.write(buffer);
		
		assertEquals(0,bufferedChannel.getUnpersistedBytes());
	}
	
	
	/*
	 * Test per verificare la lettura dal Channel se ci sono bytes presenti.
	 * Category Partition:
	 * 1. int maxLen : {> 0; <= 0}
	 * 2. int pos : {<0 ; 0 ; 0 < x <= maxLen ; > maxLen}
	 * 3. int length : {<0 ; 0 ; 0 < x <= |maxLen-pos| ; > |maxLen-pos|}
	 * Metodo unidimensionale
	 * */
	@Test
	@Parameters({
		"40,0,20,20,false", //maxLen > 0; pos = 0 ; 0 < length <= |maxLen-pos|
		"40,30,20,0,true", //maxLen > 0; 0 < pos <= maxLen ; length > |maxLen-pos|
		"40,10,0,0,false", // maxLen > 0 ; 0 < pos <= maxLen ; length = 0
		"20,30,10,10,false", // maxLen > 0 ; pos > maxLen ; 0 < length <= |maxLen-pos|
		"40,-1,10,0,true", // maxLen > 0 ; pos < 0 ; 0 < length <= |maxLen-pos|
		"40,20,30,0,true",  // maxLen > 0 ; 0 < pos <= maxLen ; length > |maxLen-pos|
		"40,20,-1,0,true", // maxLen > 0 ; 0 < pos <= maxLen ; length < 0
		"40,50,1,0,true", // maxLen > 0 ; pos > maxLen ; 0 < length <= |maxLen-pos|  FOR COVERAGE
		"50,0,20,20,false", // maxLen > 0 ; 0 < pos <= maxLen; 0 < length <= |maxLen-pos|  FOR COVERAGE
		"30,0,200,0,true", // maxLen > 0 ; pos > maxLen ; length > |maxLen-pos|  FOR COVERAGE
		"30,30,10,10,false" // maxLen > 0 ; 0 < pos <= maxLen ; 0 < length <= |maxLen-pos|  MUTATION 240 KILLED 
							// la writeBufferStartPosition non è zero, quindi viene rilevato il + al posto del -
	})
	public void testRead(int maxLen,int pos, int length,Integer expectedResult,boolean exception) throws Exception {
		Integer result;
		bufferedChannel = createBuffer(maxLen,0); // Lo creo in modo che non faccia flush()
		buffer = generateEntry(40);
		bufferedChannel.write(buffer);
		
		try {
			ByteBuf dest = Unpooled.buffer(length); //Creo un buffer dest
			result = bufferedChannel.read(dest, pos, length);
		} catch(IOException e1) {
			assertTrue(exception);
			return;
		}catch(IllegalArgumentException e2) {
			assertTrue(exception);
			return;
		}
		assertEquals(expectedResult,result);
		
	}
	
	/*
	 * MUTATION 256 KILLED : doppia lettura e buffer non limitato cosi da non prendere l'addizione in 
	 * Math.min() come minimo ma la lunghezza del buffer stesso (che è zero perche dinamico). La prima lettura serve
	 * per cambiare readBufferStartPosition (all'inizio uguale a zero) ed entrare nel blocco di codice,
	 *  la seconda uccide la mutazione.
	 * */
	@Test
	public void killMutation256() throws Exception {
		int result;
		bufferedChannel = createBuffer(30,10); // Lo creo in modo che non faccia flush()
		buffer = generateEntry(40);
		bufferedChannel.write(buffer);
		
		try {
			ByteBuf dest = Unpooled.buffer(); //Creo un buffer dest
			result = bufferedChannel.read(dest, 5, 1);
		} catch(IOException e1) {
			assertTrue(false);
			return;
		}catch(IllegalArgumentException e2) {
			assertTrue(false);
			return;
		}
		assertEquals(30,result);
		
		
		try {
			ByteBuf dest = Unpooled.buffer(); //Creo un buffer dest
			result = bufferedChannel.read(dest, 6, 1);
		} catch(IOException e1) {
			assertTrue(false);
			return;
		}catch(IllegalArgumentException e2) {
			assertTrue(false);
			return;
		}
		assertEquals(29,result);
	}
	
	@Test
	public void testNullWriteBuffer() throws IOException {
		
		ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
		File newLogFile = File.createTempFile("test", "log");
		newLogFile.deleteOnExit();
		
		FileChannel fileChannel = new RandomAccessFile(newLogFile, "rw").getChannel();
		allocator = Mockito.spy(ByteBufAllocator.class);
		Mockito.when(allocator.directBuffer(10)).thenReturn(null);
		
		BufferedChannel bufferedChannel = new BufferedChannel(allocator, fileChannel,
				10, 0);
		
		boolean t = false;
		ByteBuf dest = Unpooled.buffer(10); //Creo un buffer dest
		int result = bufferedChannel.read(dest, 0, 10);
		assertEquals(0,result);
		
		try {
			result = bufferedChannel.read(dest, -1, 10);
		} catch (IllegalArgumentException e) {
			t = true;
		}

		assertTrue(t);
	}


	public static BufferedChannel createBuffer(int capacity,  long unpersistedBytesBound) throws Exception {

		ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
		File newLogFile = File.createTempFile("test", "log");
		newLogFile.deleteOnExit();
		FileChannel fileChannel = new RandomAccessFile(newLogFile, "rw").getChannel();

		BufferedChannel bufferedChannel = new BufferedChannel(allocator, fileChannel,
				capacity, unpersistedBytesBound);

		return bufferedChannel;
	}

	private static ByteBuf generateEntry(int length) {
		Random random = new Random();
		byte[] data = new byte[length];
		random.nextBytes(data);
		ByteBuf byteBuffer = Unpooled.buffer(length);
		byteBuffer.writeBytes(data);
		return byteBuffer;
	}

}  


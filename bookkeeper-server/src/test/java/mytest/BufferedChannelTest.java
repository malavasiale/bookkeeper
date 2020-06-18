package mytest;


import static org.junit.Assert.assertEquals;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.model.TestTimedOutException;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import junitparams.JUnitParamsRunner;

import org.apache.bookkeeper.bookie.BufferedChannel;


@RunWith(JUnitParamsRunner.class)
public class BufferedChannelTest {
	
	private BufferedChannel bufferedChannel;
	private ByteBuf buffer;

	@After
	public void close() throws IOException {
		bufferedChannel.close();
	}
	
	
	/*
	 * Test per controllare la corretta scrittura nel buffer e il corretto flush
	 * una volta raggiunto il limite massimo consentito di byte non persistenti(unpersistedBytesBound)
	 * o la capacità massima.
	 * Category partition :
	 * capacity: {<= 0 ; > 0}
	 * unpersistedBytesBound : {<= 0 ;0 < x <= capacity ; > capacity}
	 * lenght : {<= 0 ; = capacity && = unpersistedBytesBound ; < unpersistedBytesBound && < capacity ;
	 * 			unpersistedBytesBound < x < capacity ; capacity < x < unpersistedBytesBound ;
	 * 			 > capacity && > unpersistedBytesBound} 
	 * */
	@Test(timeout = 5000)
	@Parameters({
        "40,40,40", // capacity > 0 ; Bound <= capacity : length = capacity && length = unpersistedBytesBound
		"40,40,30", // capacity > 0 ; Bound <= capacity : length < capacity && length < unpersistedBytesBound
		"40,41,30", // capacity > 0 ; Bound > capacity : length < capacity && length < unpersistedBytesBound
		"40,30,35", // capacity > 0 ; Bound <= capacity ; Bound < length < capacity
		"40,50,45", // capacity > 0 ; Bound > capacity ; capacity < length < Bound
		"40,10,51", // capacity > 0 ; Bound <= capacity ; length > capacity && length > Bound
		"40,0,10", //  capacity > 0 ; Bound <= 0; Bound < length < capacity
		"0,0,1", //  capacity <= 0 ; Bound <= 0; Bound < length < capacity ----> BUG con capacità nulla
		"40,10,0" //  capacity > 0 ; Bound <= capacity; length <= 0
		
    })
	public void testWrite(int capacity,long unpersistedBytesBound,int length) throws Exception {
		
		bufferedChannel = createBuffer(capacity,unpersistedBytesBound);
		buffer = generateEntry(length);
		bufferedChannel.write(buffer);
		
		
		//Flush in memoria
		if(length >= unpersistedBytesBound && length < capacity) {
			
			//Se è <= 0 allora non c'è limite di Byte non persistenti
			if(unpersistedBytesBound == 0) {
				assertEquals(length,bufferedChannel.getNumOfBytesInWriteBuffer());
				assertEquals(0,bufferedChannel.size());
			}
			//Se diverso da 0 allora viene fatto il flush
			else {
				assertEquals(0,bufferedChannel.getNumOfBytesInWriteBuffer());
				assertEquals(length,bufferedChannel.size());
			}
			return;
		}
		
		//Bytes rimangono nel buffer
		if(length < unpersistedBytesBound && length < capacity) {
			assertEquals(length,bufferedChannel.getNumOfBytesInWriteBuffer());
			assertEquals(0,bufferedChannel.size());
			return;
		}
		
		//Bytes che voglio scrivere sono più della lunghezza del buffer
		if(length > capacity) {
			int overflow = length % capacity;
			int flushed = length - overflow;
			if(overflow < unpersistedBytesBound) {
				assertEquals(overflow,bufferedChannel.getNumOfBytesInWriteBuffer());
				assertEquals(flushed,bufferedChannel.size());
			}
			else {
				assertEquals(0,bufferedChannel.getNumOfBytesInWriteBuffer());
				assertEquals(length,bufferedChannel.size());
			}
			return;
		}
	}
	
	/*
	 * Test per verificare la lettura dal Channel se ci sono bytes presenti.
	 * Category Partition:
	 * 1. ByteBuf dest : {valido , con capacità minore dei byte che voglio leggere , null}
	 * 2. int pos : {<0 ; 0 ; 0 < x < maxLen ; > maxLen}
	 * 3. int length : {<0 ; 0 ; 0 < x < maxLen ; > maxLen}
	 * */
	@Test
	public void testRead() throws Exception {
		
		bufferedChannel = createBuffer(40,0);
		buffer = generateEntry(20);
		bufferedChannel.write(buffer);
		ByteBuf dest = Unpooled.buffer();
		assertEquals(0,dest.readableBytes());
		
		assertEquals(20,bufferedChannel.read(dest, 0, 20));
		assertEquals(20,dest.readableBytes());
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


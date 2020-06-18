package mytest;


import static org.junit.Assert.assertEquals;


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Random;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
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
	 * capacity: {< 0 ; 0 ; > 0}
	 * unpersistedBytesBound : {<= 0 ;0 < x <= capacity ; > capacity}
	 * lenght : {< 0 ; 0 ; = capacity && = unpersistedBytesBound ; < unpersistedBytesBound && < capacity ;
	 * 			unpersistedBytesBound < x < capacity ; capacity < x < unpersistedBytesBound} 
	 * */
	@Test
	@Parameters({
        "40,40,40", // capacity > 0 ; Bound <= capacity : length = capacity
		"40,40,30", // capacity > 0 ; Bound <= capacity : length < capacity && length < unpersistedBytesBound
		"40,41,30" // capacity > 0 ; Bound > capacity : length < capacity && length < unpersistedBytesBound
    })
	public void testWrite(int capacity,long unpersistedBytesBound,int length) throws Exception {
		bufferedChannel = createBuffer(capacity,unpersistedBytesBound);
		buffer = generateEntry(length);
		bufferedChannel.write(buffer);
		
		//Flush in memoria
		if(length >= unpersistedBytesBound) {
			assertEquals(0,bufferedChannel.getNumOfBytesInWriteBuffer());
			assertEquals(length,bufferedChannel.size());
		}
		//Bytes rimangono nel buffer
		else if(length < unpersistedBytesBound) {
			assertEquals(length,bufferedChannel.getNumOfBytesInWriteBuffer());
			assertEquals(0,bufferedChannel.size());
		}
		
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


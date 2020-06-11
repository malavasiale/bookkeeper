package mytests;

import static org.junit.Assert.*;

import org.apache.bookkeeper.common.util.MathUtils;
import org.junit.Test;


/*
 * Class test for MathUtils
 * */
public class MathUtilsTest {

	/*
	 * Test for 
	 * */
	@Test
	public void test() {
		int mod = MathUtils.signSafeMod(10, 2);
		assertEquals(0, mod);
	}

}

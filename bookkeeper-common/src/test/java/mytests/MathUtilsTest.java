package mytests;

import static org.junit.Assert.*;

import org.apache.bookkeeper.common.util.MathUtils;
import org.junit.Test;


/*
 * Class test for MathUtils
 * */
public class MathUtilsTest {

	/*
	 * First example test for MathUtils
	 * */
	@Test
	public void test1() {
		int mod = MathUtils.signSafeMod(10, 2);
		assertEquals(0, mod);
	}
	
	@Test
	public void test2() {
		int mod = MathUtils.signSafeMod(-11, 2);
		assertEquals(1, mod);
	}
	
	@Test(expected = Exception.class)
	public void test3() {
		MathUtils.signSafeMod(20, 0);
	}
}

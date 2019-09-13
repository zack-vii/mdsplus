package w7x;

import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class W7XSignalAccess_Test{
	static HashMap<String, W7XSignalAccess> DBL = new HashMap<String, W7XSignalAccess>();

	@AfterClass
	public static final void afterClass() throws Exception {/**/}

	@BeforeClass
	public static final void beforeClass() throws Exception {
		for(final String db : W7XSignalAccess.getDataBaseList()){
			final W7XSignalAccess sa = W7XSignalAccess.getAccess(db);
			if(sa == null) break;
			W7XSignalAccess_Test.DBL.put(db, sa);
		}
	}

	@After
	public void after() throws Exception {/**/}

	@Before
	public void before() throws Exception {/**/}

	@SuppressWarnings("static-method")
	@Test
	public void testDatabase() {
		Assert.assertEquals(W7XSignalAccess_Test.DBL.get("Test").database, "Test");
	}
}

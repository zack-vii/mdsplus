package w7x;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import w7x.W7XDataProvider.JsonReader;

public class W7XDataProvider_Test{
	static HashMap<String, W7XSignalAccess> DBL = new HashMap<String, W7XSignalAccess>();

	@AfterClass
	public static final void afterClass() throws Exception {/**/}

	@BeforeClass
	public static final void beforeClass() throws Exception {/**/
	}

	@After
	public void after() throws Exception {/**/}

	@Before
	public void before() throws Exception {/**/}

	@SuppressWarnings({"static-method", "unused"})
	@Test
	public void testJsonReader() throws IOException {
		final JsonReader jr;
		if(true){
			final URL url = new URL("http://archive-webapi.ipp-hgw.mpg.de/ArchiveDB/codac/W7X/CoDaStationDesc.106/DataModuleDesc.239_DATASTREAM/0/UB_B5/scaled/_signal.json?from=1519140613449424401&upto=1519140613481423401");
			jr = JsonReader.fromURL(url, 1519140613449424400L, 1, 0);
		}else{
			final File file = new File("C:\\git\\w7xjava\\javascope\\junit\\w7x\\_signal.json");
			@SuppressWarnings("resource")
			final InputStream is = new FileInputStream(file);
			try{
				jr = new JsonReader(is, 1519140613449424400L, 1, 0);
			}finally{
				try{
					is.close();
				}catch(final IOException e){
					e.printStackTrace();
				}
			}
		}
		Assert.assertEquals("UB_B5", jr.label);
		Assert.assertEquals("kV", jr.unit);
		Assert.assertEquals(32000, jr.sampleCount);
		Assert.assertEquals(1L, jr.dimensions[0]);
		Assert.assertEquals(27.729964, jr.values[0], 1e-6);
	}
}

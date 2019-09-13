package jscope.data.signal;

import java.util.Vector;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import jscope.data.signal.Signal.RegionDescriptor;
import jscope.data.signal.Signal.ResolutionManager;

public class Signal_Test{
	@AfterClass
	public static final void afterClass() throws Exception {/**/}

	@BeforeClass
	public static final void beforeClass() throws Exception {/**/ }

	private static void testRegions(final Vector<RegionDescriptor> regions, final double[] lowerBound, final double[] upperBound, final double[] resolution) {
		final double[] lowerBound_is = new double[regions.size()];
		final double[] upperBound_is = new double[lowerBound_is.length];
		final double[] resolution_is = new double[lowerBound_is.length];
		for(int i = 0; i < lowerBound_is.length; i++){
			final RegionDescriptor region = regions.get(i);
			lowerBound_is[i] = region.lowerBound;
			upperBound_is[i] = region.upperBound;
			resolution_is[i] = region.resolution;
		}
		Assert.assertArrayEquals(lowerBound, lowerBound_is, 1e-5);
		Assert.assertArrayEquals(upperBound, upperBound_is, 1e-5);
		Assert.assertArrayEquals(resolution, resolution_is, 1e-5);
	}

	@After
	public void after() throws Exception {/**/}

	@Before
	public void before() throws Exception {/**/}

	@SuppressWarnings("static-method")
	@Test
	public void testResolutionManager() {
		final ResolutionManager ResMan = new ResolutionManager();
		ResMan.addRegion(new RegionDescriptor(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, 100));
		ResMan.addRegion(new RegionDescriptor(0, 100, 200));
		ResMan.addRegion(new RegionDescriptor(100, 200, 200));
		ResMan.addRegion(new RegionDescriptor(50, 150, 300));
		Signal_Test.testRegions(ResMan.lowResRegions, new double[]{Double.NEGATIVE_INFINITY, 0, 50, 150, 200}, new double[]{0, 50, 150, 200, Double.POSITIVE_INFINITY}, new double[]{100, 200, 300, 200, 100});
		Signal_Test.testRegions(ResMan.getLowerResRegions(0, 200, 300), new double[]{0, 150}, new double[]{50, 200}, new double[]{300, 300});
	}
}

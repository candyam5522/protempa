package org.protempa;

import org.protempa.proposition.interval.ConstraintNetworkSegmentComparer;

/**
 * Compares the output of <code>Segment</code> with that of
 * <code>ConstraintNetwork</code>.
 * 
 * @author Andrew Post
 */
public class SegmentLength1PrimitiveParameterCompareTest extends
		ConstraintNetworkSegmentComparer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		seg = SegmentTestParameters.getLength1PrimitiveParameterSegment();
		super.setUp();
	}

}

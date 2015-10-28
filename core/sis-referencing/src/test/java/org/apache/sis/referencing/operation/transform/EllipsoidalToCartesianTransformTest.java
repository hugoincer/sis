/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.referencing.operation.transform;

import javax.measure.unit.SI;
import org.opengis.util.FactoryException;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.datum.Ellipsoid;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.GeneralDirectPosition;

import static java.lang.StrictMath.toRadians;

// Test dependencies
import org.opengis.test.ToleranceModifier;
import org.apache.sis.internal.referencing.provider.GeocentricTranslationTest;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.junit.Test;


/**
 * Tests {@link EllipsoidalToCartesianTransform}.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@DependsOn({
    CoordinateDomainTest.class,
    ContextualParametersTest.class
})
public final strictfp class EllipsoidalToCartesianTransformTest extends MathTransformTestCase {
    /**
     * Tests conversion of a single point from geographic to geocentric coordinates.
     * This test uses the example given in EPSG guidance note #7.
     * The point in WGS84 is 53°48'33.820"N, 02°07'46.380"E, 73.00 metres.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testGeographicToGeocentric() throws TransformException {
        transform = EllipsoidalToCartesianTransform.createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        isInverseTransformSupported = false;    // Geocentric to geographic is not the purpose of this test.
        validate();

        final double delta = toRadians(100.0 / 60) / 1852;          // Approximatively 100 metres
        derivativeDeltas = new double[] {delta, delta, 100};        // (Δλ, Δφ, Δh)
        tolerance = GeocentricTranslationTest.precision(2);         // Half the precision of target sample point
        verifyTransform(GeocentricTranslationTest.samplePoint(1),   // 53°48'33.820"N, 02°07'46.380"E, 73.00 metres
                        GeocentricTranslationTest.samplePoint(2));  // 3771793.968,  140253.342,  5124304.349 metres
    }

    /**
     * Tests conversion of a single point from geocentric to geographic coordinates.
     * This method uses the same point than {@link #testGeographicToGeocentric()}.
     *
     * @throws TransformException should never happen.
     */
    @Test
    public void testGeocentricToGeographic() throws TransformException {
        transform = EllipsoidalToCartesianTransform.createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true).inverse();
        isInverseTransformSupported = false;    // Geographic to geocentric is not the purpose of this test.
        validate();

        derivativeDeltas = new double[] {100, 100, 100};            // In metres
        tolerance  = GeocentricTranslationTest.precision(1);        // Required precision for (λ,φ)
        zTolerance = Formulas.LINEAR_TOLERANCE / 2;                 // Required precision for h
        zDimension = new int[] {2};                                 // Dimension of h where to apply zTolerance
        verifyTransform(GeocentricTranslationTest.samplePoint(2),   // X = 3771793.968,  Y = 140253.342,  Z = 5124304.349 metres
                        GeocentricTranslationTest.samplePoint(1));  // 53°48'33.820"N, 02°07'46.380"E, 73.00 metres
    }

    /**
     * Tests conversion of random points.
     *
     * @throws TransformException if a conversion failed.
     */
    @Test
    @DependsOnMethod({
        "testGeographicToGeocentric",
        "testGeocentricToGeographic"
    })
    public void testRandomPoints() throws TransformException {
        final double delta = toRadians(100.0 / 60) / 1852;          // Approximatively 100 metres
        derivativeDeltas  = new double[] {delta, delta, 100};       // (Δλ, Δφ, Δh)
        tolerance         = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        transform = EllipsoidalToCartesianTransform.createGeodeticConversion(CommonCRS.WGS84.ellipsoid(), true);
        verifyInDomain(CoordinateDomain.GEOGRAPHIC, 306954540);
    }

    /**
     * Tests conversion of a point on an imaginary planet with high excentricity.
     * The {@link EllipsoidalToCartesianTransform} may need to use an iterative method
     * for reaching the expected precision.
     *
     * @throws FactoryException if an error occurred while creating the transform.
     * @throws TransformException if a conversion failed.
     */
    @Test
    public void testHighExcentricity() throws TransformException, FactoryException {
        transform = new EllipsoidalToCartesianTransform(6000000, 4000000, SI.METRE, true)
                .createGeodeticConversion(DefaultFactories.forBuildin(MathTransformFactory.class));

        final double delta = toRadians(100.0 / 60) / 1852;
        derivativeDeltas  = new double[] {delta, delta, 100};
        tolerance         = Formulas.LINEAR_TOLERANCE;
        toleranceModifier = ToleranceModifier.PROJECTION;
        verifyInverse(40, 30, 10000);
    }

    /**
     * Executes the derivative test using the given ellipsoid.
     *
     * @param  ellipsoid The ellipsoid to use for the test.
     * @param  hasHeight {@code true} if geographic coordinates include an ellipsoidal height (i.e. are 3-D),
     *         or {@code false} if they are only 2-D.
     * @throws TransformException Should never happen.
     */
    private void testDerivative(final Ellipsoid ellipsoid, final boolean hasHeight) throws TransformException {
        transform = EllipsoidalToCartesianTransform.createGeodeticConversion(ellipsoid, hasHeight);
        DirectPosition point = hasHeight ? new GeneralDirectPosition(-10, 40, 200) : new DirectPosition2D(-10, 40);
        /*
         * Derivative of the direct transform.
         */
        tolerance = 1E-2;
        derivativeDeltas = new double[] {toRadians(1.0 / 60) / 1852}; // Approximatively one metre.
        verifyDerivative(point.getCoordinate());
        /*
         * Derivative of the inverse transform.
         */
        point = transform.transform(point, null);
        transform = transform.inverse();
        tolerance = 1E-8;
        derivativeDeltas = new double[] {1}; // Approximatively one metre.
        verifyDerivative(point.getCoordinate());
    }

    /**
     * Tests the {@link EllipsoidalToCartesianTransform#derivative(DirectPosition)} method on a sphere.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    public void testDerivativeOnSphere() throws TransformException {
        testDerivative(CommonCRS.SPHERE.ellipsoid(), true);
        testDerivative(CommonCRS.SPHERE.ellipsoid(), false);
    }

    /**
     * Tests the {@link EllipsoidalToCartesianTransform#derivative(DirectPosition)} method on an ellipsoid.
     *
     * @throws TransformException Should never happen.
     */
    @Test
    @DependsOnMethod("testDerivativeOnSphere")
    public void testDerivative() throws TransformException {
        testDerivative(CommonCRS.WGS84.ellipsoid(), true);
        testDerivative(CommonCRS.WGS84.ellipsoid(), false);
    }
}

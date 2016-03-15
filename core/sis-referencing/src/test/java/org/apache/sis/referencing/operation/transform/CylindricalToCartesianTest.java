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

import org.opengis.util.FactoryException;
import org.opengis.referencing.operation.TransformException;

import static java.lang.StrictMath.*;

// Test dependencies
import org.opengis.test.referencing.TransformTestCase;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestUtilities;
import org.junit.Test;


/**
 * Tests {@link CylindricalToCartesian}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
public final strictfp class CylindricalToCartesianTest extends TransformTestCase {
    /**
     * Returns coordinate points in spherical coordinates and their equivalent in Cartesian coordinates.
     */
    static double[][] testData() {
        final double z     = 20;
        final double r     = 1000;
        final double cos30 = r*sqrt(0.75);      // cos(30°) = √¾
        final double cos60 = r/2;               // cos(60°) =  ½
        final double sin60 = cos30;
        final double sin30 = cos60;
        return new double[][] {
            new double[] {                      // (r,θ,z) coordinates
                 0,       0,       0,
                 r,       0,       z,
                 r,      90,       z,
                 r,     -90,       z,
                 r,      60,       z,
                 r,      30,       z
            }, new double[] {                   // (x,y,z) coordinates
                 0,       0,       0,
                 r,       0,       z,
                 0,       r,       z,
                 0,      -r,       z,
                 cos60,   sin60,   z,
                 cos30,   sin30,   z
            }
        };
    }

    /**
     * Tests coordinate conversions.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    public void testConversion() throws FactoryException, TransformException {
        transform = CylindricalToCartesian.INSTANCE.completeTransform();
        tolerance = 1E-12;
        final double[][] data = testData();
        verifyTransform(data[0], data[1]);
    }

    /**
     * Tests calculation of a transform derivative.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    public void testDerivative() throws FactoryException, TransformException {
        transform = CylindricalToCartesian.INSTANCE.completeTransform();
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 1E-7;
        verifyDerivative(100, 60, 25);
    }

    /**
     * Tests calculation of a transform derivative.
     *
     * @throws FactoryException if the transform can not be created.
     * @throws TransformException if a coordinate can not be transformed.
     */
    @Test
    @DependsOnMethod({"testConversion", "testDerivative"})
    public void testConsistency() throws FactoryException, TransformException {
        transform = CylindricalToCartesian.INSTANCE.completeTransform();
        derivativeDeltas = new double[] {1E-6, 1E-6, 1E-6};
        tolerance = 1E-7;
        verifyInDomain(new double[] {  0, -180, -100},      // Minimal coordinates
                       new double[] {+20, +180, +100},      // Maximal coordinates
                       new int[]    { 10,   10,   10},
                       TestUtilities.createRandomNumberGenerator());
    }
}

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
package org.apache.sis.internal.referencing.provider;

import java.util.List;
import javax.measure.unit.SI;
import javax.measure.unit.NonSI;
import javax.xml.bind.annotation.XmlTransient;
import org.opengis.util.FactoryException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.Transformation;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.internal.referencing.WKTUtilities;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.internal.system.Loggers;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.measure.Units;
import org.apache.sis.parameter.Parameters;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.parameter.ParameterBuilder;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.transform.LinearTransform;
import org.apache.sis.util.logging.Logging;


/**
 * The base class of operation methods performing a translation, rotation and/or scale in geocentric coordinates.
 * Those methods may or may not include a Geographic/Geocentric conversion before the operation in geocentric domain,
 * depending on whether or not implementations extend the {@link GeocentricAffineBetweenGeographic} subclass.
 *
 * <div class="note"><b>Note on class name:</b>
 * the {@code GeocentricAffine} class name is chosen as a generalization of {@link GeocentricTranslation}.
 * "Geocentric translations" is an operation name defined by EPSG.</div>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
@XmlTransient
public abstract class GeocentricAffine extends AbstractProvider {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 8291967302538661639L;

    /**
     * The operation parameter descriptor for the <cite>X-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tX tX}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TX;

    /**
     * The operation parameter descriptor for the <cite>Y-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tY tY}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TY;

    /**
     * The operation parameter descriptor for the <cite>Z-axis translation</cite>
     * ({@linkplain BursaWolfParameters#tZ tZ}) parameter value. Valid values range
     * from negative to positive infinity. Units are {@linkplain SI#METRE metres}.
     */
    public static final ParameterDescriptor<Double> TZ;

    /**
     * The operation parameter descriptor for the <cite>X-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rX rX}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    static final ParameterDescriptor<Double> RX;

    /**
     * The operation parameter descriptor for the <cite>Y-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rY rY}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    static final ParameterDescriptor<Double> RY;

    /**
     * The operation parameter descriptor for the <cite>Z-axis rotation</cite>
     * ({@linkplain BursaWolfParameters#rZ rZ}) parameter value.
     * Units are {@linkplain NonSI#SECOND_ANGLE arc-seconds}.
     */
    static final ParameterDescriptor<Double> RZ;

    /**
     * The operation parameter descriptor for the <cite>Scale difference</cite>
     * ({@linkplain BursaWolfParameters#dS dS}) parameter value.
     * Valid values range from negative to positive infinity.
     * Units are {@linkplain Units#PPM parts per million}.
     */
    static final ParameterDescriptor<Double> DS;
    static {
        final ParameterBuilder builder = builder();
        TX = createShift(builder.addName("X-axis translation").addName(Citations.OGC, "dx"));
        TY = createShift(builder.addName("Y-axis translation").addName(Citations.OGC, "dy"));
        TZ = createShift(builder.addName("Z-axis translation").addName(Citations.OGC, "dz"));
        RX = createRotation(builder, "X-axis rotation", "ex");
        RY = createRotation(builder, "Y-axis rotation", "ey");
        RZ = createRotation(builder, "Z-axis rotation", "ez");
        DS = builder.addName("Scale difference").addName(Citations.OGC, "ppm").create(1, Units.PPM);
    }

    /**
     * Convenience method for building the rotation parameters.
     */
    private static ParameterDescriptor<Double> createRotation(final ParameterBuilder builder, final String name, final String alias) {
        return builder.addName(name).addName(Citations.OGC, alias).createBounded(-180*60*60, 180*60*60, 0, NonSI.SECOND_ANGLE);
    }

    /**
     * Return value for {@link #getType()}.
     */
    static final int TRANSLATION=1, SEVEN_PARAM=2, FRAME_ROTATION=3, OTHER=0;

    /**
     * Constructs a provider with the specified parameters.
     *
     * @param sourceDimensions Number of dimensions in the source CRS of this operation method.
     * @param targetDimensions Number of dimensions in the target CRS of this operation method.
     */
    GeocentricAffine(int sourceDimensions, int targetDimensions, ParameterDescriptorGroup parameters) {
        super(sourceDimensions, targetDimensions, parameters);
    }

    /**
     * Returns the interface implemented by all coordinate operations that extends this class.
     *
     * @return Fixed to {@link Transformation}.
     */
    @Override
    public final Class<Transformation> getOperationType() {
        return Transformation.class;
    }

    /**
     * Returns the operation sub-type as one of {@link #TRANSLATION}, {@link #SEVEN_PARAM},
     * {@link #FRAME_ROTATION} or {@link #OTHER} constants.
     */
    abstract int getType();

    /**
     * Creates a math transform from the specified group of parameter values.
     * The default implementation creates an affine transform, but some subclasses
     * will wrap that affine operation into Geographic/Geocentric conversions.
     *
     * @param  factory The factory to use for creating concatenated transforms.
     * @param  values The group of parameter values.
     * @return The created math transform.
     * @throws FactoryException if a transform can not be created.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public MathTransform createMathTransform(final MathTransformFactory factory, final ParameterValueGroup values)
            throws FactoryException
    {
        final BursaWolfParameters parameters = new BursaWolfParameters(null, null);
        final Parameters pv = Parameters.castOrWrap(values);
        boolean reverseRotation = false;
        switch (getType()) {
            default:             throw new AssertionError();
            case FRAME_ROTATION: reverseRotation = true;               // Fall through
            case SEVEN_PARAM:    parameters.rX = pv.doubleValue(RX);
                                 parameters.rY = pv.doubleValue(RY);
                                 parameters.rZ = pv.doubleValue(RZ);
                                 parameters.dS = pv.doubleValue(DS);
            case TRANSLATION:    parameters.tX = pv.doubleValue(TX);   // Fall through
                                 parameters.tY = pv.doubleValue(TY);
                                 parameters.tZ = pv.doubleValue(TZ);
        }
        if (reverseRotation) {
            parameters.reverseRotation();
        }
        return MathTransforms.linear(parameters.getPositionVectorTransformation(null));
    }

    /**
     * Given a transformation chain, conditionally replaces the affine transform elements by an alternative object
     * showing the Bursa-Wolf parameters. The replacement is applied if and only if the affine transform is a scale,
     * translation or rotation in the geocentric domain.
     *
     * <p>This method is invoked only by {@code ConcatenatedTransform.getPseudoSteps()} for the need of WKT formatting.
     * The purpose of this method is very similar to the purpose of {@code AbstractMathTransform.beforeFormat(List, int,
     * boolean)} except that we need to perform the {@code forDatumShift(…)} work only after {@code beforeFormat(…)}
     * finished its work for all {@code ContextualParameters}, including the {@code EllipsoidalToCartesianTransform}'s
     * one.</p>
     *
     * @param transforms The full chain of concatenated transforms.
     */
    public static void asDatumShift(final List<Object> transforms) throws IllegalArgumentException {
        for (int i=transforms.size() - 2; --i >= 0;) {
            if (isOperation(GeographicToGeocentric.NAME, transforms.get(i)) &&
                isOperation(GeocentricToGeographic.NAME, transforms.get(i+2)))
            {
                final Object step = transforms.get(i+1);
                if (step instanceof LinearTransform) {
                    final BursaWolfParameters parameters = new BursaWolfParameters(null, null);
                    try {
                        /*
                         * We use a 0.01 metre tolerance (Formulas.LINEAR_TOLERANCE) based on the knowledge that the
                         * translation terms are in metres and the rotation terms have the some order of magnitude.
                         */
                        parameters.setPositionVectorTransformation(((LinearTransform) step).getMatrix(), Formulas.LINEAR_TOLERANCE);
                    } catch (IllegalArgumentException e) {
                        /*
                         * Should not occur, except sometime on inverse transform of relatively complex datum shifts
                         * (more than just translation terms). We can fallback on formatting the full matrix.
                         */
                        Logging.recoverableException(Logging.getLogger(Loggers.WKT), GeocentricAffine.class, "asDatumShift", e);
                        continue;
                    }
                    final boolean isTranslation = parameters.isTranslation();
                    final Parameters values = Parameters.castOrWrap(
                            (isTranslation ? GeocentricTranslation.PARAMETERS : PositionVector7Param.PARAMETERS).createValue());
                    values.getOrCreate(TX).setValue(parameters.tX);
                    values.getOrCreate(TY).setValue(parameters.tY);
                    values.getOrCreate(TZ).setValue(parameters.tZ);
                    if (!isTranslation) {
                        values.getOrCreate(RX).setValue(parameters.rX);
                        values.getOrCreate(RY).setValue(parameters.rY);
                        values.getOrCreate(RZ).setValue(parameters.rZ);
                        values.getOrCreate(DS).setValue(parameters.dS);
                    }
                    transforms.set(i+1, new FormattableObject() {
                        @Override protected String formatTo(final Formatter formatter) {
                            WKTUtilities.appendParamMT(values, formatter);
                            return WKTKeywords.Param_MT;
                        }
                    });
                }
            }
        }
    }

    /**
     * Returns {@code true} if the given object is an operation of the given name.
     */
    private static boolean isOperation(final String expected, final Object actual) {
        return (actual instanceof Parameterized) &&
               IdentifiedObjects.isHeuristicMatchForName(((Parameterized) actual).getParameterDescriptors(), expected);
    }
}
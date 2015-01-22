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
package org.apache.sis.math;

import java.util.Arrays;
import java.io.Serializable;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.apache.sis.internal.util.DoubleDouble;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.util.resources.Errors;


/**
 * Equation of a plane in a three-dimensional space (<var>x</var>,<var>y</var>,<var>z</var>).
 * The plane equation is expressed by {@link #c}, {@link #cx} and {@link #cy} coefficients as below:
 *
 * <blockquote>
 *   <var>z</var>(<var>x</var>,<var>y</var>) = <var>c</var> + <var>cx</var>⋅<var>x</var> + <var>cy</var>⋅<var>y</var>
 * </blockquote>
 *
 * Those coefficients can be set directly, or computed by a linear regression of this plane
 * through a set of three-dimensional points.
 *
 * @author  Martin Desruisseaux (MPO, IRD)
 * @author  Howard Freeland (MPO, for algorithmic inspiration)
 * @since   0.5 (derived from geotk-1.0)
 * @version 0.5
 * @module
 */
public class Plane implements Cloneable, Serializable {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 2956201711131316723L;

    /**
     * The <var>c</var> coefficient for this plane. This coefficient appears in the plane equation
     * <var><strong>c</strong></var>+<var>cx</var>⋅<var>x</var>+<var>cy</var>⋅<var>y</var>.
     */
    public double c;

    /**
     * The <var>cx</var> coefficient for this plane. This coefficient appears in the plane equation
     * <var>c</var>+<var><strong>cx</strong></var>⋅<var>x</var>+<var>cy</var>⋅<var>y</var>.
     */
    public double cx;

    /**
     * The <var>cy</var> coefficient for this plane. This coefficient appears in the place equation
     * <var>c</var>+<var>cx</var>⋅<var>x</var>+<var><strong>cy</strong></var>⋅<var>y</var>.
     */
    public double cy;

    /**
     * Construct a new plane. All coefficients are set to 0.
     */
    public Plane() {
    }

    /**
     * Computes the <var>z</var> value for the specified (<var>x</var>,<var>y</var>) point.
     * The <var>z</var> value is computed using the following equation:
     *
     * <blockquote>z(x,y) = {@linkplain #c} + {@linkplain #cx}⋅x + {@linkplain #cy}⋅y</blockquote>
     *
     * @param x The <var>x</var> value.
     * @param y The <var>y</var> value.
     * @return  The <var>z</var> value.
     */
    public final double z(final double x, final double y) {
        return c + cx*x + cy*y;
    }

    /**
     * Computes the <var>y</var> value for the specified (<var>x</var>,<var>z</var>) point.
     * The <var>y</var> value is computed using the following equation:
     *
     * <blockquote>y(x,z) = (z - ({@linkplain #c} + {@linkplain #cx}⋅x)) / {@linkplain #cy}</blockquote>
     *
     * @param x The <var>x</var> value.
     * @param z The <var>y</var> value.
     * @return  The <var>y</var> value.
     */
    public final double y(final double x, final double z) {
        return (z - (c + cx*x)) / cy;
    }

    /**
     * Computes the <var>x</var> value for the specified (<var>y</var>,<var>z</var>) point.
     * The <var>x</var> value is computed using the following equation:
     *
     * <blockquote>x(y,z) = (z - ({@linkplain #c} + {@linkplain #cy}⋅y)) / {@linkplain #cx}</blockquote>
     *
     * @param y The <var>x</var> value.
     * @param z The <var>y</var> value.
     * @return  The <var>x</var> value.
     */
    public final double x(final double y, final double z) {
        return (z - (c + cy*y)) / cx;
    }

    /**
     * Computes the plane's coefficients from the given ordinate values.
     * This method uses a linear regression in the least-square sense.
     * {@link Double#NaN} values are ignored.
     *
     * <p>The result is undetermined if all points are colinear.</p>
     *
     * @param  x vector of <var>x</var> coordinates.
     * @param  y vector of <var>y</var> coordinates.
     * @param  z vector of <var>z</var> values.
     * @return An estimation of the Pearson correlation coefficient.
     * @throws IllegalArgumentException if <var>x</var>, <var>y</var> and <var>z</var> do not have the same length.
     */
    public double fit(final double[] x, final double[] y, final double[] z) {
        return fit(new CompoundDirectPositions(x, y, z));
    }

    /**
     * Computes the plane's coefficients from the given sequence of points.
     * This method uses a linear regression in the least-square sense.
     * Points shall be three dimensional with ordinate values in the (<var>x</var>,<var>y</var>,<var>z</var>) order.
     * {@link Double#NaN} ordinate values are ignored.
     *
     * <p>The result is undetermined if all points are colinear.</p>
     *
     * @param  points The three dimensional points.
     * @return An estimation of the Pearson correlation coefficient.
     * @throws MismatchedDimensionException if a point is not three-dimensional.
     */
    public double fit(final DirectPosition... points) throws MismatchedDimensionException {
        return fit(Arrays.asList(points));
    }

    /**
     * Implementation of public {@code fit(…)} methods.
     * This method needs to iterate over the points two times:
     * one for computing the coefficients, and one for the computing the Pearson coefficient.
     */
    private double fit(final Iterable<DirectPosition> points) {
        int i = 0, n = 0;
        final DoubleDouble sum_x  = new DoubleDouble();
        final DoubleDouble sum_y  = new DoubleDouble();
        final DoubleDouble sum_z  = new DoubleDouble();
        final DoubleDouble sum_xx = new DoubleDouble();
        final DoubleDouble sum_yy = new DoubleDouble();
        final DoubleDouble sum_xy = new DoubleDouble();
        final DoubleDouble sum_zx = new DoubleDouble();
        final DoubleDouble sum_zy = new DoubleDouble();
        final DoubleDouble xx     = new DoubleDouble();
        final DoubleDouble yy     = new DoubleDouble();
        final DoubleDouble xy     = new DoubleDouble();
        final DoubleDouble zx     = new DoubleDouble();
        final DoubleDouble zy     = new DoubleDouble();
        for (final DirectPosition p : points) {
            final int dimension = p.getDimension();
            if (dimension != 3) {
                throw new MismatchedDimensionException(Errors.format(
                        Errors.Keys.MismatchedDimension_3, "positions[" + i + ']', 3, dimension));
            }
            i++;
            final double xi = p.getOrdinate(0); if (Double.isNaN(xi)) continue;
            final double yi = p.getOrdinate(1); if (Double.isNaN(yi)) continue;
            final double zi = p.getOrdinate(2); if (Double.isNaN(zi)) continue;
            xx.setToProduct(xi, xi);
            yy.setToProduct(yi, yi);
            xy.setToProduct(xi, yi);
            zx.setToProduct(zi, xi);
            zy.setToProduct(zi, yi);
            sum_x.add(xi);
            sum_y.add(yi);
            sum_z.add(zi);
            sum_xx.add(xx);
            sum_yy.add(yy);
            sum_xy.add(xy);
            sum_zx.add(zx);
            sum_zy.add(zy);
            n++;
        }
        /*
         *    ( sum_zx - sum_z*sum_x )  =  cx*(sum_xx - sum_x*sum_x) + cy*(sum_xy - sum_x*sum_y)
         *    ( sum_zy - sum_z*sum_y )  =  cx*(sum_xy - sum_x*sum_y) + cy*(sum_yy - sum_y*sum_y)
         */
        zx.setFrom(sum_x); zx.divide(-n, 0); zx.multiply(sum_z); zx.add(sum_zx);    // zx = sum_zx - sum_z*sum_x/n
        zy.setFrom(sum_y); zy.divide(-n, 0); zy.multiply(sum_z); zy.add(sum_zy);    // zy = sum_zy - sum_z*sum_y/n
        xx.setFrom(sum_x); xx.divide(-n, 0); xx.multiply(sum_x); xx.add(sum_xx);    // xx = sum_xx - sum_x*sum_x/n
        xy.setFrom(sum_y); xy.divide(-n, 0); xy.multiply(sum_x); xy.add(sum_xy);    // xy = sum_xy - sum_x*sum_y/n
        yy.setFrom(sum_y); yy.divide(-n, 0); yy.multiply(sum_y); yy.add(sum_yy);    // yy = sum_yy - sum_y*sum_y/n
        /*
         * den = (xy*xy - xx*yy)
         */
        final DoubleDouble tmp = new DoubleDouble(xx); tmp.multiply(yy);
        final DoubleDouble den = new DoubleDouble(xy); den.multiply(xy);
        den.subtract(tmp);
        /*
         * cx = (zy*xy - zx*yy) / den
         * cy = (zx*xy - zy*xx) / den
         * c  = (sum_z - (cx*sum_x + cy*sum_y)) / n
         */
        final DoubleDouble cx = new DoubleDouble(zy); cx.multiply(xy); tmp.setFrom(zx); tmp.multiply(yy); cx.subtract(tmp); cx.divide(den);
        final DoubleDouble cy = new DoubleDouble(zx); cy.multiply(xy); tmp.setFrom(zy); tmp.multiply(xx); cy.subtract(tmp); cy.divide(den);
        final DoubleDouble c  = new DoubleDouble(cy);
        c.multiply(sum_y);
        tmp.setFrom(cx);
        tmp.multiply(sum_x);
        tmp.add(c);
        c.setFrom(sum_z);
        c.subtract(tmp);
        c.divide(n, 0);
        /*
         * Done - store the result.
         */
        this.c  = c .value;
        this.cx = cx.value;
        this.cy = cy.value;
        /*
         * At this point, the model is computed. Now computes an estimation of the Pearson
         * correlation coefficient. Note that both the z array and the z computed from the
         * model have the same average, called sum_z below (the name is not true anymore).
         *
         * We do not use double-double arithmetic here since the Pearson coefficient is
         * for information purpose (quality estimation).
         */
        final double mean_x = sum_x.value / n;
        final double mean_y = sum_y.value / n;
        final double mean_z = sum_z.value / n;
        double sx=0, sy=0, pe=0;
        for (final DirectPosition p : points) {
            final double xi = p.getOrdinate(2) - mean_z;
            if (!Double.isNaN(xi)) {
                final double yi = cx.value * (p.getOrdinate(0) - mean_x)
                                + cy.value * (p.getOrdinate(1) - mean_y);
                if (!Double.isNaN(yi)) {
                    sx += xi * xi;
                    sy += yi * yi;
                    pe += xi * yi;
                }
            }
        }
        return pe / Math.sqrt(sx * sy);
    }

    /**
     * Returns a string representation of this plane.
     * The string will contains the plane's equation, as below:
     *
     * <blockquote>
     *     <var>z</var>(<var>x</var>,<var>y</var>) = {@link #c} +
     *     {@link #cx}⋅<var>x</var> + {@link #cy}⋅<var>y</var>
     * </blockquote>
     */
    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("z(x,y)= ");
        if (c == 0 && cx == 0 && cy == 0) {
            buffer.append(0);
        } else {
            if (c != 0) {
                buffer.append(c).append(" + ");
            }
            if (cx != 0) {
                buffer.append(cx).append("⋅x");
                if (cy != 0) {
                    buffer.append(" + ");
                }
            }
            if (cy != 0) {
                buffer.append(cy).append("⋅y");
            }
        }
        return buffer.toString();
    }

    /**
     * Compares this plane with the specified object for equality.
     *
     * @param object The object to compare with this plane for equality.
     * @return {@code true} if both objects are equal.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && getClass() == object.getClass()) {
            final Plane that = (Plane) object;
            return Numerics.equals(this.c,  that.c ) &&
                   Numerics.equals(this.cx, that.cx) &&
                   Numerics.equals(this.cy, that.cy);
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code value for this plane.
     */
    @Override
    public int hashCode() {
        return Numerics.hashCode(serialVersionUID
                     ^ (Double.doubleToLongBits(c )
                + 31 * (Double.doubleToLongBits(cx)
                + 31 * (Double.doubleToLongBits(cy)))));
    }

    /**
     * Returns a clone of this plane.
     *
     * @return A clone of this plane.
     */
    @Override
    public Plane clone() {
        try {
            return (Plane) super.clone();
        } catch (CloneNotSupportedException exception) {
            throw new AssertionError(exception);
        }
    }
}

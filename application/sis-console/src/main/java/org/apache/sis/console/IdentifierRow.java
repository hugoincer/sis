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
package org.apache.sis.console;

import java.util.Set;
import java.util.EnumSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.io.PrintWriter;
import org.opengis.metadata.Identifier;
import org.opengis.util.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.ReferenceSystem;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.CRS;
import org.apache.sis.internal.util.X364;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Vocabulary;


/**
 * A row containing a metadata or CRS identifier, its name and a status flag.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 */
final class IdentifierRow {
    /**
     * The state to write in the left margin before the identifier.
     *
     * <b>MAINTENANCE NOTE:</b> if this enumeration is modified,
     * update {@code IdentifierState.properties} accordingly.
     */
    static enum State {
        VALID("   "), APPROXIMATIVE("~  "), AXIS_ORDER("!  "), MISMATCH("!! "), UNKNOWN("?  ");

        /** The string representation. */ final String text;
        private State(final String p) {this.text = p;};
    }

    /**
     * The two-letters state to write before the identifier.
     */
    private final State state;

    /**
     * The identifier.
     */
    private final String identifier;

    /**
     * A description to write after the identifier.
     */
    private final CharSequence description;

    /**
     * Creates a row for the given elements.
     */
    IdentifierRow(final State state, final String identifier, final CharSequence description) {
        this.state       = state;
        this.identifier  = identifier;
        this.description = description;
    }

    /**
     * Creates an identifier row for the given CRS.
     * This method gives precedence to {@code "urn:ogc:def:"} identifiers if possible.
     *
     * @return The row, or {@code null} if no identifier has been found.
     */
    static IdentifierRow create(ReferenceSystem rs) throws FactoryException {
        String identifier = IdentifiedObjects.lookupURN(rs, null);
        if (identifier == null) {
            /*
             * If we can not find an identifier matching the EPSG or WMS definitions,
             * look at the identifiers declared in the CRS and verify their validity.
             */
            for (final Identifier id : rs.getIdentifiers()) {
                final String c = IdentifiedObjects.toURN(rs.getClass(), id);
                if (c != null) {
                    identifier = c;
                    break;                                          // Stop at the first "urn:ogc:def:…".
                }
                if (identifier == null) {
                    identifier = IdentifiedObjects.toString(id);    // "AUTHORITY:CODE" as a fallback if no URN.
                }
            }
            if (identifier == null) {
                return null;                                        // No identifier found.
            }
        }
        /*
         * The CRS provided by the user contains identifier, but the 'lookupURN' operation above failed to
         * find it. The most likely cause is that the user-provided CRS does not use the same axis order.
         */
        State state;
        try {
            final ReferenceSystem def = CRS.forCode(identifier);
            final ComparisonMode c = ComparisonMode.equalityLevel(def, rs);
            if (c == null) {
                state = State.MISMATCH;
            } else switch (c) {
                case ALLOW_VARIANT: {
                    state = State.AXIS_ORDER;
                    break;
                }
                case APPROXIMATIVE: {
                    state = State.APPROXIMATIVE;
                    rs = def;
                    break;
                }
                default: {
                    state = State.VALID;
                    rs = def;
                    break;
                }
            }
        } catch (NoSuchAuthorityCodeException e) {
            state = State.UNKNOWN;
        }
        return new IdentifierRow(state, identifier, rs.getName().getCode());
    }

    /**
     * Prints all non-null rows to the given output.
     */
    static void print(final Iterable<IdentifierRow> rows, final PrintWriter out,
            final Locale locale, final boolean colors)
    {
        int width = 0;
        for (final IdentifierRow row : rows) {
            if (row != null) {
                width = Math.max(width, row.identifier.length());
            }
        }
        width += 4;
        final Set<State> states = EnumSet.noneOf(State.class);
        for (final IdentifierRow row : rows) {
            if (row != null) {
                states.add(row.state);
                final boolean warning = colors && row.state.text.startsWith("!");
                if (warning) out.print(X364.FOREGROUND_RED.sequence());
                out.print(row.state.text);
                out.print(' ');
                out.print(row.identifier);
                if (warning) out.print(X364.FOREGROUND_DEFAULT.sequence());
                if (colors)  out.print(X364.FOREGROUND_GRAY.sequence());
                out.print(CharSequences.spaces(width - row.identifier.length()));
                out.print("| ");
                out.println(row.description);
                if (colors) out.print(X364.FOREGROUND_DEFAULT.sequence());
            }
        }
        states.remove(State.VALID);
        if (!states.isEmpty()) {
            out.println();
            out.println(Vocabulary.getResources(locale).getLabel(Vocabulary.Keys.Legend));
            final ResourceBundle resources = ResourceBundle.getBundle("org.apache.sis.console.IdentifierState", locale);
            for (final State state : states) {
                final boolean warning = colors && state.text.startsWith("!");
                if (warning) out.print(X364.FOREGROUND_RED.sequence());
                out.print(state.text);
                if (warning) out.print(X364.FOREGROUND_DEFAULT.sequence());
                out.print(' ');
                out.println(resources.getString(state.name()));
            }
        }
        out.flush();
    }
}
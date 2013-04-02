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

/**
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation Acquisition} implementation.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.acquisition OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in SIS, see the
 * <a href="{@docRoot}/../sis-metadata/index.html">Metadata page on the project web site</a>.
 *
 * <table class="sis"><tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td nowrap>
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation Acquisition information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultObjective Objective}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatformPass PlatformPass}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEvent Event}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequirement Requirement}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequestedDate Requested date}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlan Plan}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultOperation Operation}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform Platform}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultInstrument Instrument}<br>
 * {@code  └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEnvironmentalRecord Environmental record}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.ObjectiveType Objective type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Trigger Trigger}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Context Context}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Sequence Sequence}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.Priority Priority}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.acquisition.GeometryType Geometry type}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.acquisition.OperationType Operation type}<br>
 * </td><td class="sep" nowrap>
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultAcquisitionInformation Acquisition information}<br>
 * {@code  ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultObjective Objective}<br>
 * {@code  │   ├─} {@linkplain org.opengis.metadata.acquisition.ObjectiveType Objective type} (a code list)<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatformPass PlatformPass}<br>
 * {@code  │   │   └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEvent Event}<br>
 * {@code  │   │       ├─} {@linkplain org.opengis.metadata.acquisition.Trigger Trigger} (a code list)<br>
 * {@code  │   │       ├─} {@linkplain org.opengis.metadata.acquisition.Context Context} (a code list)<br>
 * {@code  │   │       └─} {@linkplain org.opengis.metadata.acquisition.Sequence Sequence} (a code list)<br>
 * {@code  │   ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequirement Requirement}<br>
 * {@code  │   │   ├─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultRequestedDate Requested date}<br>
 * {@code  │   │   └─} {@linkplain org.opengis.metadata.acquisition.Priority Priority} (a code list)<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlan Plan}<br>
 * {@code  │       ├─} {@linkplain org.opengis.metadata.acquisition.GeometryType Geometry type} (a code list)<br>
 * {@code  │       └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultOperation Operation}<br>
 * {@code  │           ├─} {@linkplain org.opengis.metadata.acquisition.OperationType Operation type} (a code list)<br>
 * {@code  │           └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultPlatform Platform}<br>
 * {@code  │               └─} {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultInstrument Instrument}<br>
 * {@linkplain org.apache.sis.metadata.iso.acquisition.DefaultEnvironmentalRecord Environmental record}<br>
 * </td></tr></table>
 *
 * {@section Collections and null values}
 * Unless otherwise noted in the Javadoc, all constructors and setter methods accept {@code null} argument.
 * A null argument value means that the metadata element can not be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if there is no value. More specifically:</p>
 * <ul>
 *   <li>If the return type is a collection, the method may return an empty collection (never {@code null}).</li>
 *   <li>If the return type is an array, the method may return an empty array (never {@code null}).</li>
 *   <li>Otherwise the method may return {@code null}.</li>
 * </ul>
 *
 * Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <cite>live</cite>: adding new elements in the collection
 * modify directly the underlying metadata object.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMI, xmlns = {
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(CI_ResponsibleParty.class),
    @XmlJavaTypeAdapter(EX_Extent.class),
    @XmlJavaTypeAdapter(GM_Object.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_ProgressCode.class),
    @XmlJavaTypeAdapter(MI_ContextCode.class),
    @XmlJavaTypeAdapter(MI_EnvironmentalRecord.class),
    @XmlJavaTypeAdapter(MI_Event.class),
    @XmlJavaTypeAdapter(MI_GeometryTypeCode.class),
    @XmlJavaTypeAdapter(MI_Instrument.class),
    @XmlJavaTypeAdapter(MI_Objective.class),
    @XmlJavaTypeAdapter(MI_ObjectiveTypeCode.class),
    @XmlJavaTypeAdapter(MI_Operation.class),
    @XmlJavaTypeAdapter(MI_OperationTypeCode.class),
    @XmlJavaTypeAdapter(MI_Plan.class),
    @XmlJavaTypeAdapter(MI_Platform.class),
    @XmlJavaTypeAdapter(MI_PlatformPass.class),
    @XmlJavaTypeAdapter(MI_PriorityCode.class),
    @XmlJavaTypeAdapter(MI_RequestedDate.class),
    @XmlJavaTypeAdapter(MI_Requirement.class),
    @XmlJavaTypeAdapter(MI_SequenceCode.class),
    @XmlJavaTypeAdapter(MI_TriggerCode.class),

    // Java types, primitive types and basic OGC types handling
//  @XmlJavaTypeAdapter(GO_DateTime.class), // TODO
    @XmlJavaTypeAdapter(StringAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class)
})
package org.apache.sis.metadata.iso.acquisition;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
import org.apache.sis.internal.jaxb.geometry.GM_Object;

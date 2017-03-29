/**
 * Copyright (C) 2012-2015 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.wps.tamis;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlString;
import org.apache.xmlbeans.impl.values.XmlAnyTypeImpl;
import org.n52.iceland.coding.decode.DecoderKey;
import org.n52.iceland.exception.CodedException;
import org.n52.iceland.exception.ows.InvalidParameterValueException;
import org.n52.iceland.exception.ows.MissingParameterValueException;
import org.n52.iceland.exception.ows.OwsExceptionCode;
import org.n52.iceland.exception.ows.OwsExceptionReport;
import org.n52.iceland.exception.ows.concrete.UnsupportedDecoderInputException;
import org.n52.iceland.ogc.gml.AbstractFeature;
import org.n52.iceland.ogc.gml.CodeWithAuthority;
import org.n52.iceland.ogc.gml.time.Time;
import org.n52.iceland.ogc.gml.time.Time.NilReason;
import org.n52.iceland.ogc.gml.time.Time.TimeIndeterminateValue;
import org.n52.iceland.ogc.gml.time.TimeInstant;
import org.n52.iceland.ogc.gml.time.TimePeriod;
import org.n52.iceland.ogc.om.OmConstants;
import org.n52.iceland.ogc.sos.ConformanceClasses;
import org.n52.iceland.ogc.sos.Sos2Constants;
import org.n52.iceland.ogc.sos.SosConstants;
import org.n52.iceland.service.ServiceConstants.SupportedType;
import org.n52.iceland.util.Constants;
import org.n52.sos.decode.AbstractOmDecoderv20;
import org.n52.sos.exception.ows.concrete.UnsupportedDecoderXmlInputException;
import org.n52.sos.ogc.om.AbstractPhenomenon;
import org.n52.sos.ogc.om.ObservationValue;
import org.n52.sos.ogc.om.OmObservableProperty;
import org.n52.sos.ogc.om.OmObservation;
import org.n52.sos.ogc.om.OmObservationConstellation;
import org.n52.sos.ogc.om.SingleObservationValue;
import org.n52.sos.ogc.om.TimeValuePair;
import org.n52.sos.ogc.om.values.BooleanValue;
import org.n52.sos.ogc.om.values.CountValue;
import org.n52.sos.ogc.om.values.NilTemplateValue;
import org.n52.sos.ogc.om.values.TVPValue;
import org.n52.sos.ogc.om.values.TextValue;
import org.n52.sos.ogc.sensorML.SensorML;
import org.n52.sos.ogc.sos.SosProcedureDescription;
import org.n52.sos.ogc.wml.MeasurementTimeseries;
import org.n52.sos.ogc.wml.MeasurementTimeseriesPoint;
import org.n52.sos.ogc.wml.WaterMLConstants;
import org.n52.sos.util.CodingHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import net.opengis.om.x20.NamedValuePropertyType;
import net.opengis.om.x20.OMObservationType;
import net.opengis.om.x20.TimeObjectPropertyType;

/**
 * @since 4.0.0
 *
 */
public class OmWmlResultDecoderv20 extends AbstractOmDecoderv20 {

    private static final Logger LOGGER = LoggerFactory.getLogger(OmWmlResultDecoderv20.class);

    private static final Set<DecoderKey> DECODER_KEYS = CodingHelper
            .decoderKeysForElements(WaterMLConstants.NS_WML_20, 
                                    OMObservationType.class,
                                    NamedValuePropertyType.class,
                                    NamedValuePropertyType[].class);

    private static final Set<SupportedType> SUPPORTED_TYPES = ImmutableSet
            .<SupportedType>builder()
            .add(OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION_TYPE)
            //.add(OmConstants.OBS_TYPE_COMPLEX_OBSERVATION_TYPE)
            .add(OmConstants.OBS_TYPE_GEOMETRY_OBSERVATION_TYPE)
            .add(OmConstants.OBS_TYPE_CATEGORY_OBSERVATION_TYPE)
            .add(OmConstants.OBS_TYPE_COUNT_OBSERVATION_TYPE)
            .add(OmConstants.OBS_TYPE_MEASUREMENT_TYPE)
            .add(OmConstants.OBS_TYPE_TEXT_OBSERVATION_TYPE)
            .add(OmConstants.OBS_TYPE_TRUTH_OBSERVATION_TYPE)
            .build();

    private static final Set<String> CONFORMANCE_CLASSES = ImmutableSet.of(
            ConformanceClasses.OM_V2_MEASUREMENT,
            ConformanceClasses.OM_V2_CATEGORY_OBSERVATION,
            ConformanceClasses.OM_V2_COUNT_OBSERVATION,
            ConformanceClasses.OM_V2_TRUTH_OBSERVATION,
            // ConformanceClasses.OM_V2_GEOMETRY_OBSERVATION,
            ConformanceClasses.OM_V2_TEXT_OBSERVATION);

    public OmWmlResultDecoderv20() {
        LOGGER.debug("Decoder for the following keys initialized successfully: {}!", Joiner.on(", ")
                .join(DECODER_KEYS));
    }

    public Set<DecoderKey> getKeys() {
        return Collections.unmodifiableSet(DECODER_KEYS);
    }

    public Set<SupportedType> getSupportedTypes() {
        return Collections.unmodifiableSet(SUPPORTED_TYPES);
    }

    public Set<String> getConformanceClasses(String service, String version) {
        if(SosConstants.SOS.equals(service) && Sos2Constants.SERVICEVERSION.equals(version)) {
            return Collections.unmodifiableSet(CONFORMANCE_CLASSES);
        }
        return Collections.emptySet();
    }

    @Override
    public Object decode(Object object) throws OwsExceptionReport {
        // validate document
        // XmlHelper.validateDocument((XmlObject) object);
        if (object instanceof OMObservationType) {
            return parseOmObservation((OMObservationType) object);
        } else if (object instanceof NamedValuePropertyType) {
            return parseNamedValueType((NamedValuePropertyType) object);
        } else if (object instanceof NamedValuePropertyType[]) {
            return parseNamedValueTypeArray((NamedValuePropertyType[]) object);
        } else {
            if (object instanceof XmlObject) {
                throw new UnsupportedDecoderXmlInputException(this, (XmlObject)object);
            }
            throw new UnsupportedDecoderInputException(this, object);
        }
    }

    private OmObservation parseOmObservation(OMObservationType omObservation) throws OwsExceptionReport {
        Map<String, AbstractFeature> featureMap = Maps.newHashMap();
        OmObservation sosObservation = new OmObservation();
        sosObservation.setIdentifier(getIdentifier(omObservation));
        if (omObservation.isSetDescription()) {
            sosObservation.setDescription(omObservation.getDescription().getStringValue());
        }
        OmObservationConstellation observationConstallation = getObservationConstellation(omObservation);
        sosObservation.setObservationConstellation(observationConstallation);
        sosObservation.setResultTime(getResultTime(omObservation));
        sosObservation.setValidTime(getValidTime(omObservation));
        if (omObservation.getParameterArray() != null) {
            sosObservation.setParameter(parseNamedValueTypeArray(omObservation.getParameterArray()));
        }
        sosObservation.setValue(getObservationValue(omObservation));
        try {
            Object decodeXmlElement = CodingHelper.decodeXmlElement(omObservation.getFeatureOfInterest());
            if (decodeXmlElement instanceof AbstractFeature) {
                AbstractFeature featureOfInterest = (AbstractFeature) decodeXmlElement;
                observationConstallation.setFeatureOfInterest(checkFeatureWithMap(featureOfInterest, featureMap));
            }
        } catch (OwsExceptionReport e) {
            if (sosObservation.getValue() != null && sosObservation.getValue().getPhenomenonTime() != null
                    && sosObservation.getPhenomenonTime().isSetNilReason()
                    && sosObservation.getValue().getPhenomenonTime().getNilReason().equals(NilReason.template)) {
                for (CodedException exception : e.getExceptions()) {
                    if (exception.getCode().equals(OwsExceptionCode.InvalidParameterValue)) {
                        throw new InvalidParameterValueException().at(exception.getLocator()).withMessage(
                                exception.getMessage());
                    } else if (exception.getCode().equals(OwsExceptionCode.MissingParameterValue)) {
                        throw new MissingParameterValueException(exception.getLocator());
                    }
                }
            }
            throw e;
        }
        // TODO: later for spatial filtering profile
        // omObservation.getParameterArray();

        return sosObservation;
    }

    private CodeWithAuthority getIdentifier(OMObservationType omObservation) throws OwsExceptionReport {
        if (omObservation.getIdentifier() != null) {
            Object decodedObject = CodingHelper.decodeXmlObject(omObservation.getIdentifier());
            if (decodedObject instanceof CodeWithAuthority) {
                return (CodeWithAuthority) decodedObject;
            }
        }
        return null;
    }

    private OmObservationConstellation getObservationConstellation(OMObservationType omObservation)
            throws OwsExceptionReport {
        OmObservationConstellation observationConstellation = new OmObservationConstellation();
        observationConstellation.setObservationType(getObservationType(omObservation));
        observationConstellation.setProcedure(createProcedure(getProcedure(omObservation)));
        observationConstellation.setObservableProperty(getObservableProperty(omObservation));
        return observationConstellation;
    }

    private String getObservationType(OMObservationType omObservation) {
        if (omObservation.getType() != null) {
            return omObservation.getType().getHref();
        }
        return null;
    }

    private String getProcedure(OMObservationType omObservation) {
        if (omObservation.getProcedure() != null) {
            return omObservation.getProcedure().getHref();
        }
        return null;
    }

    private AbstractPhenomenon getObservableProperty(OMObservationType omObservation) {
        if (omObservation.getObservedProperty() != null) {
            return new OmObservableProperty(omObservation.getObservedProperty().getHref());
        }
        return null;
    }

    private Time getPhenomenonTime(OMObservationType omObservation) throws OwsExceptionReport {
        TimeObjectPropertyType phenomenonTime = omObservation.getPhenomenonTime();
        if (phenomenonTime.isSetHref() && phenomenonTime.getHref().startsWith(Constants.NUMBER_SIGN_STRING)) {
            TimeInstant timeInstant = new TimeInstant();
            timeInstant.setGmlId(phenomenonTime.getHref());
            return timeInstant;
        } else if (phenomenonTime.isSetNilReason() && phenomenonTime.getNilReason() instanceof String
                && ((String) phenomenonTime.getNilReason()).equals(TimeIndeterminateValue.template.name())) {
            TimeInstant timeInstant = new TimeInstant();
            timeInstant.setIndeterminateValue(TimeIndeterminateValue.getEnumForString((String) phenomenonTime
                    .getNilReason()));
            return timeInstant;
        } else if (phenomenonTime.isSetAbstractTimeObject()) {
            Object decodedObject = CodingHelper.decodeXmlObject(phenomenonTime.getAbstractTimeObject());
            if (decodedObject instanceof Time) {
                return (Time) decodedObject;
            }
            // FIXME else
        }
        throw new InvalidParameterValueException().at(Sos2Constants.InsertObservationParams.observation).withMessage(
                "The requested phenomenonTime type is not supported by this service!");
    }

    private TimeInstant getResultTime(OMObservationType omObservation) throws OwsExceptionReport {
        if (omObservation.getResultTime().isSetHref()) {
            TimeInstant timeInstant = new TimeInstant();
            timeInstant.setGmlId(omObservation.getResultTime().getHref());
            if (omObservation.getResultTime().getHref().charAt(0) == Constants.NUMBER_SIGN_CHAR) {
                // document internal link
                // TODO parse linked element
                timeInstant.setReference(Sos2Constants.EN_PHENOMENON_TIME);
            } else {
                timeInstant.setReference(omObservation.getResultTime().getHref());
            }
            return timeInstant;
        } else if (omObservation.getResultTime().isSetNilReason()
                && omObservation.getResultTime().getNilReason() instanceof String
                && NilReason.template.equals(NilReason.getEnumForString((String) omObservation.getResultTime()
                        .getNilReason()))) {
            TimeInstant timeInstant = new TimeInstant();
            timeInstant
                    .setNilReason(NilReason.getEnumForString((String) omObservation.getResultTime().getNilReason()));
            return timeInstant;
        } else if (omObservation.getResultTime().isSetTimeInstant()) {
            Object decodedObject = CodingHelper.decodeXmlObject(omObservation.getResultTime().getTimeInstant());
            if (decodedObject instanceof TimeInstant) {
                return (TimeInstant) decodedObject;
            }
            throw new InvalidParameterValueException().at(Sos2Constants.InsertObservationParams.observation)
                    .withMessage("The requested resultTime type is not supported by this service!");
        } else {
            throw new InvalidParameterValueException().at(Sos2Constants.InsertObservationParams.observation)
                    .withMessage("The requested resultTime type is not supported by this service!");
        }
    }

    private TimePeriod getValidTime(OMObservationType omObservation) throws OwsExceptionReport {
        if (omObservation.isSetValidTime()) {
            Object decodedObject = CodingHelper.decodeXmlObject(omObservation.getValidTime().getTimePeriod());
            if (decodedObject instanceof TimePeriod) {
                return (TimePeriod) decodedObject;
            }
            throw new InvalidParameterValueException().at(Sos2Constants.InsertObservationParams.observation)
                    .withMessage("The requested validTime type is not supported by this service!");
        }
        return null;
    }

    private ObservationValue<?> getObservationValue(OMObservationType omObservation) throws OwsExceptionReport {
        Time phenomenonTime = getPhenomenonTime(omObservation);
        ObservationValue<?> observationValue;
        if (!omObservation.getResult().getDomNode().hasChildNodes() && phenomenonTime.isSetNilReason()
                && phenomenonTime.getNilReason().equals(NilReason.template)) {
            observationValue = new SingleObservationValue<String>(new NilTemplateValue());
        } else {
            observationValue = getResult(omObservation);
        }
        observationValue.setPhenomenonTime(phenomenonTime);
        return observationValue;
    }

    protected ObservationValue<?> getResult(OMObservationType omObservation) throws OwsExceptionReport {
        XmlObject xbResult = omObservation.getResult();

        if (xbResult.schemaType() == XmlAnyTypeImpl.type) {
            // Template observation for InsertResultTemplate operation
            if (!xbResult.getDomNode().hasChildNodes()) {
                return new SingleObservationValue<String>(new NilTemplateValue());
            } else {
                try {
                    xbResult = XmlObject.Factory.parse(xbResult.xmlText().trim());
                } catch (XmlException e) {
                    LOGGER.error("Error while parsing NamedValueValue", e);
                }
            }
        }
        // TruthObservation
        if (xbResult.schemaType() == XmlBoolean.type) {
            XmlBoolean xbBoolean = (XmlBoolean) xbResult;
            BooleanValue booleanValue = new BooleanValue(xbBoolean.getBooleanValue());
            return new SingleObservationValue<Boolean>(booleanValue);
        }
        // CountObservation
        else if (xbResult.schemaType() == XmlInteger.type) {
            XmlInteger xbInteger = (XmlInteger) xbResult;
            CountValue countValue = new CountValue(Integer.parseInt(xbInteger.getBigIntegerValue().toString()));
            return new SingleObservationValue<Integer>(countValue);
        }
        // TextObservation
        else if (xbResult.schemaType() == XmlString.type) {
            XmlString xbString = (XmlString) xbResult;
            TextValue stringValue = new TextValue(xbString.getStringValue());
            return new SingleObservationValue<String>(stringValue);
        }
        // result elements with other encoding like SWE_ARRAY_OBSERVATION
        else {
            Object decodedObject = CodingHelper.decodeXmlObject(xbResult);
            if (decodedObject instanceof MeasurementTimeseries) {
                
                MeasurementTimeseries measurementTimeseries = (MeasurementTimeseries)decodedObject;
                                
                TVPValue timeValuePairValue = new TVPValue(); 
                
                for (MeasurementTimeseriesPoint point : measurementTimeseries.getPoints()) {
                    timeValuePairValue.addValue(point.getMeasurementTimeValuePair());
                }                
                SingleObservationValue<List<TimeValuePair>> result = new SingleObservationValue<List<TimeValuePair>>();
                result.setValue(timeValuePairValue);
                return result;
            }
            throw new InvalidParameterValueException().at(Sos2Constants.InsertObservationParams.observation)
                    .withMessage("The requested result type is not supported by this service!");
        }
    }

    private AbstractFeature checkFeatureWithMap(AbstractFeature featureOfInterest,
            Map<String, AbstractFeature> featureMap) {
        if (featureOfInterest.getGmlId() != null && !featureOfInterest.getGmlId().isEmpty()) {
            if (featureMap.containsKey(featureOfInterest.getGmlId())) {
                return featureMap.get(featureOfInterest.getGmlId());
            } else {
                featureMap.put(featureOfInterest.getGmlId(), featureOfInterest);
            }
        }
        return featureOfInterest;
    }

    private SosProcedureDescription createProcedure(String procedureIdentifier) {
        SensorML procedure = new SensorML();
        procedure.setIdentifier(procedureIdentifier);
        return procedure;
    }

}
/*
 Copyright 2008-2011 ITACA-TSB, http://www.tsb.upv.es
 Instituto Tecnologico de Aplicaciones de Comunicacion 
 Avanzadas - Grupo Tecnologias para la Salud y el 
 Bienestar (TSB)

 See the NOTICE file distributed with this work for additional 
 information regarding copyright ownership

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package org.universAAL.hw.exporter.zigbee.ha.devices;

import it.cnr.isti.zigbee.ha.cluster.glue.general.event.OnOffEvent;
import it.cnr.isti.zigbee.ha.cluster.glue.general.event.OnOffListener;
import it.cnr.isti.zigbee.ha.device.api.lighting.OnOffLight;
import it.cnr.isti.zigbee.ha.driver.core.ZigBeeHAException;

import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.universAAL.hw.exporter.zigbee.ha.Activator;
import org.universAAL.hw.exporter.zigbee.ha.services.OnOffLightService;
import org.universAAL.middleware.context.ContextEvent;
import org.universAAL.middleware.context.DefaultContextPublisher;
import org.universAAL.middleware.context.owl.ContextProvider;
import org.universAAL.middleware.context.owl.ContextProviderType;
import org.universAAL.middleware.owl.Restriction;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.ServiceCall;
import org.universAAL.middleware.service.ServiceCallee;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.process.ProcessInput;
import org.universAAL.middleware.service.owls.process.ProcessOutput;
import org.universAAL.middleware.service.owls.profile.ServiceProfile;
import org.universAAL.middleware.util.Constants;
import org.universAAL.ontology.lighting.LightSource;
import org.universAAL.ontology.location.indoor.Room;

public class OnOffLightCallee extends ServiceCallee implements OnOffListener {

    static final String DEVICE_URI_PREFIX = OnOffLightService.LIGHTING_SERVER_NAMESPACE
	    + "zbLamp";
    static final String INPUT_DEVICE_URI = OnOffLightService.LIGHTING_SERVER_NAMESPACE
	    + "lampURI";

    private final static Logger log = LoggerFactory
	    .getLogger(OnOffLightCallee.class);
    private OnOffLight zbDevice;
    private DefaultContextPublisher cp;
    LightSource ontologyDevice;

    private ServiceProfile[] newProfiles = OnOffLightService.profiles;

    public OnOffLightCallee(BundleContext context, OnOffLight serv) {
	super(context, null);
	log.debug("Ready to subscribe");
	zbDevice = serv;
	// Commissioning
	String deviceSuffix = zbDevice.getZBDevice().getUniqueIdenfier()
		.replace("\"", "");
	String deviceURI = DEVICE_URI_PREFIX + deviceSuffix;
	ontologyDevice = new LightSource(deviceURI);
	String locationSuffix = Activator.getProperties().getProperty(
		deviceSuffix);
	if (locationSuffix != null
		&& !locationSuffix.equals(Activator.UNINITIALIZED_SUFFIX)) {
	    ontologyDevice
		    .setLocation(new Room(
			    Constants.uAAL_MIDDLEWARE_LOCAL_ID_PREFIX
				    + locationSuffix));
	} else {
	    Properties prop = Activator.getProperties();
	    prop.setProperty(deviceSuffix, Activator.UNINITIALIZED_SUFFIX);
	    Activator.setProperties(prop);
	}
	// Serv reg
	ServiceProfile[] newProfiles = OnOffLightService.profiles;

	ProcessInput input = new ProcessInput(OnOffLightService.INPUT_LAMP);
	input.setParameterType(LightSource.MY_URI);
	input.setCardinality(1, 0);

	Restriction r = Restriction.getFixedValueRestriction(
		OnOffLightService.PROP_CONTROLS, ontologyDevice);

	newProfiles[0].addInput(input);
	newProfiles[0].getTheService().addInstanceLevelRestriction(r,
		new String[] { OnOffLightService.PROP_CONTROLS });
	newProfiles[1].addInput(input);
	newProfiles[1].getTheService().addInstanceLevelRestriction(r,
		new String[] { OnOffLightService.PROP_CONTROLS });
	newProfiles[2].addInput(input);
	newProfiles[2].getTheService().addInstanceLevelRestriction(r,
		new String[] { OnOffLightService.PROP_CONTROLS });
	this.addNewRegParams(newProfiles);

	ContextProvider info = new ContextProvider(
		OnOffLightService.LIGHTING_SERVER_NAMESPACE
			+ "zbLightingContextProvider");
	info.setType(ContextProviderType.controller);
	cp = new DefaultContextPublisher(context, info);

	if (zbDevice.getOnOff().subscribe(this)) {
	    log.debug("Subscribed");
	} else {
	    log.error("Failed to Subscribe!!!");
	}
    }

    public void changedOnOff(OnOffEvent event) {
	log.debug("Changed-Event received");
	LightSource ls = ontologyDevice;
	ls.setBrightness(event.getEvent() ? 100 : 0);
	cp.publish(new ContextEvent(ls, LightSource.PROP_SOURCE_BRIGHTNESS));
    }

    public void unregister() {
	this.removeMatchingRegParams(newProfiles);
    }

    public void communicationChannelBroken() {
	unregister();
    }

    public ServiceResponse handleCall(ServiceCall call) {
	log.debug("Received a call");
	ServiceResponse response;
	if (call == null) {
	    response = new ServiceResponse(CallStatus.serviceSpecificFailure);
	    response.addOutput(new ProcessOutput(
		    ServiceResponse.PROP_SERVICE_SPECIFIC_ERROR, "Null Call!"));
	    return response;
	}

	String operation = call.getProcessURI();
	if (operation == null) {
	    response = new ServiceResponse(CallStatus.serviceSpecificFailure);
	    response.addOutput(new ProcessOutput(
		    ServiceResponse.PROP_SERVICE_SPECIFIC_ERROR,
		    "Null Operation!"));
	    return response;
	}

	if (operation.startsWith(OnOffLightService.SERVICE_GET_ON_OFF)) {
	    return getOnOffStatus();
	} else if (operation.startsWith(OnOffLightService.SERVICE_TURN_ON)) {
	    return setOnStatus();
	} else if (operation.startsWith(OnOffLightService.SERVICE_TURN_OFF)) {
	    return setOffStatus();
	} else {
	    response = new ServiceResponse(CallStatus.serviceSpecificFailure);
	    response.addOutput(new ProcessOutput(
		    ServiceResponse.PROP_SERVICE_SPECIFIC_ERROR,
		    "Invlaid Operation!"));
	    return response;
	}
    }

    private ServiceResponse setOffStatus() {
	log.debug("The service called was 'set the status OFF'");
	try {
	    zbDevice.getOnOff().off();
	    return new ServiceResponse(CallStatus.succeeded);
	} catch (ZigBeeHAException e) {
	    e.printStackTrace();
	    return new ServiceResponse(CallStatus.serviceSpecificFailure);
	}
    }

    private ServiceResponse setOnStatus() {
	log.debug("The service called was 'set the status ON'");
	try {
	    zbDevice.getOnOff().on();
	    return new ServiceResponse(CallStatus.succeeded);
	} catch (ZigBeeHAException e) {
	    e.printStackTrace();
	    return new ServiceResponse(CallStatus.serviceSpecificFailure);
	}
    }

    private ServiceResponse getOnOffStatus() {
	log.debug("The service called was 'get the status'");
	try {
	    ServiceResponse sr = new ServiceResponse(CallStatus.succeeded);
	    sr.addOutput(new ProcessOutput(
		    OnOffLightService.OUTPUT_LAMP_BRIGHTNESS, new Integer(
			    zbDevice.getOnOff().getOnOff() ? 100 : 0)));
	    return sr;
	} catch (ZigBeeHAException e) {
	    e.printStackTrace();
	    return new ServiceResponse(CallStatus.serviceSpecificFailure);
	}
    }

}
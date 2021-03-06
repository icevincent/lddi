/*
     Copyright 2010-2014 AIT Austrian Institute of Technology GmbH
	 http://www.ait.ac.at
     
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

package org.universAAL.lddi.knx.refinementdriver.dpt1.activityhub;

import java.util.Properties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Constants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.universAAL.lddi.lib.activityhub.devicecategory.ActivityHubDeviceCategoryUtil.ActivityHubDeviceCategory;
import org.universAAL.lddi.lib.activityhub.devicemodel.ActivityHubFactory;
import org.universAAL.lddi.lib.activityhub.devicemodel.ActivityHubSensor;
import org.universAAL.lddi.knx.groupdevicecategory.IKnxDpt1;
import org.universAAL.lddi.knx.groupdevicemodel.KnxDpt1GroupDevice;
import org.universAAL.lddi.knx.interfaces.KnxDriver;
import org.universAAL.lddi.knx.utils.KnxEncoder;

/**
 * Working instance of the IKnxDpt1 driver. Registers a service/device in OSGi registry.
 * Tracks on the KNX groupDevice service passed in the attach method in KnxDpt1RefinementDriver class. 
 * When the KNX groupDevice disappears, this service/device is unregistered.
 * 
 * This driver handles 1-bit events (knx datapoint 1), which is on/off.
 * It maps to the appropriate sensor-event of the created ISO11073 sensor. 
 *  
 * Initially it was planned to map certain parameters of KNX sensors to ISO sensors (e.g. location)
 * where this mapping info is stored in a config file. Although, in universAAL this kind of configuration
 * should be done by other components (e.g. AAL Space Configurator). Therefore all knx-iso-mapping code
 * is commented below.
 * 
 * Possibility for automatic location mapping from ETS config: 
 * In ETS Building Parts can be assigned free name and description (String). As a convention the
 * location name according to ISO 11073 (e.g. MDC_AI_LOCATION_BEDROOM) can be assigned in name or
 * description field in ETS. But this may conflict with ETS planning/config from electrical engineers! 
 * 
 * @author Thomas Fuxreiter (foex@gmx.at)
 */
public class KnxDpt1Instance extends KnxDriver implements IKnxDpt1
,ServiceTrackerCustomizer, Constants {

	private BundleContext context;
	private LogService logger;
//	private Dictionary knxIsoMappingProperties;
	
	// my mapping ISO device; exactly one
	private ActivityHubSensor activityHubSensor;
	
//	private Dictionary<String,String> knxIsoMappingProperties;
	
	// the ISO device I registered at OSGi registry
//	private ServiceRegistration myIsoDeviceRegistration;
	
	private ActivityHubDeviceCategory isoDeviceCategory = null;
	
	// constructor called from the driver.attach method
	public KnxDpt1Instance(BundleContext c, LogService log, ActivityHubDeviceCategory ahDevCat) {
		this.context = c;
		this.logger = log;
		this.isoDeviceCategory = ahDevCat;
		
//		// test
//		this.logger.log(LogService.LOG_WARNING, "CHECK default values: False: " + 
//				String.format("%02X", DEFAULT_FALSE_VALUE) + 
//				" True: " + String.format("%02X", DEFAULT_TRUE_VALUE));
		
		//this.knxIsoMappingProperties = knxIsoMappingProperties;
	}


//	/** set my KNX-to-ISO device mapping properties */
//	public void setKnxIsoMappingProperties(Dictionary dictionary) {
//		this.knxIsoMappingProperties = dictionary;
////		this.knxIsoMappingProperties = new Properties();
//	}
	
	
	/**
	 * track on my groupDevice
	 * @param KnxDpt1GroupDevice
	 */
	public Object addingService(ServiceReference reference) {
		
		KnxDpt1GroupDevice knxDev = (KnxDpt1GroupDevice) this.context.getService(reference);

		
		/** now couple my driver to the groupDevice */
		if ( this.setgroupDevice(knxDev) )
			this.logger.log(LogService.LOG_INFO, "Successfully coupled " + IKnxDpt1.MY_DEVICE_CATEGORY 
					+ " driver to groupDevice " + this.groupDevice.getGroupAddress());
		else {
			this.logger.log(LogService.LOG_ERROR, "Error coupling " + IKnxDpt1.MY_DEVICE_CATEGORY
					+ " driver to groupDevice " + this.groupDevice.getGroupAddress() + ". No appropriate " +
					"ISO device created!");
			return null;
		}
		

		/** create appropriate ISO device */
		if ( this.isoDeviceCategory != null ) {

//			this.logger.log(LogService.LOG_INFO, "KNX-ISO mapping config: " + this.knxIsoMappingProperties);

//			// get knx-iso mapping properties for my groupDevice according to groupAddress
//			String isoDeviceType = (String) this.knxIsoMappingProperties.get("isoDeviceType");
//			KnxGroupDeviceCategory isoDeviceCategory = KnxGroupDeviceCategoryUtil.
//				toActivityHubDevice(isoDeviceType);
			
			if ( this.groupDevice.getGroupAddress() != null ) {
				// isoDeviceType configuration found
				
				this.logger.log(LogService.LOG_INFO, "KNX to ISO mapping parameter found for " +
						"groupDevice " + this.groupDevice.getGroupAddress() + " with KNX datapoint type " +
						this.groupDevice.getDatapointType() + " : " +isoDeviceCategory);

//				// check deviceLocation property
//				String loc = (String) this.knxIsoMappingProperties.get("deviceLocation");
//				ActivityHubLocation isoDeviceLocation = ActivityHubLocationUtil.
//					toActivityHubLocation(loc);
//				if (isoDeviceLocation == null) {
//					this.logger.log(LogService.LOG_WARNING, "Location for KNX groupDevice " +
//							this.device.getGroupAddress() + " not found!");
//				}
				

				this.logger.log(LogService.LOG_WARNING, "Mapping of locations from ETS config " +
						"to ISO defined ActivityHubLocation is not in place yet! ");
//				+
//						"Location info from ETS:: Name: " + this.groupDevice.getDeviceLocation() +
//						"; Type: " + this.groupDevice.getDeviceLocationType() +
//						"; Description: " + this.groupDevice.getDeviceLocationDescription());

				
				// create appropriate ActivityHub groupDevice
				this.activityHubSensor = ActivityHubFactory.createInstance(
						isoDeviceCategory,
//						isoDeviceLocation,
						null,
						this.groupDevice.getGroupAddress(),this.logger);
				if (this.activityHubSensor==null) {
					this.logger.log(LogService.LOG_ERROR, "Error on creating ActivityHubSensor " +
							" for device category: " + isoDeviceCategory + " with deviceId: " + 
							this.groupDevice.getGroupAddress());
					return null;
				}
				
//				// set instance alive
//				// use knx group address for now as groupDevice ID !
//				ahd.setParams(isoDeviceType,this.device.getGroupAddress(),this.logger);
				
				// register AH device in OSGi registry
				Properties propDeviceService=new Properties();

				// use properties value as device category
				propDeviceService.put(
						org.osgi.service.device.Constants.DEVICE_CATEGORY, isoDeviceCategory.toString());
				// more possible properties from OSGi: description, serial, id
				
//				this.myIsoDeviceRegistration = 
					this.context.registerService(
						org.osgi.service.device.Device.class.getName(), this.activityHubSensor, 
						propDeviceService);
				
				this.logger.log(LogService.LOG_INFO, "Registered ISO device " +
						this.activityHubSensor.getDeviceId() + " in OSGi registry under " +
						"device category: " + isoDeviceCategory);
				
			} else {
				String s1 = "KNX group address is null for groupDevice " + this.groupDevice.getGroupAddress();
				this.logger.log(LogService.LOG_ERROR, s1);
				throw new NullPointerException(s1);
			}
		} else {
			String s2 = "No configuration parameter found for Knx to ISO mapping for groupDevice " +
				this.groupDevice.getGroupAddress() + "; isoDeviceType=null";
			this.logger.log(LogService.LOG_ERROR, s2);
			throw new NullPointerException(s2);
		}
		return knxDev;
	}

	
	/**
	 * @see org.universAAL.lddi.knx.groupdevicecategory.IKnxDpt1.devicecategory.KnxDpt1#newMessageFromKnxBus(byte[])
	 * got new message from knx bus
	 * pass to ISO device
	 */
	public void newMessageFromKnxBus(byte[] event) {
		// try to display event byte readable. No good: Byte.toString(byte), Integer.toHexString(byte)
		this.logger.log(LogService.LOG_INFO, "Driver " + IKnxDpt1.MY_DEVICE_CATEGORY + " for groupDevice " + 
				this.groupDevice.getGroupAddress() + " with knx datapoint type " + this.groupDevice.getDatapointType() +
				" received new knx message " + 
				KnxEncoder.convertToReadableHex(event));
//				String.format("%02X", event));

		if (this.activityHubSensor != null){
		
//			// map on dpt!!
//			String dptString = "_" + this.device.getDatapointType().replace('.', '_');
//			this.logger.log(LogService.LOG_INFO, "Datapoint type String: " + dptString); 
//			
//			String constName = IKnxDpt1.DEFAULT_VALUE_ON
//			
			/**
			 * KNX datapoint type 1.*** is a 1-bit signal; therefore only on/off is forwarded to ISO devices!  
			 */
			if ( event[0] == DEFAULT_VALUE_OFF ) {
				this.logger.log(LogService.LOG_INFO, "Event matches to DEFAULT_VALUE_OFF");
				this.activityHubSensor.setSensorEventOff();
			} else if ( event[0] == DEFAULT_VALUE_ON ) {
				this.logger.log(LogService.LOG_INFO, "Event matches to DEFAULT_VALUE_ON");
				this.activityHubSensor.setSensorEventOn();
			} else {
				this.logger.log(LogService.LOG_ERROR, "No matches on incoming Event " + Integer.toHexString(event[0]) +
						" from groupDevice " + this.groupDevice.getGroupAddress());
				return;
			}
		} else {
			this.logger.log(LogService.LOG_ERROR, "Driver " + IKnxDpt1.MY_DEVICE_CATEGORY + " for groupDevice " + 
					this.groupDevice.getGroupAddress() + " lost its mapping ISO11073 sensor! " +
							"Cannot forward incoming message!");
		}
	}

	/**
	 * @param reference KNX groupDevice service
	 */
	public void removedService(ServiceReference reference, Object service) {
		// removed groupDevice service
		this.context.ungetService(reference);
		this.detachDriver();
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		this.logger.log(LogService.LOG_INFO, "Tracked knx groupDevice service was modified. Going to update the KnxDpt1Instance");
		removedService(reference, service);
		addingService(reference);			
	}


//	/**
//	 * Managed associated ISO devices according to configuration change.
//	 * Maybe destroy old ISO object and create new one.
//	 * 
//	 * @param properties 
//	 * @return true if successful
//	 */
//	public boolean updateConfiguration(Dictionary properties) {
//		return true;
//	}

}

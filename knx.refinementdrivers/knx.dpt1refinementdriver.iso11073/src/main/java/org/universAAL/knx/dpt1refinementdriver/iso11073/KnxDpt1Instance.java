package org.universAAL.knx.dpt1refinementdriver.iso11073;

import it.polito.elite.domotics.dog2.knxnetworkdriver.interfaces.KnxDriver;
import it.polito.elite.domotics.dog2.knxnetworkdriver.interfaces.KnxNetwork;

import java.util.Dictionary;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.device.Constants;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.universAAL.iso11073.activityhub.ActivityHubFactory;
import org.universAAL.iso11073.activityhub.ActivityHubSensor;
import org.universAAL.knx.devicecategory.KnxDpt1;
import org.universAAL.knx.devicemodel.KnxDevice;
import org.universAAL.knx.devicemodel.KnxDpt1Device;

/**
 * Working instance of the KnxDpt1 driver. Registers a service/device.
 * Tracks on the KNX device service passed in the attach method in KnxDpt1RefinementDriver class. 
 * When the KNX device disappears, this service/device is unregistered.
 *  
 * @author Thomas Fuxreiter (foex@gmx.at)
 */
public class KnxDpt1Instance extends KnxDriver implements KnxDpt1
//, ManagedService 
,ServiceTrackerCustomizer, Constants {

//	private static final String KNX_DRIVER_CONFIG_NAME = "knx.refinementdriver";
	private BundleContext context;
	private LogService logger;
	private Dictionary knxIsoMappingProperties;
//	private Dictionary<String,String> knxIsoMappingProperties;
	
	// the iso device I registered at osgi reg
	private ServiceRegistration myIsoDeviceRegistration;

	
	// called from the driver.attach method
	public KnxDpt1Instance(BundleContext c, ServiceReference sr, KnxNetwork network, 
			LogService log) throws InvalidSyntaxException {

		//initialize knxdriver and service tracker
//		super(c,sr,null);
//		super(network, c, sr);
		super(network);
//		open();
		
		this.context=c;
		this.logger=log;

//		this.logger.log(LogService.LOG_ERROR, "Hello Driver Instance!");

		
		//		this.knxIsoMappingProperties = knxIsoMappingProperties;
		
//		this.registerManagedService();
		
//		ServiceTracker st=new ServiceTracker(c,sr, this);
//		st.open();
	}

	public void setKnxIsoMappingProperties(Dictionary dictionary) {
		this.knxIsoMappingProperties = dictionary;
//		this.knxIsoMappingProperties = new Properties();
//		
//		// groupAddress config format is "A-B-C"; change to "A/B/C"
//		Enumeration<String> en = knxIsoMappingProp.keys(); 
//		while (en.hasMoreElements()) {
//			String key = en.nextElement();
//			String newKey = key.replace('-', '/');
//
//			String value = knxIsoMappingProp.get(key);
//			// convert key to char[] because Felix ConfigurationManager says '/' is an illegal character
//			this.knxIsoMappingProperties.put(newKey, value);
//		}
	}
	
	
//	/**
//	 * Register this class as Managed Service
//	 */
//	public void registerManagedService() {
//		this.logger.log(LogService.LOG_ERROR, "Register managed service!");
//
//		Properties propManagedService=new Properties();
//		propManagedService.put(org.osgi.framework.Constants.SERVICE_PID, 
//				KNX_DRIVER_CONFIG_NAME);
//		this.context.registerService(ManagedService.class.getName(), this, propManagedService);
//	}

	/*** ServiceTracker ***/
	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
//	@Override
	public Object addingService(ServiceReference reference) {
//		Object o = super(reference);
		// device service was found
		
		// get device service
//		if ( this.context != null) this.logger.log(LogService.LOG_ERROR, "context is not null: "
//				+ this.context);
//		else this.logger.log(LogService.LOG_ERROR, "context is null!");
//		
//		if ( reference != null) this.logger.log(LogService.LOG_ERROR, "reference is not null: "
//				+ reference);
//		else this.logger.log(LogService.LOG_ERROR, "reference is null!");

		
		KnxDpt1Device knxDev = (KnxDpt1Device) this.context.getService(reference);
		
		// register driver in knx.network driverList
		this.setDevice( (KnxDevice) knxDev);
		
		
		
		// knx device and knx driver are coupled now
		// Regeln f�r die decodierung in device_category ??
		
		
		// passendes ISO device erzeugen..........................
		if ( !this.knxIsoMappingProperties.isEmpty() && this.device.getGroupAddress() != null ) {

//			this.logger.log(LogService.LOG_INFO, "KNX-ISO mapping config: " + this.knxIsoMappingProperties);

			// get knx-iso mapping properties for my device according to groupAddress
			String isoDeviceType = (String) this.knxIsoMappingProperties.get("isoDeviceType");
			
			if (isoDeviceType != null) {
				// isoDeviceType configuration found
				
				this.logger.log(LogService.LOG_INFO, "KNX - ISO mapping parameter found for device " + this.device.getGroupAddress() + 
						": " + isoDeviceType);

				// na dann erzeug ma jetzt ein iso device nach dem isoDeviceType.........
				// dazu w�r wohl eine Factory angesagt
				
				// create appropriate ActivityHub device
				ActivityHubSensor ahs = ActivityHubFactory.createInstance(isoDeviceType);
				
				// set instance alive
				// use knx group address for now as device ID !
				ahs.setParams(isoDeviceType,this.device.getGroupAddress(),this.logger);
				
				// register AH device in OSGi registry
				Properties propDeviceService=new Properties();

				// use properties value as device category
				propDeviceService.put(
						org.osgi.service.device.Constants.DEVICE_CATEGORY, isoDeviceType);
				// more possible properties from OSGi: description, serial, id
				
				this.logger.log(LogService.LOG_INFO, "Register ISO device " +
						ahs.getDeviceId() + " in OSGi registry under " +
						"device category: " + isoDeviceType);
				
				this.myIsoDeviceRegistration = this.context.registerService(
						org.osgi.service.device.Device.class.getName(), ahs, 
						propDeviceService);
				
			} else {
				this.logger.log(LogService.LOG_ERROR, "No configuration parameter found for Knx" +
						" to Iso device mapping for knx device " + this.device.getGroupAddress());
			}
		} else {
			this.logger.log(LogService.LOG_ERROR, "No configuration parameter found for " +
					"Knx to ISO mapping!");
		}
		return knxDev;
	}

	
	/* (non-Javadoc)
	 * @see it.polito.elite.domotics.dog2.knxnetworkdriver.interfaces.KnxDriver#setDevice(org.universAAL.knx.devicemodel.KnxDevice)
	 */
//	@Override
//	public void setDevice(KnxDevice device) {
//		this.device = device;
//		this.network.addDriver(this.device.getGroupAddress(), this);
//	}

//	@Override
	public void removedService(ServiceReference reference, Object service) {
		// removed device service
		this.context.ungetService(reference);
		this.removeDriver();
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
//	@Override
	public void modifiedService(ServiceReference reference, Object service) {
		this.logger.log(LogService.LOG_INFO, "Tracked knx device service was modified. Going to update the KnxDpt1Instance");
		removedService(reference, service);
		addingService(reference);			
	}
	
	
	
	/*** from device category ***/
	/* (non-Javadoc)
	 * @see org.universAAL.knx.devicecategory.KnxDpt1#receivePacket(long)
	 */
	public byte[] receivePacket(long timeout) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.universAAL.knx.devicecategory.KnxDpt1#sendPacket(byte[])
	 */
	public void sendPacket(byte[] data) {
		// TODO Auto-generated method stub

	}

	
	/*** from KnxDriver ***/
	/* (non-Javadoc)
	 * @see it.polito.elite.domotics.dog2.knxnetworkdriver.interfaces.KnxDriver#newMessageFromHouse(java.lang.String, byte[])
	 */
	@Override
	public void newMessageFromHouse(String deviceAddress, byte[] message) {
		
		// und wos tua ma jetzt??
		
		this.logger.log(LogService.LOG_INFO, "Incoming message " + message + " from address " + 
				deviceAddress);
		
		
	}

	/* (non-Javadoc)
	 * @see it.polito.elite.domotics.dog2.knxnetworkdriver.interfaces.KnxDriver#specificConfiguration()
	 */
	@Override
	protected void specificConfiguration() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Managed associated ISO devices according to configuration change.
	 * Maybe destroy old ISO object and create new one.
	 * 
	 * @param properties 
	 * @return true if successful
	 */
	public boolean updateConfiguration(Dictionary properties) {
		// TODO Auto-generated method stub
		return true;
		
	}

	/**
	 * Unregister associated services and destroy me
	 */
	public void destroy() {
		// TODO Auto-generated method stub
		
	}

//	/* (non-Javadoc)
//	 * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
//	 */
//	public void updated(Dictionary properties) throws ConfigurationException {
//		this.logger.log(LogService.LOG_INFO, "KnxDpt1Driver updated. " +
//				"Mapping from KNX device to ISO 11073 device. " + properties);
//
//		if (properties != null) {
//			this.knxIsoMappingProperties = properties;
//		} else {
//			this.logger.log(LogService.LOG_ERROR, "No configuration found for Knx to ISO device mapping!");
//		}
//			
//	}



}
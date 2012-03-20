package it.polito.elite.domotics.dog2.knxnetworkdriver;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.osgi.service.log.LogService;

/** Provides readings from the knx gateway by the LAN.
 * Uses the encoder to operate translation from low level data
 * (knx) to high level data (sent to ?).
 * @author Enrico Allione (enrico.allione@gmail.com)
 * @author Thomas Fuxreiter (foex@gmx.at)
 */

public class KnxReader extends Thread {

	protected KnxNetworkDriverImp core;
	protected KnxEncoder encoder;
	
	static private int socketTimeout = 0;	// infinite timeout on receive()
	static private int telegramLenght = 15;	// KNX core telegram length

	private boolean running;

	public KnxReader(KnxNetworkDriverImp core) {
		this.core = core;
		this.running=true;
	}

	public void stopReader(){
		this.running=false;
	}
	
	public void run()  {
		while(running){
			listen();
			Thread.yield();
		}
	}

	private void listen(){
		int k = 0;
		boolean flag = true;

		try {
			MulticastSocket mcReceiver = new MulticastSocket(core.getMyUdpPort());
			InetAddress group = InetAddress.getByName(core.getMulticastIp());
			mcReceiver.joinGroup(group);

			mcReceiver.setSoTimeout(socketTimeout);

			core.getLogger().log(LogService.LOG_INFO,"Server KNX listening on port " + 
					core.getMyUdpPort() + " (joined " + core.getMulticastIp() + ")");

			while (flag) {
				byte buffer[] = new byte[mcReceiver.getReceiveBufferSize()];
				DatagramPacket udpPacket = new DatagramPacket(buffer, buffer.length);
				mcReceiver.receive(udpPacket);
				//The datagram packet contains also the sender's IP address, and the port number
				//on the sender's machine. This method blocks until a datagram is received.

				byte[]temp = udpPacket.getData();
				core.getLogger().log(LogService.LOG_INFO,"KNX telegram received: " + KnxEncoder.decode(temp));
						
				byte[] deviceByte = new byte[2];
				
				//difference between the aspected size of the telegram and the real one
				int oversize=temp.length-KnxReader.telegramLenght;
				oversize=oversize<0?0:oversize; //avoid array index exception
				
				
				byte[] statusByte = new byte[oversize+2];
				deviceByte[0] = temp[10];		
				deviceByte[1] = temp[11];
				for(int i=0; i<statusByte.length; i++){
					//statusByte[0] = temp[14]; //old version
					statusByte[i]=temp[temp.length-2-oversize+i];
				}
				//TODO change to handle data value such as temperature
				String knxDevice = KnxEncoder.getGroupAddress(deviceByte);
				//String knxStatus = KnxEncoder.getStatus(statusByte);
				
				core.getLogger().log(LogService.LOG_DEBUG,"\n---------COMMAND FROM HOUSE TO DOG--------- " + udpPacket.getAddress().toString()+"BYTE "+KnxWriter.byteArrayToHexString(temp));
				
				 //byteString=new StringBuilder();
				
				

				k += 1;
				core.getLogger().log(LogService.LOG_DEBUG,"Source: " + knxDevice + "; TELEGRAM: " +KnxWriter.byteArrayToHexString(temp));

				this.core.newMessageFromHouse(knxDevice, statusByte);
			
			}
		}
		catch (Exception e){
			core.getLogger().log(LogService.LOG_ERROR,e.getMessage());
			// e.printStackTrace();
		}
	}



}
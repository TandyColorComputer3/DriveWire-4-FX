package com.groupunix.drivewireserver.virtualserial;



import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.dwexceptions.DWConnectionNotValidException;
import com.groupunix.drivewireserver.dwexceptions.DWPortNotValidException;
import com.groupunix.drivewireserver.dwprotocolhandler.DWVSerialProtocol;

public class DWVPortTCPServerThread implements Runnable {

	private static final Logger logger = Logger.getLogger("DWServer.DWVPortTCPServerThread");
	
	private int vport = -1;

	private int conno;
	private boolean wanttodie = false;
	private int mode = 0;
	private DWVSerialPorts dwVSerialPorts;
	
	private static final int MODE_TELNET = 1;
	private static final int MODE_TERM = 3;

	private SocketChannel sktchan;
	
	
	public DWVPortTCPServerThread(DWVSerialProtocol dwProto, int vport, int conno) throws DWConnectionNotValidException
	{
		logger.debug("init tcp server thread for conn " + conno);	
		this.vport = vport;
		this.conno = conno;

		this.dwVSerialPorts = dwProto.getVPorts();
		this.mode = this.dwVSerialPorts.getListenerPool().getMode(conno);
		this.sktchan = this.dwVSerialPorts.getListenerPool().getConn(conno);
		
		
	}
	

	public void run() 
	{
		Thread.currentThread().setName("tcpserv-" + Thread.currentThread().getId());
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
		
		try
		{
		
			// setup ties
			this.dwVSerialPorts.getListenerPool().setConnPort(this.conno, this.vport);
	
			dwVSerialPorts.setConn(this.vport,this.conno);
		
		
		logger.info("TCP server thread running for conn " + this.conno + " on port " + this.vport);
			
		if (sktchan == null)
		{
			logger.error("got a null socket, bailing out");
			return;
		}
		
		// Ensure port is open before starting data flow
		try {
			if (!dwVSerialPorts.isOpen(vport))
			{
				logger.info("Port " + vport + " not open, opening it now");
				dwVSerialPorts.openPort(vport);
			}
			logger.info("Port " + vport + " is open, starting data flow");
		} catch (Exception e) {
			logger.error("Failed to open port " + vport + ": " + e.getMessage(), e);
			return;
		}
		
		// 	set pass through mode
		dwVSerialPorts.markConnected(vport);	
		dwVSerialPorts.setUtilMode(vport, DWDefs.UTILMODE_TCPIN);
		dwVSerialPorts.setPortChannel(vport, sktchan);
		
		logger.info("Port " + vport + " configured for TCP pass-through, starting data loop");
		
		int lastbyte = -1;
		
		while ((wanttodie == false) && (sktchan.isOpen()) && (dwVSerialPorts.isOpen(this.vport) || (mode == MODE_TERM)))
			{
			
				int databyte = sktchan.socket().getInputStream().read();
				if (databyte == -1)
				{
					logger.info("TCP connection closed (EOF) for conn " + this.conno);
					wanttodie = true;
				}
				else
				{
					if (logger.isDebugEnabled())
					{
						logger.debug("TCP data received: " + databyte + " (0x" + Integer.toHexString(databyte) + ") for port " + this.vport);
					}
					// filter CR,NULL if in telnet or term mode unless PD.INT and PD.QUT = 0
					if (((mode == MODE_TELNET) || (mode == MODE_TERM)) && ((dwVSerialPorts.getPD_INT(this.vport) != 0) || (dwVSerialPorts.getPD_QUT(this.vport) != 0)))
					{
						// logger.debug("telnet in : " + databyte);
						// TODO filter CR/LF.. should do this better
						if (!((lastbyte == 13) && ((databyte == 10) || (databyte == 0))))
						{
							// write it to the serial port
							// logger.debug("passing : " + databyte);
							dwVSerialPorts.writeToCoco(this.vport,(byte)databyte);
							lastbyte = databyte;
						}
					}
					else
					{
						logger.info("TCP data received: " + databyte + " (0x" + Integer.toHexString(databyte) + ") for port " + this.vport);
						dwVSerialPorts.writeToCoco(this.vport,(byte)databyte);
						logger.info("Data written to virtual serial port " + this.vport + " input buffer");
					}
				}				
			}
			
			dwVSerialPorts.markDisconnected(this.vport);
			dwVSerialPorts.setPortChannel(vport, null);
			
		
			// 	only if we got connected.. and its not term
			if ((sktchan != null) && (mode != MODE_TERM))
			{
				if (sktchan.isConnected())
				{
		
					logger.debug("exit stage 1, flush buffer");
		
					// 	flush buffer, term port
					try 
					{
						while ((dwVSerialPorts.bytesWaiting(this.vport) > 0) && (dwVSerialPorts.isOpen(this.vport)))
						{
							logger.debug("pause for the cause: " + dwVSerialPorts.bytesWaiting(this.vport) + " bytes left" );
							Thread.sleep(100);
						}
					} 
					catch (InterruptedException e) 
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		
					logger.debug("exit stage 2, send peer signal");
		
					dwVSerialPorts.closePort(this.vport);
				
				}
			}
			
			
		}
		 
		catch (DWPortNotValidException e) 
		{
			logger.error(e.getMessage());
		} 
		catch (IOException e) 
		{
			logger.error(e.getMessage());
		} 
		catch (DWConnectionNotValidException e) 
		{
			logger.error(e.getMessage());
		}
		
		try 
		{
			this.dwVSerialPorts.getListenerPool().clearConn(this.conno);
		} 
		catch (DWConnectionNotValidException e) 
		{
			logger.error(e.getMessage());
		}
		
		
		logger.debug("thread exiting");
	}

		
		

	public void shutdown()
	{
		logger.debug("shutting down");
		this.wanttodie = true;
		try
		{
			if (this.sktchan != null)
				this.sktchan.close();
		} 
		catch (IOException e)
		{
			logger.warn("IOException while closing socket: " + e.getMessage());
		}
	}	
	
	
	
	
	
}

	
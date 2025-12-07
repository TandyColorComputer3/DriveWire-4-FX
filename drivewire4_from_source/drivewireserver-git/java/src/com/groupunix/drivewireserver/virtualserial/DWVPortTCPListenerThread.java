package com.groupunix.drivewireserver.virtualserial;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.dwexceptions.DWPortNotValidException;
import com.groupunix.drivewireserver.dwprotocolhandler.DWVSerialProtocol;

public class DWVPortTCPListenerThread implements Runnable 
{

	private static final Logger logger = Logger.getLogger("DWServer.DWVPortTCPListenerThread");
	
	private int vport;
	private int tcpport;

	private DWVSerialPorts dwVSerialPorts;
	
	private int mode = 0;
	private boolean do_banner = false;
	private boolean do_telnet = false;
	private boolean wanttodie = false;
	
	private static int BACKLOG = 20;
	private DWVSerialProtocol dwProto;
	
	
	
	
	public DWVPortTCPListenerThread(DWVSerialProtocol dwProto2, int vport, int tcpport)
	{
		logger.debug("init tcp listener thread on port "+ tcpport);	
		this.vport = vport;
		this.tcpport = tcpport;
		this.dwProto = dwProto2;
		this.dwVSerialPorts = dwProto2.getVPorts();
		
	}
	
	
	
	public void run() 
	{
		
		Thread.currentThread().setName("tcplisten-" + Thread.currentThread().getId());
		Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
		
		logger.debug("run");
		
		
		try 
		{
			// startup server 
			ServerSocketChannel srvr = ServerSocketChannel.open();
			
			
			try
			{
				InetSocketAddress sktaddr = new InetSocketAddress(this.tcpport);
				
				srvr.socket().setReuseAddress(true);
				srvr.socket().bind(sktaddr, BACKLOG);
				
				/*
				if (dwProto.getConfig().containsKey("ListenAddress"))
				{
					srvr = new ServerSocket(this.tcpport, BACKLOG, InetAddress.getByName(dwProto.getConfig().getString("ListenAddress")) );
				}
				else
				{
					srvr = new ServerSocket(this.tcpport, BACKLOG);
				}
				*/
				
				this.dwVSerialPorts.getListenerPool().addListener(this.vport, srvr);
				
				logger.info("tcp listening on port " + srvr.socket().getLocalPort());
			}
			catch (IOException e2) 
			{
				logger.error(e2.getMessage());
				dwVSerialPorts.sendUtilityFailResponse(this.vport, DWDefs.RC_NET_IO_ERROR, e2.getMessage());
				return;
			} 
			
			dwVSerialPorts.writeToCoco(vport, "OK listening on port " + this.tcpport + (char) 10 + (char) 13);

			this.dwVSerialPorts.setUtilMode(vport, DWDefs.UTILMODE_TCPLISTEN);
			
			
		
			while ((wanttodie == false) && dwVSerialPorts.isOpen(this.vport) && (srvr.isOpen()) && (srvr.socket().isClosed() == false))
			{
				logger.debug("waiting for connection");
				SocketChannel skt = srvr.accept();
				
			
				logger.info("new connection from " + skt.socket().getInetAddress());
			
				this.dwVSerialPorts.getListenerPool().addConn(this.vport, skt, mode);
				
				
				if (mode == 2)
				{
					// http mode
					logger.error("HTTP MODE NO LONGER SUPPORTED");
			
				}
			else
			{
				// For raw DriveWire connections (like HDB-DOS), skip telnet preflight and add connection directly
				// Telnet preflight interferes with raw DriveWire protocol
				if (this.do_telnet || this.do_banner)
				{
					// run telnet preflight, let it add the connection to the pool if things work out
					Thread pfthread = new Thread(new DWVPortTelnetPreflightThread(this.dwProto, this.vport, skt, this.do_telnet, this.do_banner));
					pfthread.start();
				}
				else
				{
					// Raw DriveWire connection - add directly to pool without telnet negotiation
					logger.debug("Adding raw DriveWire connection directly to pool (no telnet preflight)");
					try
					{
						int conno = this.dwVSerialPorts.getListenerPool().addConn(this.vport, skt, mode);
						if (conno < 0)
						{
							logger.error("Connection pool full, cannot add raw connection for port " + this.vport);
							skt.close();
							continue; // Skip to next connection
						}
						
						// Start the TCP server thread to handle data flow for this connection
						// This thread reads from TCP socket and writes to virtual serial port (and vice versa)
						try
						{
							logger.info("Starting TCP server thread for raw DriveWire connection #" + conno + " on port " + this.vport);
							DWVPortTCPServerThread serverThread = new DWVPortTCPServerThread(this.dwProto, this.vport, conno);
							Thread tcpServerThread = new Thread(serverThread);
							tcpServerThread.setDaemon(true);
							tcpServerThread.setName("tcpserv-raw-" + conno);
							tcpServerThread.start();
							logger.info("Started TCP server thread for raw DriveWire connection #" + conno + " (thread: " + tcpServerThread.getName() + ")");
						}
						catch (Exception serverThreadEx)
						{
							logger.error("Failed to start TCP server thread for connection #" + conno + ": " + serverThreadEx.getMessage(), serverThreadEx);
							// Clean up the connection if we can't start the handler
							try {
								skt.close();
								this.dwVSerialPorts.getListenerPool().clearConn(conno);
							} catch (Exception cleanupEx) {
								logger.error("Error cleaning up failed connection: " + cleanupEx.getMessage());
							}
							continue; // Skip to next connection
						}
						
						// Only send announcement if we have valid socket info and port handler is initialized
						// Announcement is optional - connection will work without it
						try
						{
							if (skt.socket() != null && skt.socket().getInetAddress() != null)
							{
								this.dwVSerialPorts.sendConnectionAnnouncement(this.vport, conno, skt.socket().getLocalPort(), skt.socket().getInetAddress().getHostAddress());
								logger.info("Raw DriveWire connection added: connection #" + conno + " from " + skt.socket().getInetAddress().getHostAddress());
							}
							else
							{
								logger.info("Raw DriveWire connection added: connection #" + conno + " (socket info unavailable)");
							}
						}
						catch (Exception annEx)
						{
							// Announcement failed but connection is still valid - log and continue
							logger.debug("Could not send connection announcement (port handler may not be initialized): " + annEx.getMessage());
							logger.info("Raw DriveWire connection added: connection #" + conno + " from " + (skt.socket() != null && skt.socket().getInetAddress() != null ? skt.socket().getInetAddress().getHostAddress() : "unknown"));
						}
					}
					catch (Exception e)
					{
						logger.error("Error adding raw connection: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()), e);
						try {
							if (skt.isOpen() && skt.isConnected())
							{
								skt.close();
							}
						} catch (IOException e1) {
							logger.error("Error closing socket: " + e1.getMessage());
						}
					}
				}
			}
			
			
			}
		
			if (srvr != null)
			{
				try 
				{
					srvr.close();
				} 
				catch (IOException e) 
				{
					logger.error("error closing server socket: " + e.getMessage());
				}
			}
			
		} 
		catch (IOException e2) 
		{
			logger.error(e2.getMessage());
		} 
		catch (DWPortNotValidException e) 
		{
			logger.error(e.getMessage());
		}
	
		
		logger.debug("tcp listener thread exiting");
	}

	

	




	

	public void setDo_banner(boolean do_banner)
	{
		this.do_banner = do_banner;
	}

	public boolean isDo_banner()
	{
		return do_banner;
	}

	
	public void setMode(int mode)
	{
		this.mode = mode;
	}
	
	public int getMode()
	{
		return(this.mode);
	}

	public void setDo_telnet(boolean b)
	{
		this.do_telnet = b;
		
	}
	
	public boolean isDo_telnet()
	{
		return do_telnet;
	}
	
}

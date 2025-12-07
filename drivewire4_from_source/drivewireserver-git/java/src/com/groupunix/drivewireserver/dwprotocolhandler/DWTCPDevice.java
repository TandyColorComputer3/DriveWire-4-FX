package com.groupunix.drivewireserver.dwprotocolhandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.apache.log4j.Logger;

import com.groupunix.drivewireserver.DriveWireServer;

public class DWTCPDevice implements DWProtocolDevice {

	private static final Logger logger = Logger.getLogger("DWServer.DWTCPDevice");
	private int tcpport;
	private int handlerno;
	private ServerSocket srvr;
	private Socket skt = null;
	private boolean bytelog = false;
	private String client = null;
	private int totalBytesRead = 0; // Track total bytes read for diagnostic logging
	
	public DWTCPDevice(int handlerno, int tcpport) throws IOException 
	{
		this.handlerno = handlerno;
		this.tcpport = tcpport;
		
		bytelog = DriveWireServer.getHandler(this.handlerno).getConfig().getBoolean("LogDeviceBytes",false);
		
		logger.debug("init tcp device server on port " + tcpport + " for handler #" + handlerno + " (logging bytes: " + bytelog + ")");
		
		// check for listen address
			
		if (DriveWireServer.getHandler(this.handlerno).getConfig().containsKey("ListenAddress"))
		{
			srvr = new ServerSocket(this.tcpport, 0, InetAddress.getByName(DriveWireServer.getHandler(this.handlerno).getConfig().getString("ListenAddress")) );
		}
		else
		{
			srvr = new ServerSocket(this.tcpport, 0);
		}
		
		// CRITICAL: Set initial timeout on ServerSocket to prevent blocking on first accept()
		// This ensures the protocol handler loop can start immediately without waiting for a client
		srvr.setSoTimeout(5000);
		
		logger.info("listening on port " + srvr.getLocalPort() + " (with 5-second accept timeout)");
		
	}


	public void close() 
	{
		logger.debug("closing tcp device in handler #" + this.handlerno);
		
		closeClient();
		
		try 
		{
			srvr.close();
		} 
		catch (IOException e) 
		{
			logger.debug(e.getMessage());
		}
		
	}

	
	
	private void closeClient() 
	{
		logger.debug("closing client connection");
		
		if ((skt != null) && (!skt.isClosed()))
		{
			try 
			{
				skt.close();
			} 
			catch (IOException e) 
			{
				logger.debug(e.getMessage());
			}
		}
		
		client = null;
		skt = null;
	}


	public byte[] comRead(int len) throws IOException 
	{

		byte[] buf = new byte[len];
		
		for (int i = 0;i<len;i++)
		{
			buf[i] = (byte) comRead1(true);
		}
		
		return(buf);

	}

	
	public int comRead1(boolean timeout) throws IOException 
	{
		int data = -1;
		
		if (skt == null)
		{
			getClientConnection();
			
			if (skt == null)
			{
				return -1;
			}
		}
		
		if (skt != null)
		{
			// Check if socket is still connected
			if (skt.isClosed() || !skt.isConnected())
			{
				logger.warn("Socket is closed or not connected, closing client");
				closeClient();
				// Try to get a new connection
				if (skt == null)
				{
					getClientConnection();
				}
			}
			
			if (skt != null)
			{
				try 
				{
					data = skt.getInputStream().read();
					
					if (bytelog)
						logger.debug("TCPREAD: " + data + " (0x" + Integer.toHexString(data) + ")");
				} 
				catch (IOException e) 
				{
					logger.warn("IOException reading from TCP device: " + e.getMessage());
					closeClient();
					return -1;
				}
				
				if (data < 0)
				{
					// EOF - client disconnected
					logger.info("Client disconnected (EOF), closing connection");
					closeClient();
					return -1;
				}
			}
		}
		
		return data;
	}

	
	
	public void comWrite(byte[] data, int len, boolean prefix) 
	{
		if ((skt != null) && (!skt.isClosed()) && skt.isConnected())
		{
			try 
			{
				skt.getOutputStream().write(data, 0, len);
				skt.getOutputStream().flush(); // Ensure data is sent immediately
				
				if (bytelog)
				{
					String tmps = new String();
				
					for (int i = 0;i< len;i++)
					{
						tmps += " " + (int)(data[i] & 0xFF);
					}
				
					logger.debug("TCPWRITE " + len + " bytes:" + tmps);
				}
				else
				{
					logger.trace("TCPWRITE " + len + " bytes");
				}
			} 
			catch (IOException e) 
			{
				logger.error("IOException writing to TCP device: " + e.getMessage());
				closeClient();
			}
		}
		else
		{
			logger.warn("Cannot write to TCP device: socket is null, closed, or not connected");
		}
	}

	
	public void comWrite1(int data, boolean prefix) 
	{
		if ((skt != null) && (!skt.isClosed()) && skt.isConnected())
		{
			try 
			{
				skt.getOutputStream().write((byte) data);
				skt.getOutputStream().flush(); // Ensure data is sent immediately
				
				if (bytelog)
					logger.debug("TCPWRITE1: " + data + " (0x" + Integer.toHexString(data) + ")");
				else
					logger.trace("TCPWRITE1: " + data + " (0x" + Integer.toHexString(data) + ")");
			} 
			catch (IOException e) 
			{
				logger.error("IOException writing single byte to TCP device: " + e.getMessage());
				closeClient();
			}
		}
		else
		{
			logger.warn("Cannot write single byte to TCP device: socket is null, closed, or not connected");
		}
	}


	public boolean connected() 
	{
		if (skt == null)
			return false;
		
		return true;
	}


	public void shutdown() 
	{
			
		close();
	}

	private void getClientConnection()
	{
		// Only log once when first waiting for connection
		if (skt == null) {
			logger.debug("Waiting for client connection on port " + this.tcpport + "...");
		}
		
		try 
		{
			// MANDATORY: Ensure 5-second timeout is set on accept() to prevent blocking protocol handler loop
			// This is CRITICAL - without it, accept() blocks indefinitely and breaks connection flow
			// The timeout is already set in constructor, but ensure it's still set (in case it was reset)
			if (srvr.getSoTimeout() != 5000) {
				srvr.setSoTimeout(5000);  // 5 seconds - REQUIRED
			}
			skt = srvr.accept();
			// Keep timeout at 5000 for next connection attempt (if client disconnects)
			// This ensures we never block on accept() - CRITICAL for protocol handler loop
			srvr.setSoTimeout(5000);
		} 
		catch (java.net.SocketTimeoutException e)
		{
			// Timeout is expected - no client connected yet, return and let loop continue
			// Debug level, NOT error - this happens frequently during normal operation
			logger.debug("No client connection yet on port " + this.tcpport + ", will retry...");
			skt = null;
			// Timeout is already set to 5000, no need to reset
			// This allows the protocol handler loop to continue without blocking
			return;
		}
		catch (IOException e1) 
		{
			logger.error("IO error while listening for client: " + e1.getMessage());
			skt = null;
			// Reset timeout on error to ensure next attempt works
			try {
				srvr.setSoTimeout(5000);
			} catch (SocketException se) {
				logger.debug("Failed to reset ServerSocket timeout: " + se.getMessage());
			}
			return;
		}
		
		if (skt != null)
		{
			String clientAddr = skt.getInetAddress().getHostAddress();
			String clientName = skt.getInetAddress().getCanonicalHostName();
			logger.info("New client connected from " + clientAddr + " (" + clientName + ")");
			
			this.client = clientName;
			
			try 
			{
				skt.setTcpNoDelay(true);
				skt.setSoTimeout(0);  // Blocking reads after connection - REQUIRED
			} 
			catch (SocketException e) 
			{
				logger.warn("Failed to configure TCP socket options: " + e.getMessage());
			}
		}
	}
	
	public int getRate()
	{
		// doesn't make sense here?
		return(-1);
	}


	@Override
	public String getDeviceName() 
	{
		return("listen:" + this.tcpport);
	}


	@Override
	public String getDeviceType() 
	{
		return("tcp");
	}


	@Override
	public String getClient() 
	{
		return this.client;
	}


	@Override
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return null;
	}



	
}

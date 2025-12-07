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
		
		logger.info("listening on port " + srvr.getLocalPort());
		
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
			// Try to get a connection, but don't block indefinitely
			// Use a timeout to allow the protocol handler loop to continue
			getClientConnection();
			
			// If still no connection after timeout, return -1 to allow loop to continue
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
					// Log when we're about to read (first few times to confirm the handler is active)
					if (totalBytesRead < 5)
					{
						logger.info("About to read from TCP device (blocking call, waiting for data from client)...");
					}
					
					// Check if data is available before reading (non-blocking check)
					int available = skt.getInputStream().available();
					if (totalBytesRead < 5)
					{
						logger.info("Bytes available: " + available);
					}
					
					// Always try to read (blocking call - will wait for data)
					data = skt.getInputStream().read();
					
					if (bytelog)
						logger.debug("TCPREAD: " + data + " (0x" + Integer.toHexString(data) + ")");
					else if (data >= 0 && data < 256) // Log first few bytes to help diagnose
					{
						// Log first 20 bytes received to help diagnose connection issues
						if (totalBytesRead < 20)
						{
							logger.info("TCPREAD (first bytes): " + data + " (0x" + Integer.toHexString(data) + ") - total bytes read: " + (totalBytesRead + 1));
							totalBytesRead++;
						}
					}
				} 
				catch (IOException e) 
				{
					logger.warn("IOException reading from TCP device: " + e.getMessage());
					closeClient();
					// Return -1 to indicate no data available, don't recurse
					return -1;
				}
				
				if (data < 0)
				{
					// EOF - client disconnected
					logger.info("Client disconnected (EOF), closing connection");
					closeClient();
					// Return -1 instead of recursing to avoid infinite loop
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
		// Only log once per connection attempt, not on every retry
		// Use a static flag or check if we've logged recently
		if (skt == null) {
			logger.info("Waiting for client connection on port " + this.tcpport + "...");
		}
		
		try 
		{
			// Set a timeout on the ServerSocket to prevent indefinite blocking
			// This allows the protocol handler loop to continue and check other conditions
			// Use a longer timeout (5 seconds) to avoid interfering with normal connections
			// but still allow the handler to continue if no client connects
			srvr.setSoTimeout(5000); // 5 second timeout
			skt = srvr.accept();
			// Reset timeout after accepting (or it will timeout on next accept)
			srvr.setSoTimeout(0); // Remove timeout after connection established
		} 
		catch (java.net.SocketTimeoutException e)
		{
			// Timeout is expected - no client connected yet, return and let the loop continue
			// Don't log this as it happens frequently - only log at debug level
			logger.debug("No client connection yet on port " + this.tcpport + ", will retry...");
			skt = null; // Ensure skt is null if timeout occurs
			return;
		}
		catch (IOException e1) 
		{
			logger.error("IO error while listening for client: " + e1.getMessage());
			skt = null; // Ensure skt is null on other IO errors
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
				skt.setSoTimeout(0); // No read timeout - block until data arrives
				logger.info("TCP socket configured: NoDelay=true, SoTimeout=0");
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

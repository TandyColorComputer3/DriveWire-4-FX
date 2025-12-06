package com.groupunix.drivewireui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;


public class SyncThread implements Runnable 
{
	private static final int READ_BUFFER_SIZE = 2048;
	private static final String LINE_END = Character.toString((char) 13);
	private String host =  new String();
	private int port = -1;
	private Socket sock = null;
	private boolean wanttodie = false;
	private OutputStream out;
	private BufferedReader in;
	
	private HashMap<String, String> params = new HashMap<String, String>();
	private StringBuilder buffer = new StringBuilder(READ_BUFFER_SIZE * 2);
	private LogItem logbuf = new LogItem();
	private ServerStatusItem ssbuf = new ServerStatusItem();
	
	public SyncThread()
	{
		
	}
	
	@Override
	public void run() 
	{
		Thread.currentThread().setName("dwuiSync-" + Thread.currentThread().getId());
		System.out.println("=== SyncThread.run() STARTED ===");
		System.err.println("=== SyncThread.run() STARTED ===");
		
		char[] cbuf = new char[READ_BUFFER_SIZE];
		
		// initial sleep
		System.out.println("=== SyncThread: Waiting for MainWin to be ready... (ready=" + MainWin.isReady() + ") ===");
		System.err.println("=== SyncThread: Waiting for MainWin to be ready... (ready=" + MainWin.isReady() + ") ===");
		int waitCount = 0;
		while (!MainWin.isReady() && !wanttodie)
		{
			// let GUI open up..
			try
			{
				Thread.sleep(100);
				waitCount++;
				if (waitCount % 10 == 0) {
					System.out.println("SyncThread: Still waiting... (ready=" + MainWin.isReady() + ", count=" + waitCount + ")");
				}
			} 
			catch (InterruptedException e)
			{
				System.err.println("SyncThread: Interrupted while waiting");
				wanttodie = true;
			}
		}
		if (wanttodie) {
			System.out.println("=== SyncThread: Dying before main loop ===");
			System.err.println("=== SyncThread: Dying before main loop ===");
			return;
		}
		System.out.println("=== SyncThread: MainWin is ready, starting main loop... ===");
		System.err.println("=== SyncThread: MainWin is ready, starting main loop... ===");
		
		while (!wanttodie)
		{
			
			// change/establish connection
			String currentHost = MainWin.getHost();
			int currentPort = MainWin.getPort();
			
			// Check if we need to connect/reconnect
			// Need connection if: host is null, host changed, port changed, or socket is null
			boolean needConnection = (currentHost == null) || 
			                        (this.host == null) ||
			                        !currentHost.equals(this.host) || 
			                        (currentPort != this.port) || 
			                        (this.sock == null);
			
			System.out.println("SyncThread: Connection check - currentHost=" + currentHost + ", this.host=" + this.host + ", currentPort=" + currentPort + ", this.port=" + this.port + ", sock=" + (this.sock == null ? "null" : "connected") + ", needConnection=" + needConnection);
			System.err.println("SyncThread: Connection check - currentHost=" + currentHost + ", this.host=" + this.host + ", currentPort=" + currentPort + ", this.port=" + this.port + ", sock=" + (this.sock == null ? "null" : "connected") + ", needConnection=" + needConnection);
			
			if (!wanttodie && needConnection)
			{
				System.out.println("SyncThread: Entering connection block");
				System.err.println("SyncThread: Entering connection block");
				
				if (!(sock == null))
				{
					MainWin.addToServerLog(new LogItem("Sync: Disconnecting from server.."));
					try 
					{
						sock.close();
					} 
					catch (IOException e) 
					{
						// TODO MainWin.addToDisplay("Sync: " + e.getMessage());
					}
				}
				
				this.host = MainWin.getHost();
				this.port = MainWin.getPort();
				System.out.println("SyncThread: Set host=" + this.host + ", port=" + this.port);
				System.err.println("SyncThread: Set host=" + this.host + ", port=" + this.port);
				System.out.flush();
				System.err.flush();
								
				try 
				{
					System.out.println("=== SyncThread: STARTING TRY BLOCK ===");
					System.err.println("=== SyncThread: STARTING TRY BLOCK ===");
					System.out.flush();
					System.err.flush();
					
					System.out.println("SyncThread: About to call setConStatusTrying()");
					System.err.println("SyncThread: About to call setConStatusTrying()");
					System.out.flush();
					System.err.flush();
					MainWin.setConStatusTrying();
					System.out.println("SyncThread: setConStatusTrying() completed");
					System.err.println("SyncThread: setConStatusTrying() completed");
					System.out.flush();
					System.err.flush();
					
					// TODO MainWin.addToDisplay("Sync: Connecting to server..");
					MainWin.debug("Sync: Connecting...");
					
				    
				    // get initial state 
				    
				    // cache error meanings
					System.out.println("SyncThread: Loading error help cache...");
					try
					{
						MainWin.errorHelpCache.load();
						System.out.println("SyncThread: Error help cache loaded");
					}
					catch (DWUIOperationFailedException e)
					{
						// don't care
						System.out.println("SyncThread: Error help cache load failed (non-fatal): " + e.getMessage());
						MainWin.debug("Sync: caching error descriptions failed");
					}
				    
				    
				    
				    // Connect FIRST, then load config
				    System.out.println("SyncThread: About to create Socket to " + host + ":" + port);
				    System.err.println("SyncThread: About to create Socket to " + host + ":" + port);
				    System.out.flush();
				    System.err.flush();
				    sock = new Socket(host, port);
				    System.out.println("SyncThread: Socket created successfully");
				    System.err.println("SyncThread: Socket created successfully");
				    System.out.flush();
				    System.err.flush();
				    
					// Set up input/output streams FIRST
					this.out = sock.getOutputStream();
				    this.in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				    
				    // Send sync command immediately to start the sync feed
				    System.out.println("SyncThread: Sending 'ui sync' command for instance " + MainWin.getInstance());
				    System.err.println("SyncThread: Sending 'ui sync' command for instance " + MainWin.getInstance());
				    this.out.write(( MainWin.getInstance()+"").getBytes());
				    this.out.write((byte) 0);
				    this.out.write("ui sync\n".getBytes());
				    this.out.flush();
				    System.out.println("SyncThread: 'ui sync' command sent successfully");
				    System.err.println("SyncThread: 'ui sync' command sent successfully");
				    
				    // Update connection status
				    MainWin.setConStatusConnect();
				    MainWin.debug("Sync: Connected.");
				    System.out.println("SyncThread: Connected! Sync feed started.");
				    System.err.println("SyncThread: Connected! Sync feed started.");
				    
				    // Now load config and state in background (non-blocking, optional)
				    // Load config - required but can fail gracefully
				    System.out.println("SyncThread: Loading server config in background...");
				    System.out.flush();
				    try {
				        MainWin.setServerConfig(UIUtils.getServerConfig());
				        System.out.println("SyncThread: Server config loaded");
				    } catch (Exception e) {
				        System.err.println("SyncThread: Failed to load server config (non-fatal): " + e.getMessage());
				        // Don't print full stack trace for timeout/connection errors
				        if (!(e instanceof java.net.SocketTimeoutException || e instanceof java.net.ConnectException)) {
				            e.printStackTrace();
				        }
				    }
				  
				    // Optional: load disk info (can timeout, that's OK)
				    System.out.println("SyncThread: Loading disk info in background...");
				    System.out.flush();
				    try
				    {
				    	MainWin.setDisks(UIUtils.getServerDisks());
				    	MainWin.applyDisks();
				    	System.out.println("SyncThread: Disk info loaded");
				    }
				    catch (Exception e)
					{
						// Don't care - disk info is optional and can timeout
						System.out.println("SyncThread: Disk info load failed (non-fatal): " + e.getClass().getSimpleName() + " - " + e.getMessage());
						MainWin.debug("Sync: loading disk info failed");
					}
				    
				    // Optional: load midi status
				    System.out.println("SyncThread: Loading MIDI status in background...");
				    System.out.flush();
				    try
				    {
				    	MainWin.setMidiStatus(UIUtils.getServerMidiStatus());
				    	MainWin.applyMIDIStatus();
				    	System.out.println("SyncThread: MIDI status loaded");
				    }
				    catch (Exception e)
					{
						// Don't care
						System.out.println("SyncThread: MIDI status load failed (non-fatal): " + e.getClass().getSimpleName() + " - " + e.getMessage());
						MainWin.debug("Sync: loading midi status failed");
					}
				} 
				catch (Exception e) 
				{
					System.err.println("=== SyncThread: EXCEPTION during connection attempt ===");
					System.err.println("Exception type: " + e.getClass().getName());
					System.err.println("Exception message: " + e.getMessage());
					System.err.println("Stack trace:");
					e.printStackTrace();
					
					if (MainWin.debugging == true)
						e.printStackTrace();
					
					MainWin.setConStatusError();
					// TODO MainWin.addToDisplay("Sync: " + e.getMessage());
					
					
					sock = null;
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) 
					{
						// the show must go on?
					}
				} 
					
			}
			
			
			if (!wanttodie && (sock != null) && !sock.isInputShutdown())
			{
				MainWin.setConStatusConnect();

				try 
				{
					int thisread = in.read(cbuf,0,READ_BUFFER_SIZE);
					
					if (thisread < 0)
					{
						System.out.println("SyncThread: Read returned -1, connection closed");
						try 
						{
							sock.close();
						} 
						catch (IOException e1) 
						{
							// TODO MainWin.addToDisplay("Sync: " + e1.getMessage());
						}
						
						sock = null;
					}
					else
					{
						System.out.println("SyncThread: Read " + thisread + " bytes from server");
						buffer.append(cbuf, 0, thisread);
						eatData(buffer);
					}
					
					
				
				} 
				catch (IOException e) 
				{
					// TODO MainWin.addToDisplay("Sync: " + e.getMessage());
					
					if (sock != null)
					try 
					{
						sock.close();
					} 
					catch (IOException e1) 
					{
						// TODO MainWin.addToDisplay("Sync: " + e.getMessage());
					}
					
					sock = null;
					
				}
				
			}
		}
	
	}
	
	
	
	private void eatData(StringBuilder buf) 
	{
		int le = buf.indexOf(LINE_END);
		
		while (le > -1)
		{
			if (le > 0)
			{
				String line = buf.substring(0, le);
				// Log all lines being processed for debugging
				if (line.length() > 0 && (line.length() <= 2 || line.charAt(1) == ':')) {
					System.out.println("SyncThread: Processing line: '" + line + "' (length=" + line.length() + ")");
				}
				processLine(line);
			}
			
			buf.delete(0, le+1);
			le = buf.indexOf(LINE_END);
		}
		
		// Log remaining buffer if it's getting large (might indicate missing line endings)
		if (buf.length() > 100) {
			System.out.println("SyncThread: WARNING - Large buffer remaining (" + buf.length() + " chars), first 100 chars: '" + buf.substring(0, Math.min(100, buf.length())) + "'");
		}
	}

	
	private void processLine(String line) 
	{
		// drives
		if (line.equals("D"))
		{
			System.out.println("SyncThread: Received disk event line 'D'");
			System.out.println("SyncThread: params d=" + this.params.get("d") + ", k=" + this.params.get("k") + ", v=" + this.params.get("v"));
			
			if (this.params.containsKey("d") && (this.params.get("d") != null))
			{
				try
				{
					int diskNum = Integer.parseInt(this.params.get("d"));
					String key = this.params.get("k");
					String val = this.params.get("v");
					System.out.println("SyncThread: Calling submitDiskEvent(disk=" + diskNum + ", key=" + key + ", val=" + val + ")");
					MainWin.submitDiskEvent(diskNum, key, val);
					// Clear params after processing disk event
					this.params.clear();
				}
				catch (NumberFormatException e)
				{
					System.err.println("SyncThread: Error parsing disk number: " + e.getMessage());
					this.params.clear();
				}
				
			} else {
				System.err.println("SyncThread: Missing 'd' parameter in disk event");
				this.params.clear();
			}
		}
		// server status
		else if (line.equals("@"))
		{
			try
			{
				if (this.params.containsKey("0"))
				{
					this.ssbuf.setInterval(Integer.parseInt(params.get("0")));
				}
				
				if (this.params.containsKey("1"))
				{
					this.ssbuf.setMemtotal(Long.parseLong(params.get("1")));
				}
				
				if (this.params.containsKey("2"))
				{
					this.ssbuf.setMemfree(Long.parseLong(params.get("2")));
				}
				
				if (this.params.containsKey("3"))
				{
					this.ssbuf.setOps(Long.parseLong(params.get("3")));
				}
				
				if (this.params.containsKey("4"))
				{
					this.ssbuf.setDiskops(Long.parseLong(params.get("4")));
				}
				
				if (this.params.containsKey("5"))
				{
					this.ssbuf.setVserialops(Long.parseLong(params.get("5")));
				}
				
				if (this.params.containsKey("6"))
				{
					this.ssbuf.setInstances(Integer.parseInt(params.get("6")));
				}
				
				if (this.params.containsKey("7"))
				{
					this.ssbuf.setInstancesalive(Integer.parseInt(params.get("7")));
				}
				
				if (this.params.containsKey("8"))
				{
					this.ssbuf.setThreads(Integer.parseInt(params.get("8")));
				}
				
				if (this.params.containsKey("9"))
				{
					this.ssbuf.setUIClients(Integer.parseInt(params.get("9")));
				}
				
				if (this.params.containsKey("!"))
				{
					this.ssbuf.setMagic(Long.parseLong(params.get("!")));
				}
				
				MainWin.submitServerStatusEvent(ssbuf);
				// Clear params after processing server status event
				this.params.clear();
			}
			catch (NumberFormatException e)
			{
				this.params.clear();
			}
			
		}
		// logging
		else if (line.equals("L"))
		{
			if (this.params.containsKey("l"))
				logbuf.setLevel(this.params.get("l"));
			
			if (this.params.containsKey("t"))
				logbuf.setTimestamp(Long.valueOf(this.params.get("t")));
			
			if (this.params.containsKey("m"))
				logbuf.setMessage(this.params.get("m"));
			
			if (this.params.containsKey("r"))
				logbuf.setThread(this.params.get("r"));
			
			if (this.params.containsKey("s"))
				logbuf.setSource(this.params.get("s"));
			
			MainWin.addToServerLog(logbuf.clone());
			// Clear params after processing log event
			this.params.clear();
				
		}
		// instance config
		else if (line.equals("I"))
		{
			//System.out.println("I" + ": " + this.params.get("k") + " = " + this.params.get("v"));
			
			if (this.params.containsKey("k") && (this.params.get("k") != null))
			{
				if (this.params.containsKey("v"))
				{
					if (this.params.get("v") == null)
					{
						MainWin.getInstanceConfig().clearProperty(this.params.get("k"));
					}
					else
					{
						MainWin.getInstanceConfig().setProperty(this.params.get("k"), this.params.get("v"));
					}
					
					
				}
					
			}
			// Clear params after processing instance config event
			this.params.clear();
		}
		// server config
		else if (line.equals("C"))
		{
			//System.out.println("C" + ": " + this.params.get("k") + " = " + this.params.get("v"));
			
			
			if (this.params.containsKey("k") && (this.params.get("k") != null))
			{
				if (this.params.containsKey("v"))
				{
					MainWin.submitServerConfigEvent(this.params.get("k"), this.params.get("v"));
				}
					
			}
			// Clear params after processing server config event
			this.params.clear();
		}
		// MIDI
		else if (line.equals("M"))
		{

			if (this.params.containsKey("k") && (this.params.get("k") != null))
			{
				if (this.params.get("k").equals("device") )
				{
					MainWin.getMidiStatus().setCurrentDevice(this.params.get("v"));
				}
				else if (this.params.get("k").equals("profile") )
				{
					MainWin.getMidiStatus().setCurrentProfile(this.params.get("v"));
				}
				else if (this.params.get("k").equals("soundbank") )
				{
					
				}
				else if (this.params.get("k").equals("voicelock") )
				{
					MainWin.getMidiStatus().setVoiceLock(Boolean.valueOf(this.params.get("v")));
				}
				
				
				MainWin.applyMIDIStatus();
			}
			// Clear params after processing MIDI event
			this.params.clear();
		}
		
		
		
		
		// params
		if (line.length()>1)
		{
			if (line.charAt(1) == ':') 
			{
				String val = null;
				if (line.length() > 2)
				{
					val = line.substring(2);
				}

				String paramKey = line.substring(0,1);
				this.params.put(paramKey, val);
				
				// Debug logging for disk-related params
				if (paramKey.equals("d") || paramKey.equals("k") || paramKey.equals("v")) {
					System.out.println("SyncThread: Parsed param " + paramKey + "=" + val + " (current params: d=" + this.params.get("d") + ", k=" + this.params.get("k") + ", v=" + this.params.get("v") + ")");
				}
			}
		} else {
			// Log non-param lines for debugging (including event type lines)
			if (line.length() > 0) {
				System.out.println("SyncThread: Received event type line: '" + line + "' (current params: d=" + this.params.get("d") + ", k=" + this.params.get("k") + ", v=" + this.params.get("v") + ")");
			}
		}
		
	}

	public void die()
	{
		this.wanttodie = true;
		
		if (this.sock != null)
		{
			try 
			{
				this.sock.close();
			} 
			catch (IOException e) 
			{
			}
			
			sock = null;
		}
	}

}

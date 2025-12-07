package com.groupunix.drivewireserver.dwcommands;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.dwprotocolhandler.DWVSerialProtocol;
import com.groupunix.drivewireserver.virtualserial.api.DWAPITCP;

public class DWCmdPortTCP extends DWCommand {

	private DWVSerialProtocol dwProto;
	
	public DWCmdPortTCP(DWVSerialProtocol dwProtocol, DWCommand parent)
	{
		setParentCmd(parent);
		this.dwProto = dwProtocol;
	}
	
	public String getCommand() 
	{
		return "tcp";
	}

	public DWCommandList getCommandList()
	{
		return(null);
	}

	public DWCommandResponse parse(String cmdline)
	{
		if (cmdline.length() == 0)
		{
			return(new DWCommandResponse(false, DWDefs.RC_SYNTAX_ERROR, "dw port tcp requires arguments. Usage: dw port tcp <port#> listen <tcpport>"));
		}
		
		String[] args = cmdline.split(" ");
		
		if (args.length < 3)
		{
			return(new DWCommandResponse(false, DWDefs.RC_SYNTAX_ERROR, "dw port tcp requires port#, command, and arguments. Usage: dw port tcp <port#> listen <tcpport>"));
		}
		
		try
		{
			int vport = Integer.parseInt(args[0]);
			
			// Validate port
			if (vport < 0 || vport >= dwProto.getVPorts().getMaxPorts())
			{
				return(new DWCommandResponse(false, DWDefs.RC_INVALID_PORT, "Invalid virtual port number: " + vport));
			}
			
			// Open the port if not already open
			if (!dwProto.getVPorts().isOpen(vport))
			{
				dwProto.getVPorts().openPort(vport);
			}
			
			// Build command array for DWAPITCP
			// Format: ["tcp", "listen", "<tcpport>"]
			String[] tcpCmd = new String[args.length];
			tcpCmd[0] = "tcp";
			for (int i = 1; i < args.length; i++)
			{
				tcpCmd[i] = args[i];
			}
			
			// Process via DWAPITCP
			DWAPITCP api = new DWAPITCP(tcpCmd, dwProto, vport);
			return api.process();
		}
		catch (NumberFormatException e)
		{
			return(new DWCommandResponse(false, DWDefs.RC_SYNTAX_ERROR, "Non-numeric port number: " + args[0]));
		}
		catch (Exception e)
		{
			return(new DWCommandResponse(false, DWDefs.RC_SERVER_ERROR, "Error setting up TCP: " + e.getMessage()));
		}
	}


	public String getShortHelp() 
	{
		return "Manage TCP connections for virtual serial ports";
	}


	public String getUsage() 
	{
		return "dw port tcp <port#> listen <tcpport>";
	}
	
	public boolean validate(String cmdline) 
	{
		return(true);
	}
}


package com.groupunix.drivewireserver.dwcommands;




import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.DriveWireServer;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocol;

public class DWCmdInstanceStart extends DWCommand {

	private DWProtocol dwProto;

	public DWCmdInstanceStart(DWProtocol dwProto2,DWCommand parent)
	{
		setParentCmd(parent);
		this.dwProto = dwProto2;
	}
	
	public String getCommand() 
	{
		return "start";
	}

	
	public String getShortHelp() 
	{
		return "Start instance #";
	}


	public String getUsage() 
	{
		return "dw instance start [#]";
	}

	public DWCommandResponse parse(String cmdline) 
	{
		// If no instance number provided, use the current instance from the protocol handler
		if (cmdline.length() == 0)
		{
			if (this.dwProto != null)
			{
				return(doStart(String.valueOf(this.dwProto.getHandlerNo())));
			}
			else
			{
				// Default to instance 0 if no protocol handler available
				return(doStart("0"));
			}
		}
		
		return(doStart(cmdline));
	}

	
	private DWCommandResponse doStart(String instr) 
	{
		
		
		try
		{
			int intno = Integer.parseInt(instr);
		
			if (!DriveWireServer.isValidHandlerNo(intno))
				return(new DWCommandResponse(false,DWDefs.RC_INVALID_HANDLER, "Invalid instance number."));
			
			if (DriveWireServer.getHandler(intno) == null)
				return(new DWCommandResponse(false,DWDefs.RC_INVALID_HANDLER, "Instance " + intno + " is not defined."));
			
			if (DriveWireServer.getHandler(intno).isReady())
				return(new DWCommandResponse(false,DWDefs.RC_INSTANCE_ALREADY_STARTED, "Instance " + intno + " is already started."));
			
			if (DriveWireServer.getHandler(intno).isDying())
				return(new DWCommandResponse(false,DWDefs.RC_INSTANCE_NOT_READY, "Instance " + intno + " is in the process of shutting down."));
			
			DriveWireServer.startHandler(intno);
			
			return(new DWCommandResponse("Starting instance # " + intno));
		
		}
		catch (NumberFormatException e)
		{
			return(new DWCommandResponse(false,DWDefs.RC_SYNTAX_ERROR, "dw instance start requires a numeric instance # as an argument"));
				
		} 
		
	}
	
	
	public boolean validate(String cmdline) 
	{
		return(true);
	}
}

package com.groupunix.drivewireserver.dwcommands;

import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocol;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocolHandler;

public class DWCmdInstanceStatus extends DWCommand {

	private DWProtocol dwProto;
	
	public DWCmdInstanceStatus(DWProtocol dwProto, DWCommand parent)
	{
		setParentCmd(parent);
		this.dwProto = dwProto;
	}

	
	public String getCommand() 
	{
		return "status";
	}

	
	public String getShortHelp() 
	{
		return "Show instance status";
	}


	public String getUsage() 
	{
		return "dw instance status";
	}

	public DWCommandResponse parse(String cmdline) 
	{
		String txt = "";
		
		if (!(dwProto instanceof DWProtocolHandler))
		{
			return(new DWCommandResponse(false, com.groupunix.drivewireserver.DWDefs.RC_INSTANCE_WONT, "This operation is not supported on this type of instance"));
		}
		
		DWProtocolHandler handler = (DWProtocolHandler) dwProto;
		
		txt = "num|" + handler.getHandlerNo() + "\n";
		txt += "name|" + handler.getConfig().getString("[@name]","not set") + "\n";
		txt += "desc|" + handler.getConfig().getString("[@desc]","not set") + "\n";
		
		txt += "proto|" + handler.getConfig().getString("Protocol","DriveWire") + "\n";
		
		txt += "autostart|" + handler.getConfig().getBoolean("AutoStart", true) + "\n";
		txt += "dying|" + handler.isDying() + "\n";
		txt += "started|" + handler.isStarted() + "\n";
		txt += "ready|" + handler.isReady() + "\n";
		txt += "connected|" + handler.isConnected() + "\n";
		
		if (handler.getProtoDev() != null)
		{
			txt += "devicetype|" + handler.getProtoDev().getDeviceType() + "\n";
			
			txt += "devicename|" + handler.getProtoDev().getDeviceName() + "\n";
			txt += "deviceconnected|" + handler.getProtoDev().connected() + "\n";
			
			if (handler.getProtoDev().getRate() > -1)
				txt += "devicerate|" + handler.getProtoDev().getRate() + "\n";
			
			if (handler.getProtoDev().getClient() != null)
				txt += "deviceclient|" + handler.getProtoDev().getClient() + "\n";
			
		}
		
		if (handler.getConfig().getString("Protocol", "DriveWire").equals("DriveWire"))
		{
			txt += "lastopcode|" + handler.getLastOpcode() + "\n";
			txt += "lastgetstat|" + (handler.getLastGetStat() == 255 ? "None" : handler.getLastGetStat()) + "\n";
			txt += "lastsetstat|" + (handler.getLastSetStat() == 255 ? "None" : handler.getLastSetStat()) + "\n";
			txt += "lastlsn|" + handler.getLastLSN() + "\n";
			txt += "lastdrive|" + handler.getLastDrive() + "\n";
			txt += "lasterror|" + handler.getLastError() + "\n";
			txt += "lastchecksum|" + handler.getLastChecksum() + "\n";
		}
		
		return(new DWCommandResponse(txt));
	}

	public boolean validate(String cmdline) 
	{
		return(true);
	}
}


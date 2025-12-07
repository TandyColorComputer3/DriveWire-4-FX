package com.groupunix.drivewireserver.uicommands;


import com.groupunix.drivewireserver.DWUIClientThread;
import com.groupunix.drivewireserver.dwcommands.DWCommand;
import com.groupunix.drivewireserver.dwcommands.DWCommandList;
import com.groupunix.drivewireserver.dwcommands.DWCommandResponse;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocol;

public class UICmd extends DWCommand
{

	static final String command = "ui";
	private DWCommandList commands;
		
	
	public UICmd(DWUIClientThread ct)
	{
		commands = new DWCommandList(null);
		commands.addcommand(new UICmdInstance(ct));
		commands.addcommand(new UICmdServer(ct));
		commands.addcommand(new UICmdSync(ct));
		commands.addcommand(new UICmdTest(ct));
		// Add "dw" as an alias for "instance" to support legacy command format
		commands.addcommand(new UICmdDW(ct));
	} 

	
	public UICmd(DWProtocol dwProto)
	{
		commands = new DWCommandList(null);
		commands.addcommand(new UICmdInstance(dwProto));
		commands.addcommand(new UICmdServer(dwProto));
	//	commands.addcommand(new UICmdSync(dwProto));
	//	commands.addcommand(new UICmdTest(dwProto));
	}


	public String getCommand() 
	{
		return command;
	}
	
	public DWCommandList getCommandList()
	{
		return(this.commands);
	}
	

	public DWCommandResponse parse(String cmdline)
	{
		if (cmdline.length() == 0)
		{
			return(new DWCommandResponse(this.commands.getShortHelp()));
		}
		
		// If command starts with "dw ", "instance ", "server ", "sync ", or "test ",
		// it's already a subcommand, so parse it directly
		// Otherwise, parse normally (which will look for "dw", "instance", etc. as commands)
		return(commands.parse(cmdline));
	}


	public String getShortHelp() 
	{
		return "Managment commands with machine parsable output";
	}


	public String getUsage() 
	{
		return "ui [command]";
	}


	public boolean validate(String cmdline) 
	{
		return(commands.validate(cmdline));
	}
	
	
}

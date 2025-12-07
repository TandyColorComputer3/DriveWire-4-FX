package com.groupunix.drivewireserver.uicommands;

import com.groupunix.drivewireserver.DWUIClientThread;
import com.groupunix.drivewireserver.dwcommands.DWCommand;
import com.groupunix.drivewireserver.dwcommands.DWCommandResponse;

/**
 * "dw" command alias - maps to "ui instance" for backward compatibility
 * Allows commands like "dw disk insert" instead of "ui instance disk insert"
 */
public class UICmdDW extends DWCommand {
	
	static final String command = "dw";
	
	private UICmdInstance instanceCmd;
	
	public UICmdDW(DWUIClientThread dwuiClientThread) {
		// Delegate to UICmdInstance - it will initialize its own commands list
		this.instanceCmd = new UICmdInstance(dwuiClientThread);
	}
	
	public String getCommand() {
		return command;
	}
	
	public DWCommandResponse parse(String cmdline) {
		// Forward to instance command handler
		return instanceCmd.parse(cmdline);
	}
	
	public String getShortHelp() {
		return "DriveWire instance commands (alias for 'ui instance')";
	}
	
	public String getUsage() {
		return "dw [command]";
	}
	
	public boolean validate(String cmdline) {
		return instanceCmd.validate(cmdline);
	}
}


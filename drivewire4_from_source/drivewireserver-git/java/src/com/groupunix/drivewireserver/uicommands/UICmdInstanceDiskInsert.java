package com.groupunix.drivewireserver.uicommands;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.DWUIClientThread;
import com.groupunix.drivewireserver.DriveWireServer;
import com.groupunix.drivewireserver.dwcommands.DWCommand;
import com.groupunix.drivewireserver.dwcommands.DWCommandResponse;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocolHandler;

public class UICmdInstanceDiskInsert extends DWCommand {
	
	static final String command = "insert";
	
	private DWUIClientThread uiref = null;
	private DWProtocolHandler dwProto = null;
	
	public UICmdInstanceDiskInsert(DWUIClientThread dwuiClientThread) {
		this.uiref = dwuiClientThread;
	}
	
	public String getCommand() {
		return command;
	}
	
	public DWCommandResponse parse(String cmdline) {
		// Get protocol handler
		if (this.dwProto == null) {
			int instanceNo = this.uiref.getInstance();
			if (instanceNo < 0) {
				return(new DWCommandResponse(false, DWDefs.RC_INSTANCE_WONT, 
					"No instance attached. Use 'ui instance attach <instance#' to attach to an instance first."));
			}
			if (!DriveWireServer.isValidHandlerNo(instanceNo)) {
				return(new DWCommandResponse(false, DWDefs.RC_INVALID_HANDLER, 
					"Invalid instance number: " + instanceNo));
			}
			if (!DriveWireServer.getHandler(instanceNo).hasDisks()) {
				return(new DWCommandResponse(false, DWDefs.RC_INSTANCE_WONT, 
					"Instance " + instanceNo + " does not support disk operations. " +
					"Instance type: " + DriveWireServer.getHandler(instanceNo).getConfig().getString("Protocol", "unknown")));
			}
			dwProto = (DWProtocolHandler) DriveWireServer.getHandler(instanceNo);
		}
		
		// Forward to protocol handler's disk insert command
		// Format: "insert <drive> <path>"
		com.groupunix.drivewireserver.dwcommands.DWCmdDiskInsert insertCmd = 
			new com.groupunix.drivewireserver.dwcommands.DWCmdDiskInsert(dwProto, null);
		return insertCmd.parse(cmdline);
	}
	
	public String getShortHelp() {
		return "Insert disk into drive";
	}
	
	public String getUsage() {
		return "dw disk insert <drive> <path>";
	}
	
	public boolean validate(String cmdline) {
		return true;
	}
}


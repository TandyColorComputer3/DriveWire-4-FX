package com.groupunix.drivewireserver.uicommands;

import com.groupunix.drivewireserver.DWDefs;
import com.groupunix.drivewireserver.DWUIClientThread;
import com.groupunix.drivewireserver.DriveWireServer;
import com.groupunix.drivewireserver.dwcommands.DWCommand;
import com.groupunix.drivewireserver.dwcommands.DWCommandResponse;
import com.groupunix.drivewireserver.dwprotocolhandler.DWProtocolHandler;

public class UICmdInstanceDiskEject extends DWCommand {
	
	static final String command = "eject";
	
	private DWUIClientThread uiref = null;
	private DWProtocolHandler dwProto = null;
	
	public UICmdInstanceDiskEject(DWUIClientThread dwuiClientThread) {
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
		
		// Forward to protocol handler's disk eject command
		// Format: "eject <drive> | all"
		com.groupunix.drivewireserver.dwcommands.DWCmdDiskEject ejectCmd = 
			new com.groupunix.drivewireserver.dwcommands.DWCmdDiskEject(dwProto, null);
		return ejectCmd.parse(cmdline);
	}
	
	public String getShortHelp() {
		return "Eject disk from drive";
	}
	
	public String getUsage() {
		return "dw disk eject <drive> | all";
	}
	
	public boolean validate(String cmdline) {
		return true;
	}
}


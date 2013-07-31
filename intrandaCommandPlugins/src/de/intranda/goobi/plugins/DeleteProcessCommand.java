package de.intranda.goobi.plugins;

import java.io.File;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.beans.Process;
import org.goobi.beans.Step;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.WebDav;
import de.sub.goobi.persistence.managers.ProcessManager;

@PluginImplementation
public class DeleteProcessCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(DeleteProcessCommand.class);

	private static final String ID = "deleteProcess";
	// private static final String NAME = "DeleteProcess Command Plugin";
	// private static final String VERSION = "1.0.20111109";

	private HashMap<String, String> parameterMap;

	@Override
	public PluginType getType() {
		return PluginType.Command;
	}

	@Override
	public String getTitle() {
		return ID;
	}

	
	public String getId() {
		return ID;
	}

	@Override
	public String getDescription() {
		return ID;
	}

	@Override
	public void setParameterMap(HashMap<String, String> parameterMap) {
		this.parameterMap = parameterMap;
	}

	@Override
	public boolean usesHttpSession() {
		return false;
	}

	@Override
	public void setHttpResponse(HttpServletResponse resp) {
	}

	@Override
	public void setHttpRequest(HttpServletRequest req) {
	}

	@Override
	public CommandResponse validate() {
		if (!parameterMap.containsKey("processId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' defined.";
			return new CommandResponse(400, title, message);
			// return new CommandResponse(title, message);
		}
		return null;
	}

	@Override
	public CommandResponse execute() {
		Integer id = Integer.parseInt(parameterMap.get("processId"));
		// if (!session.isOpen() || !session.isConnected()) {
		// Connection con = ConnectionHelper.getConnection();
		// session.reconnect(con);
		// }
		try {
			Process p = ProcessManager.getProcessById(id);
			for (Step step : p.getSchritteList()) {
				WebDav myDav = new WebDav();
				for (User b : step.getBenutzerList()) {
					try {
						myDav.UploadFromHome(b, p);
					} catch (RuntimeException e) {
					}
				}
				for (Usergroup bg : step.getBenutzergruppenList()) {
					for (User b : bg.getBenutzer()) {
						try {
							myDav.UploadFromHome(b, p);
						} catch (RuntimeException e) {
						}
					}
				}
			}

			Helper.deleteDir(new File(p.getProcessDataDirectory()));
			ProcessManager.deleteProcess(p);
			

		} catch (Exception e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(500, title, message);
			// return new CommandResponse(title, message);
		}

		String title = "Command executed";
		String message = "Process deleted";
		return new CommandResponse(200, title, message);
		// return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command deleteProcess";
		String message = "This command deletes a process in Goobi." + "\nThe parameter 'processId' defines the process.";
		return new CommandResponse(200, title, message);
		// return new CommandResponse(title, message);
	}

}

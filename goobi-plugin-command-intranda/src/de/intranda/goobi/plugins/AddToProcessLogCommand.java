package de.intranda.goobi.plugins;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.beans.LogEntry;
import org.goobi.beans.Process;
import org.goobi.beans.Step;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;



@PluginImplementation
public class AddToProcessLogCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(AddToProcessLogCommand.class);

	private static final String ID = "addToProcessLog";
//	private static final String NAME = "AddToProcessLog Command Plugin";
//	private static final String VERSION = "1.0.20111108";

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

		if (!this.parameterMap.containsKey("processId") && !this.parameterMap.containsKey("stepId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' and 'stepId' defined. One of these is required.";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}

		if (!this.parameterMap.containsKey("value")) {
			String title = "Missing parameter";
			String message = "No parameter 'value' defined";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}

		if (!this.parameterMap.containsKey("type")) {
			String title = "Missing parameter";
			String message = "No parameter 'type' defined. Possible values are: user, error, warn, info, debug";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}

		return null;
	}

	@Override
	public CommandResponse execute() {

		String value = this.parameterMap.get("value");
		try {
			value = URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			logger.warn("cannot encode " + value);
		}
		String type = this.parameterMap.get("type");
		Process process = null;
		Integer id = null;
		int processId = 0;

		if (this.parameterMap.containsKey("stepId")) {
			id = Integer.parseInt(this.parameterMap.get("stepId"));
			Step so = StepManager.getStepById(id);
			if (so == null) {
				String title = "Error during execution";
				String message = "Could not load step with id: " + id;
				return new CommandResponse(500,title, message);
//				return new CommandResponse(title, message);
			}
			processId = so.getProcessId();
		} else {
			processId = Integer.parseInt(this.parameterMap.get("processId"));
		}
		process = ProcessManager.getProcessById(processId);

        LogEntry logEntry = new LogEntry();
        logEntry.setContent(value);
        logEntry.setCreationDate(new Date());
        logEntry.setProcessId(process.getId());
        logEntry.setType(LogType.getByTitle(type));

        logEntry.setUserName("webapi");

        ProcessManager.saveLogEntry(logEntry);
		

		String title = "Command executed";
		String message = "Message to process log added";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command addToProcessLog";
		
		String message = "This command adds a message to the log for a process." +
				"\n - 'processId' defines the process." +
				"\n - 'value' defines the message that gets added." +
				"\n - 'type' defines the type of the message. Allowed values are 'error', 'info', 'user', 'debug'.";

		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

}
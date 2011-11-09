package de.intranda.goobi.plugins;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.cli.helper.WikiFieldHelper;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.Persistence.SchrittDAO;
import de.sub.goobi.helper.exceptions.DAOException;


@PluginImplementation
public class AddToProcessLogCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(AddToProcessLogCommand.class);

	private static final String ID = "addToProcessLog";
	private static final String NAME = "AddToProcessLog Command Plugin";
	private static final String VERSION = "1.0.20111108";

	private HashMap<String, String> parameterMap;

	@Override
	public PluginType getType() {
		return PluginType.Command;
	}

	@Override
	public String getTitle() {
		return NAME + " v" + VERSION;
	}

	@Override
	public String getId() {
		return ID;
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
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
		
		if (!parameterMap.containsKey("processId") && !parameterMap.containsKey("stepId")){
			String title = "Missing parameter";
			String message = "No parameter 'processId' and 'stepId' defined. One of these is required.";
			return new CommandResponse(title, message);
		}
		
		if (!parameterMap.containsKey("value")){
			String title = "Missing parameter";
			String message = "No parameter 'value' defined";
			return new CommandResponse(title, message);
		}
		
		if (!parameterMap.containsKey("type")){
			String title = "Missing parameter";
			String message = "No parameter 'type' defined. Possible values are: user, error, warn, info, debug";
			return new CommandResponse(title, message);
		}
		
		return null;
	}
	
	@Override
	public CommandResponse execute() {
		
		String value = parameterMap.get("value");
		String type = parameterMap.get("type");
		Prozess process = null;
		Integer id = null;

		try {
			if (parameterMap.containsKey("stepId")) {
				id = Integer.parseInt(parameterMap.get("stepId"));
				process = new SchrittDAO().get(id).getProzess();
			}  {
				id = Integer.parseInt(parameterMap.get("processId"));
				process = new ProzessDAO().get(id);
			}

			if (process != null) {
				process.setWikifield(process.getWikifield() + WikiFieldHelper.getWikiMessage(type, value));
				new ProzessDAO().save(process);
			}
		} catch (DAOException e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(title, message);
		}
		
		String title = "Command executed";
		String message = "Message to process log added";
		return new CommandResponse(title, message);
	}
	
	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(title, message);
	}
	
}

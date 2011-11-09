package de.intranda.goobi.plugins;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.Beans.Schritt;
import de.sub.goobi.Persistence.SchrittDAO;
import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;


@PluginImplementation
public class CloseStepCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(CloseStepCommand.class);

	private static final String ID = "closeStep";
	private static final String NAME = "CloseStep Command Plugin";
	private static final String VERSION = "1.0.20111109";

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
		if (!parameterMap.containsKey("stepId")){
			String title = "Missing parameter";
			String message = "No parameter 'stepId' defined.";
			return new CommandResponse(title, message);
		}
		return null;
	}
	
	@Override
	public CommandResponse execute() {
		Integer id = Integer.parseInt(parameterMap.get("stepId"));
		
		try {
		SchrittDAO dao = new SchrittDAO();
		Schritt step = dao.get(id);
		HelperSchritte hs = new HelperSchritte();
		step.setEditTypeEnum(StepEditType.AUTOMATIC);
		step.setBearbeitungsstatusEnum(StepStatus.DONE);
		dao.save(step);
		hs.SchrittAbschliessen(step, true);

		} catch (DAOException e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(title, message);
		}
		
		String title = "Command executed";
		String message = "Step closed";
		return new CommandResponse(title, message);
	}
	
	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(title, message);
	}
	
}

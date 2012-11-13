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

import de.sub.goobi.Persistence.apache.StepManager;
import de.sub.goobi.Persistence.apache.StepObject;
import de.sub.goobi.helper.HelperSchritteWithoutHibernate;


@PluginImplementation
public class CloseStepCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(CloseStepCommand.class);

	private static final String ID = "closeStep";
//	private static final String NAME = "CloseStep Command Plugin";
//	private static final String VERSION = "1.0.20111109";

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
		return getTitle();
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
		if (!this.parameterMap.containsKey("stepId")){
			String title = "Missing parameter";
			String message = "No parameter 'stepId' defined.";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}
		return null;
	}
	
	@Override
	public CommandResponse execute() {
		Integer id = Integer.parseInt(this.parameterMap.get("stepId"));
		logger.debug("closing step with id " + id);
		StepObject so = StepManager.getStepById(id);
		logger.debug("loaded StepObject with id " + so.getId());
		HelperSchritteWithoutHibernate hs = new HelperSchritteWithoutHibernate();
		hs.CloseStepObjectAutomatic(so, false);	
		String title = "Command executed";
		String message = "Step closed";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}
	
	@Override
	public CommandResponse help() {
		String title = "Command closeStep";
		String message = "This command closes a step and opens the next task. If next task is an automatic task, all scripts gets started." +
				"\n 'stepId' defines the step that gets closed.";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}
	
}

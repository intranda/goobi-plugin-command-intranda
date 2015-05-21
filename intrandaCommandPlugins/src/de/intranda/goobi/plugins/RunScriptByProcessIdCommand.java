package de.intranda.goobi.plugins;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;

import org.goobi.beans.Step;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.persistence.managers.StepManager;



@PluginImplementation
public class RunScriptByProcessIdCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(RunScriptByProcessIdCommand.class);
	private static final String ID = "runScriptByProcessId";
	private static final String NAME = "RunScript by Process Command Plugin";
	private HttpServletResponse response;
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
		return NAME;
	}

	@Override
	public void setParameterMap(HashMap<String, String> parameterMap) {
		this.parameterMap = parameterMap;
	}

	@Override
	public CommandResponse validate() {
		if (!this.parameterMap.containsKey("processId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' defined.";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}
		if (!this.parameterMap.containsKey("stepName")) {
			String title = "Missing parameter";
			String message = "No parameter 'stepName' defined.";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}
		return null;
	}

	@Override
	public CommandResponse execute() {
		String processId = this.parameterMap.get("processId");
		String stepName = this.parameterMap.get("stepName");
		String scriptname = this.parameterMap.get("scriptName");
		try {
			int id = Integer.parseInt(processId);
			List<Step> stepList = StepManager.getStepsForProcess(id);
			Step step = null;
			for (Step so : stepList) {
				if (so.getTitel().equals(stepName)) {
					step = so;
				}
			}
			
			if (step == null) {
				String title = "Error during execution";
				String message = "step " + stepName + " cannot be found";
				return new CommandResponse(500,title, message);
			}
			
			HelperSchritte hs = new HelperSchritte();

			if (scriptname != null && scriptname.length() > 0) {
				Map<String,String> scripts = step.getAllScripts();
				if (scripts.containsKey(scriptname)) {
					String script = scripts.get(scriptname);
					hs.executeScriptForStepObject(step, script, false);					
				} else {
					String title = "Error during execution";
					String message = "script " + scriptname + " does not exist";
					return new CommandResponse(500,title, message);
				}
			} else {
				hs.executeAllScriptsForStep(step, false);
			}
			String answer = "finished script";
			OutputStream out = this.response.getOutputStream();
			out.write(answer.getBytes());
			out.flush();
		} catch (Exception e) {
			logger.info(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(500,title, message);
		}
		String title = "Command executed";
		String message = "";
		return new CommandResponse(200,title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command runScript";
		String message = "This command calls scripts for a given task.";
		message += "\n - 'processId' defines the id of the process.";
		message += "\n - 'stepName' defines the name of task.";
		message += "\n - 'scriptname' is optional and defines a script to call. If no script is defined, all scripts of the task gets started.";
		return new CommandResponse(200,title, message);
	}

	@Override
	public boolean usesHttpSession() {
		return true;
	}

	@Override
	public void setHttpResponse(HttpServletResponse resp) {
		this.response = resp;
	}

	@Override
	public void setHttpRequest(HttpServletRequest resp) {
	}

}

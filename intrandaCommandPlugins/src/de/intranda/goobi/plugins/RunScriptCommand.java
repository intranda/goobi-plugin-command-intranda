package de.intranda.goobi.plugins;

import java.io.OutputStream;
import java.util.HashMap;
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
public class RunScriptCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(RunScriptCommand.class);
	private static final String ID = "runScript";
	private static final String NAME = "RunScript Command Plugin";
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
		if (!this.parameterMap.containsKey("stepId")) {
			String title = "Missing parameter";
			String message = "No parameter 'stepId' defined.";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}
		return null;
	}

	@Override
	public CommandResponse execute() {
		String stepId = this.parameterMap.get("stepId");
		String scriptname = this.parameterMap.get("scriptname");
		try {
			int id = Integer.parseInt(stepId);
			Step step = StepManager.getStepById(id);
//			Schritt step = new SchrittDAO().get(id);
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
//					return new CommandResponse(title, message);
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
//			return new CommandResponse(title, message);
		}
		String title = "Command executed";
		String message = "";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command runScript";
		String message = "This command calls scripts for a given task.";
		message += "\n - 'stepId' defines the id of the task.";
		message += "\n - 'scriptname' is optional and defines a script to call. If no script is defined, all scripts of the task gets started.";
//		stepId
//		scriptname
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
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


//	private void runScript(List<Prozess> inProzesse, String stepname, String scriptname) {
//		HelperSchritteWithoutHibernate hs = new HelperSchritteWithoutHibernate();
//		for (Prozess p : inProzesse) {
//			for (Schritt step : p.getSchritteList()) {
//				if (step.getTitel().equalsIgnoreCase(stepname)) {
//					if (scriptname != null) {
//						if (step.getAllScripts().containsKey(scriptname)) {
//							String path = step.getAllScripts().get(scriptname);
//							try {
//								hs.executeScript(step, path, false);
//							} catch (SwapException e) {
//								Helper.setFehlerMeldung("Error while running script " + path, e);
//							}
//						}
//					} else {
//						try {
//							hs.executeAllScriptsForStep(step, false);
//						} catch (SwapException e) {
//							Helper.setFehlerMeldung("Error while running scripts", e);
//						} catch (DAOException e) {
//							Helper.setFehlerMeldung("Error while running scripts", e);
//						}
//					}
//				}
//			}
//		}
//
//	}
}

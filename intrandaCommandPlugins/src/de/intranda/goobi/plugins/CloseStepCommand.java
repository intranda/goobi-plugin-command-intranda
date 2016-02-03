package de.intranda.goobi.plugins;

import java.io.File;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.beans.Step;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.plugin.interfaces.IValidatorPlugin;

import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.HelperSchritte;
import de.sub.goobi.helper.ShellScript;
import de.sub.goobi.persistence.managers.StepManager;


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
		Step so = StepManager.getStepById(id);
		if (so.getValidationPlugin() != null && so.getValidationPlugin().length() >0) {
			IValidatorPlugin ivp = (IValidatorPlugin) PluginLoader.getPluginByTitle(PluginType.Validation, so.getValidationPlugin());
			ivp.setStep(so);
			if (!ivp.validate()) {
				String title = "Error during execution";
				String message = "Step not closed, validation failed";
				return new CommandResponse(500, title, message);
			}
		}
		logger.debug("loaded StepObject with id " + so.getId());
		HelperSchritte hs = new HelperSchritte();
		hs.CloseStepObjectAutomatic(so);	
		
		// try to remove symlink from user home 
		if (parameterMap.get("username") != null && parameterMap.get("username").length() > 0) {
			String homeDir = ConfigurationHelper.getInstance().getUserFolder();
			String username = parameterMap.get("username");
			Process po = so.getProzess();
			String nach = homeDir + username + "/";
			
			nach += po.getTitel() + " [" + po.getId() + "]";

			/* Leerzeichen maskieren */
			nach = nach.replaceAll(" ", "__");
			File benutzerHome = new File(nach);

			String command = ConfigurationHelper.getInstance().getScriptDeleteSymLink() + " ";
			command += benutzerHome;
			// myLogger.debug(command);

			try {
			    ShellScript.legacyCallShell2(command);
			} catch (java.io.IOException ioe) {
				logger.error("IOException UploadFromHome", ioe);
				String title = "Error during execution";
				String message = "Step was closed, but unmount from user home failed";
				return new CommandResponse(500, title, message);
			} catch (InterruptedException e) {
				logger.error("IOException UploadFromHome", e);
				String title = "Error during execution";
				String message = "Step was closed, but unmount from user home failed";
				return new CommandResponse(500, title, message);
			}
			
		}
		
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

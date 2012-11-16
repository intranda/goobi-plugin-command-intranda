package de.intranda.goobi.plugins;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import ugh.dl.Fileformat;
import ugh.dl.Prefs;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;

import de.sub.goobi.Metadaten.MetadatenImagesHelper;
import de.sub.goobi.Metadaten.MetadatenVerifizierungWithoutHibernate;
import de.sub.goobi.Persistence.apache.FolderInformation;
import de.sub.goobi.Persistence.apache.ProcessManager;
import de.sub.goobi.Persistence.apache.ProcessObject;
import de.sub.goobi.Persistence.apache.StepManager;
import de.sub.goobi.Persistence.apache.StepObject;
import de.sub.goobi.config.ConfigMain;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.HelperSchritteWithoutHibernate;

@PluginImplementation
public class CloseStepByProcessIdCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(CloseStepByProcessIdCommand.class);

	private static final String ID = "closeStepByProcessId";
//	private static final String NAME = "CloseStepByProcessId Command Plugin";
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

	@Override
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
		if (!this.parameterMap.containsKey("processId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' defined.";
			return new CommandResponse(400, title, message);
			// return new CommandResponse(title, message);
		}
		return null;
	}

	@Override
	public CommandResponse execute() {
		Integer id = Integer.parseInt(this.parameterMap.get("processId"));
		ProcessObject po = ProcessManager.getProcessObjectForId(id);
		List<StepObject> stepList = StepManager.getStepsForProcess(id);
		int countOpenSteps = 0;
		for (StepObject so : stepList) {
			if (so.getBearbeitungsstatus() == 1 || so.getBearbeitungsstatus() == 2) {
				countOpenSteps++;
			}
		}
		if (countOpenSteps == 0) {
			// error, no open steps found
		} else if (countOpenSteps > 1) {
			// error, more than one open step found
		} else {
			for (StepObject so : stepList) {
				if (so.getBearbeitungsstatus() == 1 || so.getBearbeitungsstatus() == 2) {
					boolean valid = true;
					String message = "Step not closed";
					if (so.isTypeMetadataAccess() && ConfigMain.getBooleanParameter("useMetadatenvalidierung")) {
						FolderInformation fi = new FolderInformation(id, po.getTitle());
						MetadatenVerifizierungWithoutHibernate mv = new MetadatenVerifizierungWithoutHibernate();
						Prefs prefs = ProcessManager.getRuleset(po.getRulesetId()).getPreferences();
						Fileformat gdzfile;
						try {
							gdzfile = po.readMetadataFile(fi.getMetadataFilePath(), prefs);
							mv.setAutoSave(true);
							if (!mv.validate(gdzfile, prefs, id, po.getTitle())) {
								valid = false;
								message = "Step not closed, metadata could not validated.";
							}
						} catch (PreferencesException e) {
							valid = false;
							message = "Step not closed, metadata could not validated.";
						} catch (ReadException e) {
							valid = false;
							message = "Step not closed, metadata could not validated.";
						} catch (IOException e) {
							valid = false;
							message = "Step not closed, metadata could not validated.";
						}
					}

					/* Imagevalidierung */
					if (so.isTypeWriteAcces()) {
						MetadatenImagesHelper mih = new MetadatenImagesHelper(null, null);
						FolderInformation fi = new FolderInformation(id, po.getTitle());
						try {
							if (!mih.checkIfImagesValid(po.getTitle(), fi.getImagesOrigDirectory(false))) {
								valid = false;
								message = "Step not closed, images not found.";
							}
						} catch (Exception e) {
							valid = false;
							message = "Step not closed, images not found.";
						}
					}
					if (valid) {
						// StepObject so = StepManager.getStepById(id);
						logger.debug("loaded StepObject with id " + so.getId());
						HelperSchritteWithoutHibernate hs = new HelperSchritteWithoutHibernate();
						hs.CloseStepObjectAutomatic(so);
						
						// try to remove symlink from user home 
						if (parameterMap.get("username") != null && parameterMap.get("username").length() > 0) {
							String homeDir = ConfigMain.getParameter("dir_Users");
							String username = parameterMap.get("username");

							String nach = homeDir + username + "/";
							
							nach += po.getTitle() + " [" + po.getId() + "]";

							/* Leerzeichen maskieren */
							nach = nach.replaceAll(" ", "__");
							File benutzerHome = new File(nach);

							String command = ConfigMain.getParameter("script_deleteSymLink") + " ";
							command += benutzerHome;
							// myLogger.debug(command);

							try {
								Helper.callShell(command);
							} catch (java.io.IOException ioe) {
								logger.error("IOException UploadFromHome", ioe);
								String title = "Error during execution";
								message = "Step was closed, but unmount from user home failed";
								return new CommandResponse(500, title, message);
							} catch (InterruptedException e) {
								logger.error("IOException UploadFromHome", e);
								String title = "Error during execution";
								message = "Step was closed, but unmount from user home failed";
								return new CommandResponse(500, title, message);
							}
							
						}
						
						String title = "Command executed";
						message = "Step closed";
						return new CommandResponse(200, title, message);
						// return new CommandResponse(title, message);
					} else {
						String title = "Error during execution";
//						String message = "Step not closed";
						return new CommandResponse(500, title, message);
						// return new CommandResponse(title, message);
					}
				}
			}
		}
		String title = "Error during execution";
		String message = "Step not closed";
		return new CommandResponse(500, title, message);
		// return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command help for closeStepByProcessId";
		String message = "This command closes the first open step and opens the next task. If next task is an automatic task, all scripts gets started." +
				"\n 'processId' defines the process where the first open step gets closed.";
		return new CommandResponse(200, title, message);
		// return new CommandResponse(title, message);
	}

}

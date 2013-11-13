package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.beans.Usergroup;
import org.goobi.beans.HistoryEvent;
import org.goobi.beans.Project;
import org.goobi.beans.Process;
import org.goobi.beans.Step;

import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.persistence.managers.UsergroupManager;

@PluginImplementation
public class AddStepCommand implements ICommandPlugin, IPlugin {

	// private static final Logger logger = Logger.getLogger(AddStepCommand.class);

	private static final String ID = "addStepToProject";

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
		if (!parameterMap.containsKey("userGroupId")) {
			String title = "Missing parameter";
			String message = "No parameter 'userGroupId' defined.";
			return new CommandResponse(400, title, message);
			// return new CommandResponse(title, message);

		}
		if (!parameterMap.containsKey("projectId")) {
			String title = "Missing parameter";
			String message = "No parameter 'projectId' defined.";
			return new CommandResponse(400, title, message);
			// return new CommandResponse(title, message);

		}
		if (!parameterMap.containsKey("stepdata")) {
			String title = "Missing parameter";
			String message = "No parameter 'stepdata' defined.";
			return new CommandResponse(400, title, message);
			// return new CommandResponse(title, message);

		}
		if (!parameterMap.containsKey("orderNumber")) {
			String title = "Missing parameter";
			String message = "No parameter 'orderNumber' defined.";
			return new CommandResponse(400, title, message);
			// return new CommandResponse(title, message);

		}

		return null;
	}

	@Override
	public CommandResponse execute() {
		Integer projectId = Integer.parseInt(parameterMap.get("projectId"));
		Integer userGroupId = Integer.parseInt(parameterMap.get("userGroupId"));
		Integer orderNumber = Integer.parseInt(parameterMap.get("orderNumber"));
		HashMap<String, String> stepdata = generateMap(parameterMap.get("stepdata"));

		
		try {
			Project projekt = ProjectManager.getProjectById(projectId);
		

		List<Process> plist = new ArrayList<Process>();
		plist.addAll(projekt.getProzesse());
		Usergroup b = UsergroupManager.getUsergroupById(userGroupId);
		for (Process p : plist) {
			boolean added = false;
			List<Step> slist = p.getSchritteList();
			Collections.reverse(slist);

			for (Step oldStep : slist) {
				if (!added) {
					int oldOrderNumber = oldStep.getReihenfolge();

					if (oldOrderNumber == orderNumber) {
						// create new step
					    Step newStep = new Step();
						newStep.setReihenfolge(oldOrderNumber + 1);
						if (stepdata.get("titel") != null) {
							newStep.setTitel(stepdata.get("titel"));
						}
						if (stepdata.get("prioritaet") != null) {
							newStep.setPrioritaet(new Integer(stepdata.get("prioritaet")));
						}
						if (stepdata.get("typMetadaten") != null) {
							if (stepdata.get("typMetadaten").equals("true")) {
								newStep.setTypMetadaten(true);
							} else {
								newStep.setTypMetadaten(false);
							}
						}
						if (stepdata.get("typAutomatisch") != null) {
							if (stepdata.get("typAutomatisch").equals("true")) {
								newStep.setTypAutomatisch(true);
							} else {
								newStep.setTypAutomatisch(false);
							}
						}
						if (stepdata.get("typImportFileUpload") != null) {
							if (stepdata.get("typImportFileUpload").equals("true")) {
								newStep.setTypImportFileUpload(true);
							} else {
								newStep.setTypImportFileUpload(false);
							}
						}
						if (stepdata.get("typImagesLesen") != null) {
							if (stepdata.get("typImagesLesen").equals("true")) {
								newStep.setTypImagesLesen(true);
							} else {
								newStep.setTypImagesLesen(false);
							}
						}
						if (stepdata.get("typImagesSchreiben") != null) {
							if (stepdata.get("typImagesSchreiben").equals("true")) {
								newStep.setTypImagesSchreiben(true);
							} else {
								newStep.setTypImagesSchreiben(false);
							}
						}
						if (stepdata.get("typExportDMS") != null) {
							if (stepdata.get("typExportDMS").equals("true")) {
								newStep.setTypExportDMS(true);
							} else {
								newStep.setTypExportDMS(false);
							}
						}
						if (stepdata.get("typBeimAnnehmenModul") != null) {
							if (stepdata.get("typBeimAnnehmenModul").equals("true")) {
								newStep.setTypBeimAnnehmenModul(true);
							} else {
								newStep.setTypBeimAnnehmenModul(false);
							}
						}

						if (stepdata.get("typBeimAnnehmenAbschliessen") != null) {
							if (stepdata.get("typBeimAnnehmenAbschliessen").equals("true")) {
								newStep.setTypBeimAnnehmenAbschliessen(true);
							} else {
								newStep.setTypBeimAnnehmenAbschliessen(false);
							}
						}
						if (stepdata.get("typScriptStep") != null) {
							if (stepdata.get("typScriptStep").equals("true")) {
								newStep.setTypScriptStep(true);
							} else {
								newStep.setTypScriptStep(false);
							}
						}
						if (stepdata.get("scriptname1") != null) {
							newStep.setScriptname1(stepdata.get("scriptname1"));
						}
						if (stepdata.get("typAutomatischScriptpfad") != null) {
							newStep.setTypAutomatischScriptpfad(stepdata.get("typAutomatischScriptpfad"));
						}
						if (stepdata.get("scriptname2") != null) {
							newStep.setScriptname2(stepdata.get("scriptname2"));
						}
						if (stepdata.get("typAutomatischScriptpfad2") != null) {
							newStep.setTypAutomatischScriptpfad2(stepdata.get("typAutomatischScriptpfad2"));
						}
						if (stepdata.get("scriptname3") != null) {
							newStep.setScriptname3(stepdata.get("scriptname3"));
						}
						if (stepdata.get("typAutomatischScriptpfad3") != null) {
							newStep.setTypAutomatischScriptpfad3(stepdata.get("typAutomatischScriptpfad3"));
						}
						if (stepdata.get("scriptname4") != null) {
							newStep.setScriptname4(stepdata.get("scriptname4"));
						}
						if (stepdata.get("typAutomatischScriptpfad4") != null) {
							newStep.setTypAutomatischScriptpfad4(stepdata.get("typAutomatischScriptpfad4"));
						}
						if (stepdata.get("scriptname5") != null) {
							newStep.setScriptname5(stepdata.get("scriptname5"));
						}
						if (stepdata.get("typAutomatischScriptpfad5") != null) {
							newStep.setTypAutomatischScriptpfad5(stepdata.get("typAutomatischScriptpfad5"));
						}

						if (stepdata.get("typModulName") != null) {
							newStep.setTypModulName(stepdata.get("typModulName"));
						}
						if (stepdata.get("typBeimAbschliessenVerifizieren") != null) {
							if (stepdata.get("typBeimAbschliessenVerifizieren").equals("true")) {
								newStep.setTypBeimAbschliessenVerifizieren(true);
							} else {
								newStep.setTypBeimAbschliessenVerifizieren(false);
							}
						}

						newStep.setProzess(p);
						newStep.getBenutzergruppen().add(b);
						newStep.setBearbeitungsbeginn(oldStep.getBearbeitungsende());
						newStep.setEditTypeEnum(StepEditType.AUTOMATIC);
						newStep.setBearbeitungsstatusEnum(oldStep.getBearbeitungsstatusEnum());

						if (oldStep.getBearbeitungsstatusEnum().equals(StepStatus.DONE)) {
							newStep.setBearbeitungsstatusEnum(oldStep.getBearbeitungsstatusEnum());
							newStep.setBearbeitungsbenutzer(oldStep.getBearbeitungsbenutzer());
							newStep.setBearbeitungsende(oldStep.getBearbeitungsende());
						} else {
							newStep.setBearbeitungsstatusEnum(StepStatus.LOCKED);
						}
						p.getSchritte().add(newStep);
						added = true;
					}
				}
			}
			ProcessManager.saveProcess(p);
		}
		} catch (DAOException e1) {
			 String title = "Error during execution";
			 String message = "An error occured: " + e1.getMessage();
			 // return new CommandResponse(500,title, message);
			 return new CommandResponse(title, message);
		}
		// } catch (DAOException e) {
		// logger.error(e);
		// String title = "Error during execution";
		// String message = "An error occured: " + e.getMessage();
		// // return new CommandResponse(500,title, message);
		// return new CommandResponse(title, message);

		String title = "Command executed";
		String message = "Step closed";
		return new CommandResponse(200, title, message);
		// return new CommandResponse(title, message);

	}

	private HashMap<String, String> generateMap(String param) {
		HashMap<String, String> map = new HashMap<String, String>();
		String[] params = param.split(",");
		for (String p : params) {
			String[] values = p.split(":");
			map.put(values[0], values[1]);
		}
		return map;
	}

	@Override
	public CommandResponse help() {
		String title = "Command addStepToProject";
		String message = "This plugin adds a step to all processes of a given project";
		return new CommandResponse(200, title, message);
		// return new CommandResponse(title, message);

	}
}

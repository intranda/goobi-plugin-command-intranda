package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Collections;
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

import de.sub.goobi.Beans.Benutzergruppe;
import de.sub.goobi.Beans.HistoryEvent;
import de.sub.goobi.Beans.Projekt;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Beans.Schritt;
import de.sub.goobi.Persistence.BenutzergruppenDAO;
import de.sub.goobi.Persistence.ProjektDAO;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;

@PluginImplementation
public class AddStepCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(AddStepCommand.class);

	private static final String ID = "addStepToProject";
	private static final String NAME = "AddStep Command Plugin";
	private static final String VERSION = "1.0.20111109";

	private HashMap<String, String> parameterMap;

	@Override
	public PluginType getType() {
		return PluginType.Command;
	}

	@Override
	public String getTitle() {
		return NAME;
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
		if (!parameterMap.containsKey("userGroupId")) {
			String title = "Missing parameter";
			String message = "No parameter 'userGroupId' defined.";
//			return new CommandResponse(400,title, message);
			return new CommandResponse(title, message);

		}
		if (!parameterMap.containsKey("projectId")) {
			String title = "Missing parameter";
			String message = "No parameter 'projectId' defined.";
//			return new CommandResponse(400,title, message);
			return new CommandResponse(title, message);

		}
		if (!parameterMap.containsKey("stepdata")) {
			String title = "Missing parameter";
			String message = "No parameter 'stepdata' defined.";
//			return new CommandResponse(400,title, message);
			return new CommandResponse(title, message);

		}
		if (!parameterMap.containsKey("orderNumber")) {
			String title = "Missing parameter";
			String message = "No parameter 'orderNumber' defined.";
//			return new CommandResponse(400,title, message);
			return new CommandResponse(title, message);

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

			ProjektDAO pdao = new ProjektDAO();
			ProzessDAO pd = new ProzessDAO();
			BenutzergruppenDAO bendao = new BenutzergruppenDAO();
			Projekt projekt = pdao.get(projectId);

			List<Prozess> plist = new ArrayList<Prozess>();
			plist.addAll(projekt.getProzesse());
			Benutzergruppe b = bendao.get(userGroupId);
			for (Prozess p : plist) {
				boolean added = false;
				List<Schritt> slist = p.getSchritteList();
				Collections.reverse(slist);

				for (Schritt oldStep : slist) {
					if (!added) {
						int oldOrderNumber = oldStep.getReihenfolge();

						if (oldOrderNumber == orderNumber) {
							// create new step
							Schritt newStep = new Schritt();
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
								p.getHistory().add(
										new HistoryEvent(oldStep.getBearbeitungsende(), new Double(oldOrderNumber + 1).doubleValue(), newStep
												.getTitel(), HistoryEventType.stepDone, p));
							} else {
								newStep.setBearbeitungsstatusEnum(StepStatus.LOCKED);
							}
							p.getSchritte().add(newStep);
							added = true;
						} else {
							oldStep.setReihenfolge(oldOrderNumber + 1);
							List<HistoryEvent> history = p.getHistoryList();
							for (HistoryEvent e : history) {
								if (e.getStringValue() != null && e.getStringValue().equals(oldStep.getTitel())) {
									if (e.getNumericValue() != null && e.getNumericValue() == new Double(oldOrderNumber)) {
										e.setNumericValue(new Double(oldOrderNumber + 1));
									}
								}
							}
						}
					}
				}
				pd.save(p);
			}

		} catch (DAOException e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
//			return new CommandResponse(500,title, message);
			return new CommandResponse(title, message);

		}

		String title = "Command executed";
		String message = "Step closed";
//		return new CommandResponse(200,title, message);
		return new CommandResponse(title, message);

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
		String title = "Command help";
		String message = "this is the help for a command";
//		return new CommandResponse(200,title, message);
		return new CommandResponse(title, message);

	}

}

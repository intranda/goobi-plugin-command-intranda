package de.intranda.goobi.plugins;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.Import.ImportObject;
import org.goobi.production.Import.Record;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.flow.helper.JobCreation;
import org.goobi.production.plugin.PluginLoader;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IImportPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.hibernate.Session;

import ugh.dl.Prefs;
import de.intranda.goobi.plugins.helper.ConnectionHelper;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Persistence.HibernateUtilOld;
import de.sub.goobi.config.ConfigMain;

@PluginImplementation
public class ImportRecordCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(ImportRecordCommand.class);
	private static final String ID = "importRecordCommand";
	// private static final String NAME = "intranda Import Record Command Plugin";
	// private static final String VERSION = "1.0.20120716";
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
		if (!this.parameterMap.containsKey("filename")) {
			String title = "Missing parameter";
			String message = "No parameter 'filename' defined.";
			return new CommandResponse(400, title, message);
		}
		if (!this.parameterMap.containsKey("templateId")) {
			String title = "Missing parameter";
			String message = "No parameter 'templateId' defined.";
			return new CommandResponse(400, title, message);
		}

		return null;
	}

	@Override
	public CommandResponse execute() {
		Session session = HibernateUtilOld.getSessionFactory().openSession();
		if (!session.isOpen() || !session.isConnected()) {
			Connection con = ConnectionHelper.getConnection();
			session.reconnect(con);
		}
		try {
			Integer id = Integer.parseInt(this.parameterMap.get("templateId"));

			Prozess template = (Prozess) session.get(Prozess.class, id);
			// ProzessDAO dao = new ProzessDAO();
			// Prozess template = dao.get(id);

			String filename = parameterMap.get("filename");

			IImportPlugin wmi = (IImportPlugin) PluginLoader.getPluginByTitle(PluginType.Import, "goobiImport");
			// IImportPlugin wmi = (IImportPlugin) PluginLoader.getPlugin(PluginType.Import, "IntrandaGoobiImport");

			List<String> filenameList = new ArrayList<String>();
			filenameList.add(filename);
			List<Record> recordList = new ArrayList<Record>();
			List<ImportObject> answer = new ArrayList<ImportObject>();
			Prefs prefs = template.getRegelsatz().getPreferences();
			String tempfolder = ConfigMain.getParameter("tempfolder");
			wmi.setImportFolder(tempfolder);
			wmi.setPrefs(prefs);
			recordList = wmi.generateRecordsFromFilenames(filenameList);
			answer = wmi.generateFiles(recordList);

			if (answer != null && answer.size() > 0) {
				ImportObject io = answer.get(0);
				if (!io.getImportReturnValue().equals(ImportReturnValue.ExportFinished)) {
					String title = "Error during execution";
					String message = "import failed for " + io.getProcessTitle() + ", process generation failed: "
							+ io.getImportReturnValue().getValue();
					return new CommandResponse(500, title, message);
				}
				Prozess p = JobCreation.generateProcess(io, template);

				if (p == null) {
					String title = "Error during execution";
					String message = "import failed for " + io.getProcessTitle() + ", process generation failed";
					return new CommandResponse(500, title, message);
				} else {
					wmi.deleteFiles(filenameList);
					String title = "Command executed";
					String message = ImportReturnValue.ExportFinished.getValue() + " for " + io.getProcessTitle() + ", the Goobi id is "
							+ String.valueOf(p.getId());
					return new CommandResponse(title, message);
				}
			} else {
				String title = "Error during execution";
				String message = "import failed for " + filename + ", process generation failed";
				return new CommandResponse(500, title, message);
			}
		} catch (Exception e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(200, title, message);
		} finally {
			session.close();
		}
	}

	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(200, title, message);
	}
}

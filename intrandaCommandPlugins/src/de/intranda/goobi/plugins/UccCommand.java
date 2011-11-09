package de.intranda.goobi.plugins;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.export.ExportXmlLog;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.File;
import de.schlichtherle.io.FileInputStream;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.config.ConfigMain;

@PluginImplementation
public class UccCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(UccCommand.class);

	private static final String ID = "ucc";
	private static final String NAME = "UCC Command Plugin";
	private static final String VERSION = "1.0.20111109";

	private HashMap<String, String> parameterMap;
	private HttpServletResponse response;

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
		return true;
	}

	@Override
	public void setHttpResponse(HttpServletResponse resp) {
		response = resp;
	}

	@Override
	public void setHttpRequest(HttpServletRequest req) {
	}

	@Override
	public CommandResponse validate() {

		if (!parameterMap.containsKey("processId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' defined";
			return new CommandResponse(title, message);
		}

		return null;
	}

	@Override
	public CommandResponse execute() {

		Integer processId = Integer.parseInt(parameterMap.get("processId"));

		InputStream in = null;
		try {

			Prozess process = new ProzessDAO().get(processId);

			File meta = new File(process.getMetadataFilePath());
			File anchor = new File(process.getMetadataFilePath().replace("meta.xml", "meta_anchor.xml"));
			File ruleset = new File(ConfigMain.getParameter("RegelsaetzeVerzeichnis") + process.getRegelsatz().getDatei());

			ExportXmlLog export = new ExportXmlLog();
			File log = new File(ConfigMain.getParameter("tempfolder") + "logfile.xml");
			export.startExport(process, log);

			File.setDefaultArchiveDetector(new DefaultArchiveDetector("tar.bz2|tar.gz|zip"));
			File backup = new File(ConfigMain.getParameter("tempfolder") + "backup.zip");
			if (backup.exists() && backup.isArchive()) {
				// Empty archive if it already exists
				backup.deleteAll();
			}
			meta.copyTo(new File(backup + File.separator + "meta.xml"));
			if (anchor.exists()) {
				anchor.copyTo(new File(backup + File.separator + "meta_anchor.xml"));
			}
			ruleset.copyTo(new File(backup + File.separator + "ruleset.xml"));
			log.copyTo(new File(backup + File.separator + "logfile.xml"));
			File.umount();

			// output data
			String fileName = "ucc_data_" + processId + ".zip";

			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
			OutputStream out = response.getOutputStream();

			in = new FileInputStream(backup);
			int numRead;
			byte[] buf = new byte[4096];
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.flush();
			in.close();
		} catch (Exception e) {
			logger.error(e);
		}
		String title = "Command executed";
		String message = "UCC download started";
		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(title, message);
	}
}

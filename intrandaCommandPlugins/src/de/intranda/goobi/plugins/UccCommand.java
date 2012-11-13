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
import org.hibernate.Session;

import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.File;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Persistence.HibernateUtilOld;
import de.sub.goobi.Persistence.ProzessDAO;
//import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.config.ConfigMain;

@PluginImplementation
public class UccCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(UccCommand.class);

	private static final String ID = "ucc";
//	private static final String NAME = "UCC Command Plugin";
//	private static final String VERSION = "1.0.20111109";

	private HashMap<String, String> parameterMap;
	private HttpServletResponse response;

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
		return true;
	}

	@Override
	public void setHttpResponse(HttpServletResponse resp) {
		this.response = resp;
	}

	@Override
	public void setHttpRequest(HttpServletRequest req) {
	}

	@Override
	public CommandResponse validate() {

		if (!this.parameterMap.containsKey("processId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' defined";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}

		return null;
	}

	@Override
	public CommandResponse execute() {

		Integer processId = Integer.parseInt(this.parameterMap.get("processId"));

		InputStream in = null;
//		Session session = HibernateUtilOld.getSessionFactory().openSession();
//		if (!session.isOpen() || !session.isConnected()) {
//			Connection con = ConnectionHelper.getConnection();
//			session.reconnect(con);
//		}
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

			this.response.setContentType("application/zip");
			this.response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
			OutputStream out = this.response.getOutputStream();

			
			in = new java.io.FileInputStream(backup);
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
		return new CommandResponse(200, title, message);
//		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command ucc";
		String message = "The command 'ucc' sends a sip file for a specific process to the requester.";
		message += "\n The parameter 'processId' defines to which process the sip file belongs.";
		return new CommandResponse(200, title, message);
//		return new CommandResponse(title, message);
	}
}

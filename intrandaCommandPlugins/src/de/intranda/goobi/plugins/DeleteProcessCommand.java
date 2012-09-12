package de.intranda.goobi.plugins;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.hibernate.Session;

import de.intranda.goobi.plugins.helper.ConnectionHelper;
import de.sub.goobi.Beans.Benutzer;
import de.sub.goobi.Beans.Benutzergruppe;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Beans.Schritt;
import de.sub.goobi.Persistence.HibernateUtilOld;
//import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.WebDav;

@PluginImplementation
public class DeleteProcessCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(DeleteProcessCommand.class);

	private static final String ID = "deleteProcess";
//	private static final String NAME = "DeleteProcess Command Plugin";
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
		if (!parameterMap.containsKey("processId")) {
			String title = "Missing parameter";
			String message = "No parameter 'processId' defined.";
			return new CommandResponse(400,title, message);
//			return new CommandResponse(title, message);
		}
		return null;
	}

	@Override
	public CommandResponse execute() {
		Integer id = Integer.parseInt(parameterMap.get("processId"));
		Session session = HibernateUtilOld.getSessionFactory().openSession();
		if (!session.isOpen() || !session.isConnected()) {
			Connection con = ConnectionHelper.getConnection();
			session.reconnect(con);
		}
		try {
		
			Prozess p = (Prozess) session.get(Prozess.class, id);
			for (Schritt step : p.getSchritteList()) {
				WebDav myDav = new WebDav();
				for (Benutzer b : step.getBenutzerList()) {
					try {
						myDav.UploadFromHome(b, p);
					} catch (RuntimeException e) {
					}
				}
				for (Benutzergruppe bg : step.getBenutzergruppenList()) {
					for (Benutzer b : bg.getBenutzerList()) {
						try {
							myDav.UploadFromHome(b, p);
						} catch (RuntimeException e) {
						}
					}
				}
			}

			Helper.deleteDir(new File(p.getProcessDataDirectory()));
//			dao.remove(id);
			session.delete(p);
		} catch (Exception e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(500,title, message);
//			return new CommandResponse(title, message);
		} finally {
			session.close();
		}

		String title = "Command executed";
		String message = "Process deleted";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

}

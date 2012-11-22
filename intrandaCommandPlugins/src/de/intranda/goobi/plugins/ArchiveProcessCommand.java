package de.intranda.goobi.plugins;

import java.io.File;
import java.util.HashMap;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.intranda.goobi.archiving.Archiver;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Export.download.ExportMets;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.helper.Helper;

@PluginImplementation
public class ArchiveProcessCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(ArchiveProcessCommand.class);

	private static final String dashes = "--------------------";
	private static File configFile;
	private static String defaultConfigFilePath = "archiving.properties";

	private static final String ID = "archiveProcess";
//	private static final String NAME = "Archiving Command Plugin";
//	private static final String VERSION = "1.0.20120518";

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
		
		
		//get config
//		if(parameterMap.containsKey("config")) {
//			String configPath = parameterMap.get("config");
//			logger.debug("Found config path parameter " + configPath);
//			if(configPath != null && !configPath.isEmpty() && !configPath.contentEquals("null")) {				
//				configFile = new File(configPath);
//			}
//		}
		//check if we have a valid config path
		
		if(configFile == null || !configFile.isFile()) {
			Helper help = new Helper();
			configFile = new File(help.getGoobiConfigDirectory(), defaultConfigFilePath);
			if(!configFile.isFile()) {
				logger.error("Unable to locate config file. Aborting");
				String title = "Missing resources";
				String message = "Unable to locate config file";
				return new CommandResponse(500,title, message);
//				return new CommandResponse(title, message);
			}
		}
		
		Prozess p = null;
		File processDir = null;
		File origImagesDir = null;
//		Session session = HibernateUtilOld.getSessionFactory().openSession();
//		if (!session.isOpen() || !session.isConnected()) {
//			Connection con = ConnectionHelper.getConnection();
//			session.reconnect(con);
//		}
		try {
			//get Process data
			
			p = new ProzessDAO().get(Integer.valueOf(parameterMap.get("processId")));
//			p = dao.get(Integer.valueOf(parameterMap.get("processId")));
			processDir = new File(p.getProcessDataDirectory());
			origImagesDir = new File(p.getImagesOrigDirectory(true));
		} catch (Exception e) {
			logger.error("Unable to find process");
			String title = "Process Error";
			String message = "Unable to find process";
			return new CommandResponse(500,title, message);
//			return new CommandResponse(title, message);
		} 	
			//create exported mets
		try {
			File exportedMetsDir = new File(processDir, "exported_mets");
			logger.debug("attempting to create exported mets in " + exportedMetsDir.getAbsolutePath());
			exportedMetsDir.mkdirs();
			ExportMets exportMets = new ExportMets();
			exportMets.startExport(p, exportedMetsDir.getAbsolutePath() + "/");
		} catch (Exception e) {
			System.out.println(e.toString() + "\n" +  e);
			logger.error("Unable to create exported Mets");
			String title = "Process Error";
			String message = "Unable to create exported Mets";
			return new CommandResponse(500,title, message);
//			return new CommandResponse(title, message);
		} 
		
		//start archiving
		Archiver archiver = new Archiver(configFile, origImagesDir, p.getTitel());
		try {
		logger.info(dashes + "Starting Goobi Archiver"+ dashes);
		archiver.start();
		archiver.join();
		} catch(InterruptedException e) {
			logger.debug("Archiving interrupted");
		}
		logger.info(dashes + "Closing Goobi Archiver" + dashes);
		
		String title = "Done Archiving";
		String message = "Archive created: " + archiver.getArchive();
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command archiveProcess";
		String message = "Syntax: \"command=archiveProcess\"; \"processId=[The processId of the Goobi process to be archived]\".";
		message = message.concat("\nThis plugin requires the file \"archiving.properties\" in the goobi/config folder. There, additional parameters such as destination path can be adjusted.");
		return new CommandResponse(200,title, message);
//		return new CommandResponse(title, message);
	}

	

}

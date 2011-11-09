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
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.schlichtherle.io.File;
import de.schlichtherle.io.FileOutputStream;
import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.config.ConfigMain;

@PluginImplementation
public class UccUploadCommand implements ICommandPlugin, IPlugin {

	private static final Logger logger = Logger.getLogger(UccUploadCommand.class);

	private static final String ID = "ucc_upload";
	private static final String NAME = "UCC Upload Command Plugin";
	private static final String VERSION = "1.0.20111109";

	private HashMap<String, String> parameterMap;
	private HttpServletResponse response;
	private HttpServletRequest request;

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
		request = req;
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
		try {
			InputStream in = request.getInputStream();
			File archive = new File(ConfigMain.getParameter("tempfolder"), processId + ".zip");
			OutputStream out = new FileOutputStream(archive);
			int numRead;
			byte[] buf = new byte[4096];
			while ((numRead = in.read(buf)) >= 0) {
				out.write(buf, 0, numRead);
			}
			out.flush();

			Prozess process = new ProzessDAO().get(processId);
			File metaDest = new File(process.getMetadataFilePath());
			File anchorDest = new File(process.getMetadataFilePath().replace("meta.xml", "meta_anchor.xml"));

			File metaSource = new File(archive, "meta.xml");
			File anchorSource = new File(archive, "meta_anchor.xml");

			metaSource.copyTo(metaDest);
			if (anchorSource.exists()) {
				anchorSource.copyTo(anchorDest);
			}
		} catch (Exception e) {
			logger.error(e);
			String title = "Error during execution";
			String message = "An error occured: " + e.getMessage();
			return new CommandResponse(title, message);
		}

		String title = "Command executed";
		String message = "Message to process log added";
		return new CommandResponse(title, message);
	}

	@Override
	public CommandResponse help() {
		String title = "Command help";
		String message = "this is the help for a command";
		return new CommandResponse(title, message);
	}
}

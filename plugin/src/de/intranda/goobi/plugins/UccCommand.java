package de.intranda.goobi.plugins;

/**
 * This file is part of a plugin for the Goobi Application - a Workflow tool for the support of mass digitization.
 * 
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * Linking this library statically or dynamically with other modules is making a combined work based on this library. Thus, the terms and conditions
 * of the GNU General Public License cover the whole combination. As a special exception, the copyright holders of this library give you permission to
 * link this library with independent modules to produce an executable, regardless of the license terms of these independent modules, and to copy and
 * distribute the resulting executable under terms of your choice, provided that you also meet, for each linked independent module, the terms and
 * conditions of the license of that module. An independent module is a module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but you are not obliged to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.beans.Process;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.schlichtherle.io.DefaultArchiveDetector;
import de.schlichtherle.io.File;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.persistence.managers.ProcessManager;
import io.goobi.workflow.xslt.XsltPreparatorMetadata;
import net.xeoh.plugins.base.annotations.PluginImplementation;

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
            return new CommandResponse(400, title, message);
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

            Process process = ProcessManager.getProcessById(processId);

            File meta = new File(process.getMetadataFilePath());
            File anchor = new File(process.getMetadataFilePath().replace("meta.xml", "meta_anchor.xml"));
            File ruleset = new File(ConfigurationHelper.getInstance().getRulesetFolder() + process.getRegelsatz().getDatei());

//            XsltPreparatorMetadata export = new XsltPreparatorMetadata();
//            File log = new File(ConfigurationHelper.getInstance().getTemporaryFolder() + "logfile.xml");
//            export.startExport(process, log.toString());

            File.setDefaultArchiveDetector(new DefaultArchiveDetector("tar.bz2|tar.gz|zip"));
            File backup = new File(ConfigurationHelper.getInstance().getTemporaryFolder() + "backup.zip");
            if (backup.exists() && backup.isArchive()) {
                // Empty archive if it already exists
                backup.deleteAll();
            }
            meta.copyTo(new File(backup + java.io.File.separator + "meta.xml"));
            if (anchor.exists()) {
                anchor.copyTo(new File(backup + java.io.File.separator + "meta_anchor.xml"));
            }
            ruleset.copyTo(new File(backup + java.io.File.separator + "ruleset.xml"));
//            log.copyTo(new File(backup + java.io.File.separator + "logfile.xml"));
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
            logger.error(e.toString(), e);

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

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.goobi.beans.ErrorProperty;
import org.goobi.beans.Step;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.PropertyType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.HistoryManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
public class ReportProblemCommand implements ICommandPlugin, IPlugin {

    // private static final Logger logger = Logger.getLogger(ReportProblemCommand.class);
    private static final String ID = "reportProblem";
    private static final String NAME = "ReportProblem Command Plugin";
    private HashMap<String, String> parameterMap;

    @Override
    public String getTitle() {
        return ID;
    }

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    public String getDescription() {
        return NAME;
    }

    public String getId() {
        return ID;
    }

    @Override
    public void setParameterMap(HashMap<String, String> parameterMap) {
        this.parameterMap = parameterMap;
    }

    @Override
    public CommandResponse validate() {
        if (!this.parameterMap.containsKey("stepId")) {
            String title = "Missing parameter";
            String message = "No parameter 'stepId' defined.";
            return new CommandResponse(400, title, message);
            // return new CommandResponse(title, message);
        } else if (!this.parameterMap.containsKey("errorMessage")) {
            String title = "Missing parameter";
            String message = "No parameter 'errorMessage' defined.";
            return new CommandResponse(400, title, message);
            // return new CommandResponse(title, message);
        } else if (!this.parameterMap.containsKey("destinationStepName")) {
            String title = "Missing parameter";
            String message = "No parameter 'destinationStepName' defined.";
            return new CommandResponse(400, title, message);
            // return new CommandResponse(title, message);
        }

        // } else if (!this.parameterMap.containsKey("user")) {
        // String title = "Missing parameter";
        // String message = "No parameter 'user' defined.";
        // return new CommandResponse(title, message);
        String stepId = this.parameterMap.get("stepId");
        try {
            Integer.parseInt(stepId);
        } catch (Exception e) {
            String title = "Wrong value";
            String message = "value for parameter 'stepId' is not a valid number.";
            return new CommandResponse(400, title, message);
            // return new CommandResponse(title, message);
        }
        // String destinationStepId = this.parameterMap.get("destinationStepId");
        // try {
        // Integer.parseInt(destinationStepId);
        // } catch (Exception e) {
        // String title = "Wrong value";
        // String message = "value for parameter 'destinationStepId' is not a valid number.";
        // return new CommandResponse(title, message);
        // }

        return null;
    }

    @Override
    public CommandResponse execute() {
        Date myDate = new Date();
        Integer sourceid = Integer.parseInt(this.parameterMap.get("stepId"));
        String destinationTitle = this.parameterMap.get("destinationStepName");
        // Integer targetid = Integer.parseInt(this.parameterMap.get("destinationStepName"));
        String errorMessage = this.parameterMap.get("errorMessage");

        //		if (!session.isOpen() || !session.isConnected()) {
        //			Connection con = ConnectionHelper.getConnection();
        //			session.reconnect(con);
        //		}
        // SchrittDAO dao = new SchrittDAO();
        // ProzessDAO pdao = new ProzessDAO();
        try {
            Step source = StepManager.getStepById(sourceid);
            // Schritt source = (Schritt) dao.get(sourceid);
            source.setBearbeitungsstatusEnum(StepStatus.LOCKED);
            source.setEditTypeEnum(StepEditType.MANUAL_SINGLE);
            source.setBearbeitungszeitpunkt(new Date());
            // Benutzer ben = (Benutzer) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
            // if (ben != null) {
            // source.setBearbeitungsbenutzer(ben);
            // }
            source.setBearbeitungsbeginn(null);
            Step temp = null;
            for (Step s : source.getProzess().getSchritteList()) {
                if (s.getTitel().equals(destinationTitle)) {
                    temp = s;
                }
            }
            if (temp != null) {
                temp.setBearbeitungsstatusEnum(StepStatus.OPEN);
                temp.setCorrectionStep();
                temp.setBearbeitungsende(null);
                Helper.addMessageToProcessJournal(temp.getProzess().getId(), LogType.ERROR,
                        Helper.getTranslation("Korrektur notwendig") + " [automatic] " + errorMessage, "webapi");

                // dao.save(temp);
                HistoryManager.addHistory(myDate, temp.getReihenfolge().doubleValue(), temp.getTitel(), HistoryEventType.stepError.getValue(),
                        temp.getProzess().getId());

                List<Step> alleSchritteDazwischen = new ArrayList<>();
                for (Step s : source.getProzess().getSchritteList()) {
                    if (s.getReihenfolge() <= source.getReihenfolge() && s.getReihenfolge() > temp.getReihenfolge()) {
                        alleSchritteDazwischen.add(s);
                    }
                }

                /*
                 * alle Schritte zwischen dem aktuellen und dem Korrekturschritt wieder schliessen
                 */

                //                List<Step> alleSchritteDazwischen = StepManager.getSteps("Reihenfolge desc", " schritte.prozesseID = " + source.getProzess().getId()
                //                        + " AND Reihenfolge <= " + source.getReihenfolge() + "  AND Reihenfolge > " + temp.getReihenfolge(), 0, Integer.MAX_VALUE);
                // List<Schritt> alleSchritteDazwischen = Helper.getHibernateSession().createCriteria(Schritt.class)
                // .add(Restrictions.le("reihenfolge", source.getReihenfolge())).add(Restrictions.gt("reihenfolge", temp.getReihenfolge()))
                // .addOrder(Order.asc("reihenfolge")).createCriteria("prozess").add(Restrictions.idEq(source.getProzess().getId())).list();
                for (Step step : alleSchritteDazwischen) {
                    step.setBearbeitungsstatusEnum(StepStatus.LOCKED);
                    // if (step.getPrioritaet().intValue() == 0)
                    step.setCorrectionStep();
                    step.setBearbeitungsende(null);
                    ErrorProperty seg = new ErrorProperty();
                    seg.setTitel(Helper.getTranslation("Korrektur notwendig"));
                    seg.setWert(Helper.getTranslation("KorrekturFuer") + temp.getTitel() + ": " + errorMessage);
                    seg.setSchritt(step);
                    seg.setType(PropertyType.MESSAGE_IMPORTANT);
                    seg.setCreationDate(new Date());
                    step.getEigenschaften().add(seg);

                    StepManager.saveStep(step);
                }
                ProcessManager.saveProcess(source.getProzess());
                // pdao.save(source.getProzess());
            }

        } catch (DAOException e) {
            String title = "Error during execution";
            String message = "An error occured: " + e.getMessage();
            // return new CommandResponse(500,title, message);
            return new CommandResponse(title, message);
        }

        String title = "Command executed";
        String message = "Problem reported";
        return new CommandResponse(200, title, message);
        // return new CommandResponse(title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command reportProblem";
        StringBuilder message = new StringBuilder("This command reports a problem to a previous task.");
        message.append("\n - 'stepId' defines the task where the problem is noticed..");
        message.append("\n - 'errorMessage' defines the message for the error.");
        message.append("\n - 'destinationStepName' defines the name (not the ID) of the destination for the report.");
        return new CommandResponse(200, title, message.toString());
    }

    @Override
    public boolean usesHttpSession() {
        return false;
    }

    @Override
    public void setHttpResponse(HttpServletResponse resp) {
    }

    @Override
    public void setHttpRequest(HttpServletRequest resp) {
    }

}

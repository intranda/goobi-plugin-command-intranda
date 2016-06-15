package de.intranda.goobi.plugins;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.goobi.production.cli.CommandResponse;
import org.goobi.production.cli.helper.WikiFieldHelper;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.beans.Step;
import org.goobi.beans.ErrorProperty;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.enums.HistoryEventType;
import de.sub.goobi.helper.enums.PropertyType;
import de.sub.goobi.helper.enums.StepEditType;
import de.sub.goobi.helper.enums.StepStatus;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.HistoryManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.StepManager;

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
                ErrorProperty se = new ErrorProperty();
                // Benutzer ben = (Benutzer) Helper.getManagedBeanValue("#{LoginForm.myBenutzer}");
                se.setTitel(Helper.getTranslation("Korrektur notwendig"));
                se.setWert("[automatic] " + errorMessage);
                se.setType(PropertyType.messageError);
                se.setCreationDate(myDate);
                se.setSchritt(temp);
                source.getProzess().setWikifield(WikiFieldHelper.getWikiMessage(source.getProzess().getWikifield(), "error", errorMessage));

                temp.getEigenschaften().add(se);

                StepManager.saveStep(temp);
                // dao.save(temp);
                HistoryManager.addHistory(myDate, temp.getReihenfolge().doubleValue(), temp.getTitel(),
                        HistoryEventType.stepError.getValue(), temp.getProzess().getId());

                /*
                 * alle Schritte zwischen dem aktuellen und dem Korrekturschritt wieder schliessen
                 */

                List<Step> alleSchritteDazwischen =
                        StepManager.getSteps("Reihenfolge desc", " schritte.prozesseID = " + source.getProzess().getId() + " AND Reihenfolge <= "
                                + source.getReihenfolge() + "  AND Reihenfolge > " + temp.getReihenfolge(), 0, Integer.MAX_VALUE);
                // List<Schritt> alleSchritteDazwischen = Helper.getHibernateSession().createCriteria(Schritt.class)
                // .add(Restrictions.le("reihenfolge", source.getReihenfolge())).add(Restrictions.gt("reihenfolge", temp.getReihenfolge()))
                // .addOrder(Order.asc("reihenfolge")).createCriteria("prozess").add(Restrictions.idEq(source.getProzess().getId())).list();
                for (Iterator<Step> iter = alleSchritteDazwischen.iterator(); iter.hasNext();) {
                    Step step = iter.next();
                    step.setBearbeitungsstatusEnum(StepStatus.LOCKED);
                    // if (step.getPrioritaet().intValue() == 0)
                    step.setCorrectionStep();
                    step.setBearbeitungsende(null);
                    ErrorProperty seg = new ErrorProperty();
                    seg.setTitel(Helper.getTranslation("Korrektur notwendig"));
                    seg.setWert(Helper.getTranslation("KorrekturFuer") + temp.getTitel() + ": " + errorMessage);
                    seg.setSchritt(step);
                    seg.setType(PropertyType.messageImportant);
                    seg.setCreationDate(new Date());
                    step.getEigenschaften().add(seg);
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
        String message = "This command reports a problem to a previous task.";
        message += "\n - 'stepId' defines the task where the problem is noticed..";
        message += "\n - 'errorMessage' defines the message for the error.";
        message += "\n - 'destinationStepName' defines the name (not the ID) of the destination for the report.";
        return new CommandResponse(200, title, message);
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

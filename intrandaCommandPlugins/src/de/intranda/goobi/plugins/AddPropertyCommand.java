package de.intranda.goobi.plugins;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.xeoh.plugins.base.annotations.PluginImplementation;

import org.apache.log4j.Logger;
import org.goobi.production.cli.CommandResponse;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.goobi.production.plugin.interfaces.IPlugin;

import de.sub.goobi.Beans.Prozess;
import de.sub.goobi.Beans.Prozesseigenschaft;
import de.sub.goobi.Persistence.ProzessDAO;
import de.sub.goobi.helper.exceptions.DAOException;

@PluginImplementation
public class AddPropertyCommand implements ICommandPlugin, IPlugin {

    private static final Logger logger = Logger.getLogger(AddPropertyCommand.class);

    private static final String ID = "AddProperty";

    private HashMap<String, String> parameterMap;

    @Override
    public PluginType getType() {
        return PluginType.Command;
    }

    @Override
    public String getTitle() {
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
    public CommandResponse validate() {
        if (!parameterMap.containsKey("processId")) {
            String title = "Missing parameter";
            String message = "No parameter 'processId' defined.";
            return new CommandResponse(400, title, message);
        }
        if (!parameterMap.containsKey("property")) {
            String title = "Missing parameter";
            String message = "No parameter 'property' defined.";
            return new CommandResponse(400, title, message);
        }
        if (!parameterMap.containsKey("value")) {
            String title = "Missing parameter";
            String message = "No parameter 'value' defined.";
            return new CommandResponse(400, title, message);
        }

        return null;
    }

    @Override
    public CommandResponse execute() {
        boolean overwriteExistingProperty = false;

        if (parameterMap.get("overwriteExistingProperty") != null && parameterMap.get("overwriteExistingProperty").equalsIgnoreCase("true")) {
            overwriteExistingProperty = true;
        }

        Integer processID = null;
        try {
            processID = Integer.parseInt(parameterMap.get("processId"));
        } catch (Exception e) {
            String title = "Wrong value.";
            String message = "The parameter 'processID' cannot be interpreted as a number.";
            return new CommandResponse(400, title, message);
        }
        String propertyName = parameterMap.get("property");
        String propertyValue = parameterMap.get("value");

        if (propertyName == null || propertyName.isEmpty()) {
            String title = "Wrong value.";
            String message = "The parameter 'property' is empty.";
            return new CommandResponse(400, title, message);
        }
        try {
            propertyName = URLDecoder.decode(propertyName, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            logger.warn("cannot encode " + propertyName);
        }
        
        try {
            propertyValue = URLDecoder.decode(propertyValue, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            logger.warn("cannot encode " + propertyValue);
        }
        
        Prozess process = null;
        ProzessDAO dao = new ProzessDAO();
        try {
            process = dao.get(processID);
        } catch (DAOException e) {
            logger.error(e);
            String title = "Process cannot be loaded.";
            String message = "Cannot load process from database See the logfile for more information.";
            return new CommandResponse(400, title, message);
        }
        boolean propertyExistsAlready = false;
        if (overwriteExistingProperty) {
            Set<Prozesseigenschaft> propertyList = process.getEigenschaften();
            for (Prozesseigenschaft property : propertyList) {
                if (property.getTitel().equalsIgnoreCase(propertyName)) {
                    propertyExistsAlready = true;
                    property.setWert(propertyValue);
                    break;
                }
            }
        }
        if (!propertyExistsAlready) {
            Prozesseigenschaft pe = new Prozesseigenschaft();
            pe.setContainer(0);
            pe.setTitel(propertyName);
            pe.setWert(propertyValue);
            pe.setProzess(process);
            process.getEigenschaften().add(pe);
        }
        try {
            dao.save(process);
        } catch (DAOException e) {
            logger.error(e);
            String title = "Process cannot be saved.";
            String message = "Error during saving of process. See the logfile for more information.";
            return new CommandResponse(400, title, message);
        }
        String title = "Command executed";
        String message = "Created new property.";
        return new CommandResponse(200, title, message);
    }

    @Override
    public CommandResponse help() {
        String title = "Command AddProperty";
        String message = "This command creates a property for processes.";
        message += "\n - 'processId' is mandatory. 'processId' defines the id of the process.";
        message += "\n - 'property' is mandatory. 'property' defines the name of the property.";
        message += "\n -  'value' is mandatory. The value defines the value of the property.";
        message += "\n -  'overwriteExistingProperty' is optional. The value defines if the value of an existing property with the same name gets overwritten or if a new one gets created.";
        
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

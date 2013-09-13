package de.intranda.goobi.plugins;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.ICommandPlugin;
import org.junit.Test;

public class CloseStepByProcessIdCommandTest {

    @Test
    public void testGetType() {
        ICommandPlugin ip = new CloseStepByProcessIdCommand();
        assertEquals(ip.getType(), PluginType.Command);
    }

    @Test
    public void testGetTitle() {
        ICommandPlugin ip = new CloseStepByProcessIdCommand();
        assertEquals(ip.getTitle(), "closeStepByProcessId");
    }
    
    @Test
    public void testGetDescription() {
        ICommandPlugin ip = new CloseStepByProcessIdCommand();
        assertEquals(ip.getDescription(), "closeStepByProcessId");
    }
    
    
    @Test
    public void testParameterMap() {
        HashMap<String, String> parameter = new HashMap<String, String>();
        parameter.put("key", "value");
        ICommandPlugin ip = new CloseStepByProcessIdCommand();
        ip.setParameterMap(parameter);
        fail();
    }
    
}

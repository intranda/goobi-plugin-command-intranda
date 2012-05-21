package de.intranda.goobi.archiving;


import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

/**
 * Archivierung als zip-Datei inlc. Validierung eines Goobi-Vorganges
 * 
 * @author florian
 * 
 */
public class Main {

	private static final Logger logger = Logger.getLogger(Main.class);
	private static final String dashes = "--------------------";
	private static CommandLine cmd;
	private static File configFile;
	private static String defaultConfigFilePath = "archiving.properties";
	
	public static void main(String[] args) {

		//Commond line options
		Options options = new Options();
		options.addOption("h", "help", false, "show help");
		options.addOption("s", "source", true, "Folder containing original image files to be archived (mandatory)");
		options.addOption("p", "process", true, "Title of the Goobi process to be archived (mandatory)");
		options.addOption("t", "topStructID", true, "Catalog number of the process to be archived, or its anchor (mandatory)");
		options.addOption("f", "firstChildID", true, "Catalog number of the process to be archived, if it has an anchor, null otherwise (mandatory)");
		options.addOption("c", "config", true, "Path of config file. If not set, config file is assumed to be \"archiving.properties\" in the same directory as the executable");
		
		//reading command line options
		try {
		CommandLineParser parser = new PosixParser();
		cmd = parser.parse(options, args);
		} catch(ParseException e) {
			logger.error("Unable to parse command line arguments. Aborting");
			System.exit(-1);
		}

		//display help
		if (cmd.hasOption("h") || cmd.getOptions().length == 0 || !cmd.hasOption("s") || !cmd.hasOption("p") || !cmd.hasOption("t") || !cmd.hasOption("f")) {
			System.out.println(dashes + "Help mode for Goobi Archiving" + dashes);
			System.out.println("*Possible parameters (using no parameter at all will result in this message):");
			for (Object object : options.getOptions()) {
				if (object instanceof Option) {
					Option option = (Option) object;
					System.out.println("\t-" + option.getOpt() + " (--" + option.getLongOpt() + ")" + " \t" + option.getDescription());
				}
			}
			System.exit(0);
		}
		
		//get config
		if(cmd.hasOption("c")) {
			String configPath = cmd.getOptionValue("c");
			logger.debug("Found config path parameter " + configPath);
			if(configPath != null && !configPath.isEmpty() && !configPath.contentEquals("null")) {				
				configFile = new File(configPath);
			}
		}
		//check if we have a valid config path
		if(configFile == null || !configFile.isFile()) {
			configFile = new File(defaultConfigFilePath);
			logger.debug("No or no valid config path in parameters. Using default path " + configFile.getAbsolutePath());
			if(!configFile.isFile()) {
				logger.error("Unable to locate config file. Aborting");
				System.exit(-1);
			}
		}
		
		try {
		logger.info(dashes + "Starting Goobi Archiver"+ dashes);
		Archiver archiver = new Archiver(configFile, new File(cmd.getOptionValue("s")), cmd.getOptionValue("p"));
		archiver.start();
		archiver.join();
		} catch(InterruptedException e) {
			logger.debug("Archiving interrupted");
		}
		logger.info(dashes + "Closing Goobi Archiver" + dashes);
	}

}

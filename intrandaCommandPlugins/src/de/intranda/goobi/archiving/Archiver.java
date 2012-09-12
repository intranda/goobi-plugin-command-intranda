package de.intranda.goobi.archiving;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.log4j.Logger;

/**
 * Handles the archiving of one Goobi process
 * 
 * 
 * @author florian
 * 
 */
public class Archiver extends Thread {

	private static final Logger logger = Logger.getLogger(Archiver.class);
	private static String backup_already_exists_error = "Zu diesem Vorgang existiert bereits ein Backup in diesem Pfad. Wenn Sie weitere Backups ausführen möchten, wenden Sie sich bitte an ihren Goobi-Administrator; oder aktivieren Sie die Option zur Verwendung datierter Archivordner.";

	private File configFile;
	private File destFolder;
	private File goobiOrigFolder;
	private File goobiMetadataFolder;
	private String goobiProcessTitle;
	private File goobiMetadataFile;
	private File goobiAnchorFile;
	private File exportedMetsFolder;
	private File tempFolder;
	private File goobiOcrFolder;
	private boolean archiveOcrData;
	private boolean useDateFolders;
	private boolean deleteMasterTiffs;

	private File destFile;

	public Archiver(File configFile, File goobiOrigFolder, String goobiProcessTitle) {
		super();
		this.configFile = configFile;
		this.goobiOrigFolder = goobiOrigFolder;
		this.goobiProcessTitle = goobiProcessTitle;
		this.goobiMetadataFolder = goobiOrigFolder.getParentFile().getParentFile();
	}

	private boolean readConfig(File configFile) {

		try {
			PropertiesConfiguration.setDefaultListDelimiter('&');
			PropertiesConfiguration config = new PropertiesConfiguration(configFile);
			config.setReloadingStrategy(new FileChangedReloadingStrategy());

			destFolder = new File(config.getString("main_archive_directory", "/opt/backup/"));
			archiveOcrData = config.getBoolean("archive_ocr_data", true);
			useDateFolders = config.getBoolean("use_dated_sub_archive_directories", true);
			goobiMetadataFile = new File(goobiMetadataFolder, config.getString("goobi_metadatafilename", "meta.xml"));
			goobiAnchorFile = new File(goobiMetadataFolder, config.getString("goobi_anchorfilename", "meta_anchor.xml"));
			exportedMetsFolder = new File(config.getString("goobi_exported_mets_directory", "/opt/digiverso/viewer/indexed_mets"));
			goobiOcrFolder = new File(goobiMetadataFolder, config.getString("goobi_ocr_directory_name", "ocr"));
			deleteMasterTiffs = config.getBoolean("delete_original_images", true);

		} catch (ConfigurationException e) {
			logger.error("Unable to read config file " + configFile.getAbsolutePath() + ".");
			return false;
		}
		return true;
	}

	private boolean validateFiles() {

		if (destFolder != null) {
			destFolder.mkdirs();
		}
		if (destFolder == null || !destFolder.isDirectory()) {
			logger.error("Unbale to locate archive directory");
			return false;
		}

		if (goobiOrigFolder == null || !goobiOrigFolder.isDirectory()) {
			logger.error("Unbale to locate Goobi orig images directory");
			return false;
		}

		if (goobiMetadataFolder == null || !goobiMetadataFolder.isDirectory()) {
			logger.error("Unbale to locate Goobi metadata directory");
			return false;
		}

		if (goobiOcrFolder == null || !goobiOcrFolder.isDirectory()) {
			if (archiveOcrData) {
				logger.warn("Unbale to locate Goobi ocr directory. No ocr results will be archived");
				goobiOcrFolder = null;
			}
		}

		tempFolder = new File(goobiMetadataFolder, "archiveTemp");
		tempFolder.mkdirs();
		if (tempFolder == null || !tempFolder.isDirectory()) {
			logger.error("Unbale to create temporary directory");
			return false;
		}

		if (goobiMetadataFile == null || !goobiMetadataFile.isFile()) {
			logger.error("Unbale to locate Goobi metadata file");
			return false;
		}

		if (goobiAnchorFile == null || !goobiAnchorFile.isFile()) {
			logger.debug("Unbale to locate Goobi anchor file");
			goobiAnchorFile = null;
		}

		if (goobiProcessTitle == null || goobiProcessTitle.isEmpty()) {
			String timestamp = "" + System.currentTimeMillis();
			logger.warn("No valid name of Goobi process. Using timestamp " + timestamp);
			goobiProcessTitle = timestamp;
		}

		return true;
	}

	@Override
	public void run() {

		// ////////Preparing
		// reading configuration and validating settings
		logger.debug("Reding config file");
		if (!readConfig(configFile)) {
			logger.error("Error getting configuration. Aborting");
			close();
		}
		logger.info("Checking source directories and preparing for archiving.");
		if (!validateFiles()) {
			logger.error("Unable to locate necesary system resources. Aborting");
			close();
		}
		
		// creating backup file and checking if one already exists
		File destFile = null;
		File tarFile = new File(tempFolder, goobiProcessTitle + "_" + goobiMetadataFolder.getName() + ".tar");
		if (useDateFolders) {
			String dateString = ArchivingUtils.getCurrentDateString(false);
			dateString = dateString.replaceAll("\\D", "");
			destFile = new File(destFolder, dateString + File.separator + tarFile.getName());
			destFile.getParentFile().mkdir();
		} else {
			destFile = new File(destFolder, tarFile.getName());
		}
		if (destFile.isFile() && destFile.length() > 0) {
			// backup already exists
			logger.error("File " + destFile.getAbsolutePath() + " already exists. Terminating archiving.");
			System.err.println(backup_already_exists_error);
			close(false);
		}

		// get masterTiffs
		logger.debug("Getting list of mastertiffs");
		File[] masterTiffs = goobiOrigFolder.listFiles(ArchivingUtils.ImageFilter);
		if (masterTiffs == null)
			masterTiffs = new File[0];
		if (masterTiffs.length == 0) {
			logger.error("No master tiffs found in directory " + goobiOrigFolder.getAbsolutePath() + ". Aborting");
			close();
		}
		logger.debug("Found " + masterTiffs.length + " matertiffs");

		// get exported METS file
		// logger.debug("Looking for exported mets files");
		// ArrayList<File> exportedMetsFiles = new ArrayList<File>();
		// if (exportedMetsFolder == null || !exportedMetsFolder.isDirectory()) {
		// logger.warn("No exported mets directory found. Continuing without archiving exported mets file");
		// } else {
		// for (String catalogId : catalogIds) {
		// File exportedMetsFile = new File(exportedMetsFolder, catalogId + ".xml");
		// if(exportedMetsFile != null && exportedMetsFile.isFile()) {
		// exportedMetsFiles.add(exportedMetsFile);
		// }
		// }
		// if (exportedMetsFiles.isEmpty()) {
		// logger.warn("No exported mets file found. Continuing without archiving exported mets file");
		// }
		// }
		// logger.debug("Found " + exportedMetsFiles.size() + " exported mets files");
		// ////////Done preparing

		// ////////Creating a list of files to put into the zip archive, along with a map of corresponding checksums
		logger.debug("Getting files to be archived");
		// ArrayList<File> filesToBeArchived = new ArrayList<File>();
		HashMap<File, String> filenameMap = new HashMap<File, String>();

		// adding orig image files
		for (File file : masterTiffs) {
			filenameMap.put(file, ArchivingUtils.getRelativePath(file, goobiMetadataFolder));
			// checksumMap.put(file.getName(), ArchivingUtils.createChecksum(file));
		}
		// filesToBeArchived.add(goobiOrigFolder);

		// adding ocr files
		if (archiveOcrData && goobiOcrFolder != null) {
			for (File file : ArchivingUtils.getAllFilesRecursive(goobiOcrFolder)) {
				filenameMap.put(file, ArchivingUtils.getRelativePath(file, goobiMetadataFolder));
				// checksumMap.put(file.getName(), ArchivingUtils.createChecksum(file));
			}
			// filesToBeArchived.add(goobiOcrFolder);
		}

		// adding mets files
		filenameMap.put(goobiMetadataFile, ArchivingUtils.getRelativePath(goobiMetadataFile, goobiMetadataFolder));
		// checksumMap.put(goobiMetadataFile.getName(), ArchivingUtils.createChecksum(goobiMetadataFile));
		// filesToBeArchived.add(goobiMetadataFile);
		if (goobiAnchorFile != null) {
			filenameMap.put(goobiAnchorFile, ArchivingUtils.getRelativePath(goobiAnchorFile, goobiMetadataFolder));
			// checksumMap.put(goobiAnchorFile.getName(), ArchivingUtils.createChecksum(goobiAnchorFile));
			// filesToBeArchived.add(goobiAnchorFile);
		}

		// adding exported mets files
		exportedMetsFolder = new File(goobiMetadataFolder, "exported_mets");
		if (!exportedMetsFolder.isDirectory() || exportedMetsFolder.listFiles() == null || exportedMetsFolder.listFiles().length == 0) {
			logger.warn("No exported mets files found. Continuing without");
		} else {
			for (File file : exportedMetsFolder.listFiles()) {
				filenameMap.put(file, ArchivingUtils.getRelativePath(file, goobiMetadataFolder));
			}
		}
		logger.debug("Found " + filenameMap.keySet().size() + " files and folders to be archived.");
		// ////////Done creating list and map

		// ////////Creating archive
		logger.info("Creating tar-archive");
		@SuppressWarnings("unused")
		byte[] origArchiveChecksum = null;
		try {
			origArchiveChecksum = TarUtils.tarFiles(filenameMap, tarFile);
		} catch (IOException e) {
			logger.error("Failed to tar files to archive. Aborting.");
			close();
		}
		// ////////Done creating archive

		// ////////validating archive
		logger.info("Validating tar-archive");
		byte[] origArchiveAfterZipChecksum = null;
		try {
			origArchiveAfterZipChecksum = ArchivingUtils.createChecksum(tarFile);
		} catch (NoSuchAlgorithmException e) {
			logger.error("Failed to validate tar archive: " + e.toString() + ". Aborting.");
			close();
		} catch (IOException e) {
			logger.error("Failed to validate tar archive: " + e.toString() + ". Aborting.");
			close();
		}

		if (TarUtils.validateTar(tarFile, true, goobiMetadataFolder)) {
			logger.info("Tar archive is valid");
		} else {
			logger.error("Tar archive is curropted. Aborting.");
			close();
		}
		// ////////Done validating archive

		// ////////copying archive file and validating copy
		logger.info("Copying tar archive to archive");
		try {
//			if (useDateFolders) {
//				String dateString = ArchivingUtils.getCurrentDateString(false);
//				dateString = dateString.replaceAll("\\D", "");
//				destFile = new File(destFolder, dateString + File.separator + tarFile.getName());
//				destFile.getParentFile().mkdir();
//			} else {
//				destFile = new File(destFolder, tarFile.getName());
//			}
			ArchivingUtils.copyFile(tarFile, destFile);

			// validation
			if (!MessageDigest.isEqual(origArchiveAfterZipChecksum, ArchivingUtils.createChecksum(destFile))) {
				logger.error("Error copying archive file to archive: Copy is not valid. Aborting.");
				close();
			}
		} catch (IOException e) {
			logger.error("Error validating copied archive. Aborting.");
			close();
		} catch (NoSuchAlgorithmException e) {
			logger.error("Error validating copied archive. Aborting.");
			close();
		}
		logger.info("Tar archive copied to " + destFile.getAbsolutePath() + " and found to be valid.");
		// ////////done copying and validating archive

		// ////////create HashFile
		logger.debug("Creating hash file");
		try {
			HashFileCreator swap = new HashFileCreator(goobiMetadataFolder, destFile.getParentFile(), goobiProcessTitle);
			swap.addFile(destFile.getParentFile().getName() + File.separator + destFile.getName(),
					ArchivingUtils.convertChecksumToHex(origArchiveAfterZipChecksum));
			File swapFile = swap.writeHashFile(goobiMetadataFolder);
			if (swapFile == null) {
				logger.error("Unable to write hash file: Document not set up properly");
			} else {
				logger.info("Hash info written to " + swapFile.getAbsolutePath());
			}
		} catch (IOException e) {
			logger.error("Unable to write hash file: " + e.toString());
		}
		// ////////Done creating HashFile

		// ////////deleting original image files
		if (deleteMasterTiffs) {
			logger.info("Deleting orig tiff files");
			for (File file : masterTiffs) {
				if (!file.delete()) {
					logger.error("Failed to delete file " + file.getAbsolutePath() + ". Continuing.");
				}
			}
		}
		// ////////done deleting original image files

		// deleting temp folders
		if (exportedMetsFolder != null && exportedMetsFolder.isDirectory()) {
			ArchivingUtils.deleteAllFiles(exportedMetsFolder);
		}
		if (tempFolder != null && tempFolder.isDirectory()) {
			ArchivingUtils.deleteAllFiles(tempFolder);
		}
	}

	public String getArchive() {
		if (destFile != null && destFile.isFile()) {
			return destFile.getAbsolutePath();
		}
		return null;
	}

	private void close() {
		close(true);
	}
	
	private void close(boolean error) {

		if(error) {			
			System.err.println("Error archiving directory " + goobiMetadataFolder + ". See log for details");
		}

		if (exportedMetsFolder != null && exportedMetsFolder.isDirectory()) {
			ArchivingUtils.deleteAllFiles(exportedMetsFolder);
		}
		if (tempFolder != null && tempFolder.isDirectory()) {
			ArchivingUtils.deleteAllFiles(tempFolder);
		}
		interrupt();
		

	}

}

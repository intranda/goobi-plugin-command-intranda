package de.intranda.goobi.archiving;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.log4j.Logger;

public class TarUtils {
	
	private static final Logger logger = Logger.getLogger(TarUtils.class);

	public static byte[] tarFiles(HashMap<File, String> fileMap, File tarFile) throws IOException {

		MessageDigest checksum = null;

		if (tarFile == null || fileMap == null || fileMap.size() == 0) {
			return null;
		}

		tarFile.getParentFile().mkdirs();

		TarArchiveOutputStream tos = null;
		try {
			FileOutputStream fos = new FileOutputStream(tarFile, true);
			try {
				checksum = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				logger.error("No checksum algorithm \"MD5\". Disabling checksum creation");
				checksum = null;
			}
			tos = new TarArchiveOutputStream(fos);
			for (File file : fileMap.keySet()) {
				logger.debug("Adding file " + file.getAbsolutePath() + " to tarfile " + tarFile.getAbsolutePath());
				tarFile(file, fileMap.get(file), tos, checksum);
			}
		} catch (FileNotFoundException e) {
			logger.debug("Encountered FileNotFound Exception, probably due to trying to archive a directory. Ignoring");
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} finally {
			if (tos != null) {
				tos.close();
			}
		}

		return checksum.digest();
	}

	private static void tarFile(File file, String path, TarArchiveOutputStream tos, MessageDigest checksum) throws IOException {

		if (file == null) {
			logger.error("Attempting to add nonexisting file to zip archive. Ignoring entry.");
			return;
		}

		if (file.isFile()) {

			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			
			TarArchiveEntry entry = new TarArchiveEntry(file, path);
//			ArchiveEntry entry = tos.createArchiveEntry(file, path);
			tos.putArchiveEntry(entry);
			int size;
			byte[] buffer = new byte[2048];
			while ((size = bis.read(buffer, 0, buffer.length)) != -1) {
				tos.write(buffer, 0, size);
				if (checksum != null && size > 0) {
					checksum.update(buffer, 0, size);
				}
			}

			if (tos != null) {
				tos.closeArchiveEntry();
			}
			if (bis != null) {
				bis.close();
			}
		} else if (file.isDirectory()) {
			ArchiveEntry dirEntry = tos.createArchiveEntry(file, path + File.separator);
			tos.putArchiveEntry(dirEntry);
			if(tos != null && dirEntry != null) {
				tos.closeArchiveEntry();
			}
			File[] subfiles = file.listFiles();
			if (subfiles != null && subfiles.length > 0) {
				for (File subFile : subfiles) {
					tarFile(subFile, path + File.separator + subFile.getName(), tos, checksum);
				}
			}
		} else {
			logger.warn("File " + file.getAbsolutePath() + " doesn't seem to exist and cannot be added to zip archive.");
		}
	}
	
	/**
	 * Unzip a tar archive and write results into Array of Strings
	 * 
	 * @param source
	 * @return
	 * @throws IOException
	 */
	public static ArrayList<File> untarFile(File source, File destDir) throws IOException {
		ArrayList<File> fileList = new ArrayList<File>();

		if (!destDir.isDirectory())
			destDir.mkdirs();

		TarArchiveInputStream in = null;
		try {
			in = new TarArchiveInputStream((new BufferedInputStream(new FileInputStream(source))));
			ArchiveEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				File tempFile = new File(destDir, entry.getName());
				if(entry.isDirectory()) {
					tempFile.mkdirs();
					continue;
				}
				fileList.add(tempFile);
				tempFile.getParentFile().mkdirs();
//				tempFile.createNewFile();
				logger.debug("Untaring file " + entry.getName() + " from archive " + source.getName() + " to " + tempFile.getAbsolutePath());
				BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile));

				int size;
				byte[] buffer = new byte[2048];
				while ((size = in.read(buffer, 0, buffer.length)) != -1) {
					out.write(buffer, 0, size);
				}
				if (out != null) {
					out.flush();
					out.close();
				}
			}
		} catch (FileNotFoundException e) {
			logger.debug("Encountered FileNotFound Exception, probably due to trying to extract a directory. Ignoring");
		} finally {
			if (in != null)
				in.close();
		}

		return fileList;
	}
	
	public static boolean validateTar(File tarFile, boolean createTempFile, File origFilesParent) {

		TarArchiveInputStream in = null;
		try {
			in = new TarArchiveInputStream((new BufferedInputStream(new FileInputStream(tarFile))));
			TarArchiveEntry entry;
			File tempFile = null;
			while ((entry = in.getNextTarEntry()) != null) {
				File f = new File(entry.getName());
			    tempFile = new File(tarFile.getParentFile(), f.getName());
				if(entry.isDirectory()) {
					continue;
				}
				logger.debug("Testing file " + entry.getName() + " from archive " + tarFile.getName());
				BufferedOutputStream out = null;
				if(createTempFile) {
					if(!tempFile.isFile()) {
						tempFile.createNewFile();
					}
					out = new BufferedOutputStream(new FileOutputStream(tempFile));
				}

				int size;
				byte[] buffer = new byte[2048];
				while ((size = in.read(buffer, 0, buffer.length)) != -1) {
					if(createTempFile) {
						out.write(buffer, 0, size);
					}
				}
				if (out != null) {
					out.flush();
					out.close();
				}
				
				if(createTempFile && (tempFile == null || !tempFile.isFile())) {
					logger.debug("Found corrupted archive entry: Unable to create file");
					return false;
				}
				
				//check checksum of file
				try {
				if(createTempFile && origFilesParent != null) {
					File origFile = new File(origFilesParent, entry.getName());
					if(origFile == null || !origFile.isFile()) {
						logger.debug("Unable to find orig file for entry " + entry.getName());
						continue;
					}
					logger.debug("Testing entry against original file " + origFile.getAbsolutePath());
					byte[] tempFileChecksum = ArchivingUtils.createChecksum(tempFile);
					byte[] origFileChecksum = ArchivingUtils.createChecksum(origFile);
					if(!MessageDigest.isEqual(tempFileChecksum, origFileChecksum)) {
						logger.debug("Found corrupted archive entry: Checksums don't match");
						return false;
					}
					
				}
				} catch (NoSuchAlgorithmException e) {
					logger.error("Unable to check file: Unknown algorithm");
				} finally {
					if(tempFile != null && tempFile.isFile()) {
						tempFile.delete();
					}
				}

			}
//		} catch (FileNotFoundException e) {
//			logger.debug("Encountered FileNotFound Exception, probably due to trying to extract a directory. Ignoring");
		} catch (IOException e) {
			logger.debug("Found corrupted archive entry");
			return false;
		}
		finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		return true;
	}
	
}

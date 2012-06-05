package de.intranda.goobi.archiving;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ArchivingUtils {
	
	private static final Logger logger = Logger.getLogger(ArchivingUtils.class);
	private static final String encoding = "utf-8";

	public static ArrayList<File> getAllFilesRecursive(File dir) {
		ArrayList<File> fileList = new ArrayList<File>();
		File[] subFiles = dir.listFiles();
		if(dir == null || !dir.isDirectory() || subFiles == null || subFiles.length == 0) {
			return fileList;
		}
		
		for (File file : subFiles) {
			if(file.isFile()) {
				fileList.add(file);
			} else if(file.isDirectory()) {
				fileList.addAll(getAllFilesRecursive(file));
			}
		}
		
		return fileList;
	}
	
	public static String getRelativePath(File file, File relativeParent) {
		String path = "";
		
		while(!relativeParent.getAbsolutePath().contentEquals(file.getAbsolutePath())) {
			String filename = file.getName();
			if(!file.isFile()) {
				filename = filename.concat("/");
			}
			path = filename.concat(path);
			file = file.getParentFile();
			if(file == null) {
				break;
			}
		}
		return path;
	}
	
	public static FilenameFilter ImageFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			// jpeg
			if (name.endsWith("jpg") || name.endsWith("JPG") || name.endsWith("jpeg") || name.endsWith("JPEG")) {
				validImage = true;
			}
			if (name.endsWith(".tif") || name.endsWith(".TIF")) {
				validImage = true;
			}
			// png
			if (name.endsWith(".png") || name.endsWith(".PNG")) {
				validImage = true;
			}
			// gif
			if (name.endsWith(".gif") || name.endsWith(".GIF")) {
				validImage = true;
			}
			// jpeg2000
			if (name.endsWith(".jp2") || name.endsWith(".JP2")) {
				validImage = true;
			}

			return validImage;
		}
	};

	public static byte[] createChecksum(File file) throws NoSuchAlgorithmException, IOException {
		InputStream fis = new FileInputStream(file);

		byte[] buffer = new byte[1024];
		MessageDigest complete = MessageDigest.getInstance("MD5");
		int numRead;
		do {
			numRead = fis.read(buffer);
			if (numRead > 0) {
				complete.update(buffer, 0, numRead);
			}
		} while (numRead != -1);
		fis.close();
		return complete.digest();
	}
	
	   public static String getMD5Checksum(File file) throws NoSuchAlgorithmException, IOException  {
	       byte[] b = createChecksum(file);
	       String result = "";

	       for (int i=0; i < b.length; i++) {
	           result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
	       }
	       return result;
	   }
	   
	   public static String convertChecksumToHex(byte[] checksum) {
	       String result = "";

	       for (int i=0; i < checksum.length; i++) {
	           result += Integer.toString( ( checksum[i] & 0xff ) + 0x100, 16).substring( 1 );
	       }
	       return result;
	   }
	   
		/**
		 * Deletes a directory with all included files and subdirectories. If the argument is a file, it will simply delete this
		 * 
		 * @param dir
		 */
		public static boolean deleteAllFiles(File dir) {
			if (dir == null) {
				return false;
			}
			if (dir.isFile()) {
				logger.error("Unable to delete directory " + dir.getAbsolutePath());
				return dir.delete();
			}
			boolean success = true;
			if (dir.isDirectory()) {
				File[] fileList = dir.listFiles();
				if (fileList != null) {
					for (File file : fileList) {
						if (file.isDirectory()) {
							if (!deleteAllFiles(file)) {
								logger.error("Unable to delete directory " + file.getAbsolutePath());
								success = false;
							}

						} else {
							if (!file.delete()) {
								logger.error("Unable to delete directory " + file.getAbsolutePath());
								success = false;
							}
						}
					}
				}
				if (!dir.delete()) {
					logger.error("Unable to delete directory " + dir.getAbsolutePath());
					success = false;
				}
			}
			return success;
		}

		/**
		 * Copies the content of file source to file dest
		 * 
		 * @param source
		 * @param dest
		 * @throws IOException
		 */
		public static void copyFile(File source, File dest) throws IOException {

			if (!dest.exists()) {
				dest.createNewFile();
			}
			InputStream in = null;
			OutputStream out = null;
			try {
				in = new FileInputStream(source);
				out = new FileOutputStream(dest);

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} finally {
				in.close();
				out.close();
			}
		}
		
		/**
		 * Returns a String containing a human readable representation of the current date, and the current time if that parameter is true
		 * 
		 * @param timeOfDay
		 * @return
		 */
		public static String getCurrentDateString(boolean timeOfDay) {
			Calendar cal = GregorianCalendar.getInstance();
			Date date = cal.getTime();

			SimpleDateFormat simpDate;
			if (timeOfDay) {
				simpDate = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
			} else {
				simpDate = new SimpleDateFormat("dd.MM.yyyy");
			}
			return simpDate.format(date);
		}
		
		/**
		 * Simply write a String into a text file.
		 * 
		 * @param string
		 *            The String to write
		 * @param file
		 *            The file to write to (will be created if it doesn't exist
		 * @param encoding
		 *            The character encoding to use. If null, a standard utf-8 encoding will be used
		 * @param append
		 *            Whether to append the text to an existing file (true), or to overwrite it (false)
		 * @return
		 * @throws IOException
		 */
		public static File writeTextFile(String string, File file, String encoding, boolean append) throws IOException {

			if (encoding == null)
				encoding = ArchivingUtils.encoding;

			FileWriterWithEncoding writer = null;
			writer = new FileWriterWithEncoding(file, encoding, append);
			writer.write(string);
			if (writer != null)
				writer.close();

			return file;
		}

		/**
		 * Writes the Document doc into an xml File file
		 * 
		 * @param file
		 * @param doc
		 * @throws IOException
		 */
		public static void getFileFromDocument(File file, Document doc) throws IOException {
			writeTextFile(getStringFromDocument(doc, encoding), file, encoding, false);
		}

		/**
		 * 
		 * Creates a single String out of the Document document
		 * 
		 * @param document
		 * @param encoding
		 *            The character encoding to use. If null, a standard utf-8 encoding will be used
		 * @return
		 */
		public static String getStringFromDocument(Document document, String encoding) {
			if (document == null) {
				logger.warn("Trying to convert null document to String. Aborting");
				return null;
			}
			if (encoding == null)
				encoding = encoding;

			XMLOutputter outputter = new XMLOutputter();
			Format xmlFormat = outputter.getFormat();
			if (!(encoding == null) && !encoding.isEmpty())
				xmlFormat.setEncoding(encoding);
			xmlFormat.setExpandEmptyElements(true);
			outputter.setFormat(xmlFormat);
			String docString = outputter.outputString(document);

			return docString;
		}
		
		/**
		 * Load a jDOM document from an xml file
		 * 
		 * @param file
		 * @return
		 */
		public static Document getDocumentFromFile(File file) {
			SAXBuilder builder = new SAXBuilder(false);
			Document document = null;

			try {
				document = builder.build(file);
			} catch (JDOMException e) {
				logger.error(e.toString(), e);
				return null;
			} catch (IOException e) {
				logger.error(e.toString(), e);
				return null;
			}
			return document;
		}
	
}

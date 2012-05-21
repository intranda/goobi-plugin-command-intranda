package de.intranda.goobi.archiving;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jdom.Document;
import org.jdom.Element;

public class HashFileCreator {

	private Document doc = null;
	private File goobiMetadataDir;
	private File swappedDir;
	private String processTitle;
	private HashMap<String, String> fileMap = new HashMap<String, String>();
	
	public HashFileCreator(File goobiMetadataDir, File swappedDir, String processTitle) {
		this.goobiMetadataDir = goobiMetadataDir;
		this.swappedDir = swappedDir;
		this.processTitle = processTitle;
	}
	
	public void addFile(String filepath, String checksum) {
		fileMap.put(filepath, checksum);
	}
	
	public File writeHashFile(File destDir) throws IOException {
		if(doc == null) {
			createHashFile();
		}
		
		if(doc != null) {
			File destFile = new File(destDir, "swapped.xml");
			ArchivingUtils.getFileFromDocument(destFile, doc);
			return destFile;
		}
		return null;
	}
	
	public void createHashFile() {
		
		Element root = new Element("goobiArchive");
		doc = new Document(root);
		
		Element source = new Element("source");
		source.setText(goobiMetadataDir.getAbsolutePath());
		root.addContent(source);
		
		Element target = new Element("target");
		target.setText(swappedDir.getAbsolutePath());
		root.addContent(target);
		
		Element title = new Element("title");
		title.setText(processTitle);
		root.addContent(title);
		
		Element date = new Element("date");
		date.setText(ArchivingUtils.getCurrentDateString(true));
		root.addContent(date);
		
		if(fileMap != null && !fileMap.isEmpty()) {
			for (String filename : fileMap.keySet()) {
				Element file = new Element("file");
				file.setAttribute("path", filename);
				file.setAttribute("crc32", fileMap.get(filename));
				root.addContent(file);
			}
		}
	}
	
}

package de.intranda.goobi.archiving;

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
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

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
        if (doc == null) {
            createHashFile(destDir);
        }

        if (doc != null) {
            File destFile = new File(destDir, "swapped.xml");
            ArchivingUtils.getFileFromDocument(destFile, doc);
            return destFile;
        }
        return null;
    }

    public void createHashFile(File fileDir) {

        //first get all existing entries in the swap file//only makes sense when using date folders
        File swappedFile = new File(fileDir, "swapped.xml");
        if (fileDir != null && fileDir.isDirectory() && swappedFile.isFile()) {
            doc = ArchivingUtils.getDocumentFromFile(swappedFile);
            @SuppressWarnings("rawtypes")
            List filePaths = doc.getRootElement().getChildren("file");
            if (filePaths != null) {
                for (Object filePath : filePaths) {
                    if (filePath instanceof Element) {
                        Element element = (Element) filePath;
                        fileMap.put(element.getAttributeValue("path"), element.getAttributeValue("crc32"));
                    }
                }
            }
        }

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

        if (fileMap != null && !fileMap.isEmpty()) {
            for (String filename : fileMap.keySet()) {
                Element file = new Element("file");
                file.setAttribute("path", filename);
                file.setAttribute("crc32", fileMap.get(filename));
                root.addContent(file);
            }
        }
    }

}

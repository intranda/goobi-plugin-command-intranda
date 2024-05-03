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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.log4j.Logger;

public class TarUtils {

    private static final Logger logger = Logger.getLogger(TarUtils.class);

    /**
     * Create a tar archive and write results into Array of Strings. Returns the MD5 checksum as byte-Array
     * 
     * @param source
     * @return
     * @throws IOException
     */
    public static byte[] tarFiles(HashMap<File, String> fileMap, File tarFile) throws IOException {

        MessageDigest checksum = null;
        boolean gzip = false;
        if (tarFile.getName().endsWith(".gz")) {
            gzip = true;
        }

        if (tarFile == null || fileMap == null || fileMap.size() == 0) {
            return null;
        }

        tarFile.getParentFile().mkdirs();

        TarArchiveOutputStream tos = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        GZIPOutputStream zip = null;
        try {
            fos = new FileOutputStream(tarFile, true);
            bos = new BufferedOutputStream(fos);
            if (gzip) {
                zip = new GZIPOutputStream(bos);
            }
            try {
                checksum = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                logger.error("No checksum algorithm \"MD5\". Disabling checksum creation");
                checksum = null;
            }
            if (gzip) {
                tos = new TarArchiveOutputStream(zip);
            } else {
                tos = new TarArchiveOutputStream(bos);
            }
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
            if (fos != null) {
                tos.close();
            }
            if (bos != null) {
                tos.close();
            }
            if (zip != null) {
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
            if (tos != null && dirEntry != null) {
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

        if (!destDir.isDirectory()) {
            destDir.mkdirs();
        }

        boolean isGzip = false;
        if (source.getName().endsWith(".gz")) {
            isGzip = true;
        }

        GZIPInputStream zip = null;
        TarArchiveInputStream in = null;
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(source);
            bis = new BufferedInputStream(fis);
            if (isGzip) {
                zip = new GZIPInputStream(bis);
                in = new TarArchiveInputStream(zip);
            } else {
                in = new TarArchiveInputStream(bis);
            }
            ArchiveEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                File tempFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
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
            if (in != null) {
                in.close();
            }
        }

        return fileList;
    }

    public static boolean validateTar(File tarFile, boolean createTempFile, File origFilesParent) {

        boolean isGzip = false;
        if (tarFile.getName().endsWith(".gz")) {
            isGzip = true;
        }

        GZIPInputStream zip = null;
        TarArchiveInputStream in = null;
        BufferedInputStream bis = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(tarFile);
            bis = new BufferedInputStream(fis);
            if (isGzip) {
                zip = new GZIPInputStream(bis);
                in = new TarArchiveInputStream(zip);
            } else {
                in = new TarArchiveInputStream(bis);
            }

            //			in = new TarArchiveInputStream((new BufferedInputStream(new FileInputStream(tarFile))));
            TarArchiveEntry entry;
            File tempFile = null;
            while ((entry = in.getNextTarEntry()) != null) {
                File f = new File(entry.getName());
                tempFile = new File(tarFile.getParentFile(), f.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                logger.debug("Testing file " + entry.getName() + " from archive " + tarFile.getName());
                BufferedOutputStream out = null;
                if (createTempFile) {
                    if (!tempFile.isFile()) {
                        tempFile.createNewFile();
                    }
                    out = new BufferedOutputStream(new FileOutputStream(tempFile));
                }

                int size;
                byte[] buffer = new byte[2048];
                while ((size = in.read(buffer, 0, buffer.length)) != -1) {
                    if (createTempFile) {
                        out.write(buffer, 0, size);
                    }
                }
                if (out != null) {
                    out.flush();
                    out.close();
                }

                if (createTempFile && (tempFile == null || !tempFile.isFile())) {
                    logger.debug("Found corrupted archive entry: Unable to create file");
                    return false;
                }

                //check checksum of file
                try {
                    if (createTempFile && origFilesParent != null) {
                        File origFile = new File(origFilesParent, entry.getName());
                        if (origFile == null || !origFile.isFile()) {
                            logger.debug("Unable to find orig file for entry " + entry.getName());
                            continue;
                        }
                        logger.debug("Testing entry against original file " + origFile.getAbsolutePath());
                        byte[] tempFileChecksum = ArchivingUtils.createChecksum(tempFile);
                        byte[] origFileChecksum = ArchivingUtils.createChecksum(origFile);
                        if (!MessageDigest.isEqual(tempFileChecksum, origFileChecksum)) {
                            logger.debug("Found corrupted archive entry: Checksums don't match");
                            return false;
                        }

                    }
                } catch (NoSuchAlgorithmException e) {
                    logger.error("Unable to check file: Unknown algorithm");
                } finally {
                    if (tempFile != null && tempFile.isFile()) {
                        tempFile.delete();
                    }
                }

            }
            //		} catch (FileNotFoundException e) {
            //			logger.debug("Encountered FileNotFound Exception, probably due to trying to extract a directory. Ignoring");
        } catch (IOException e) {
            logger.debug("Found corrupted archive entry");
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (zip != null) {
                    zip.close();
                }
                if (bis != null) {
                    bis.close();
                }
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return true;
    }

}

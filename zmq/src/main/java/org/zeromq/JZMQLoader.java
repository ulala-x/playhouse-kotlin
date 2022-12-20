package org.zeromq;



import java.io.*;
import java.util.UUID;


public class JZMQLoader
{


    private static File nativeLibFile = null;

    static void cleanUpExtractedNativeLib()
    {
        if (nativeLibFile != null && nativeLibFile.exists()) {
            boolean deleted = nativeLibFile.delete();
            if (!deleted) {
                // Deleting native lib has failed, but it's not serious so simply ignore it here
            }
        }
    }


    private static boolean contentsEquals(InputStream in1, InputStream in2)
            throws IOException
    {
        if (!(in1 instanceof BufferedInputStream)) {
            in1 = new BufferedInputStream(in1);
        }
        if (!(in2 instanceof BufferedInputStream)) {
            in2 = new BufferedInputStream(in2);
        }

        int ch = in1.read();
        while (ch != -1) {
            int ch2 = in2.read();
            if (ch != ch2) {
                return false;
            }
            ch = in1.read();
        }
        int ch2 = in2.read();
        return ch2 == -1;
    }

    /**
     * Extract the specified library file to the target folder
     *
     * @param nativeLibraryFilePath
     * @param libraryFileName
     * @param targetFolder
     * @return
     */
    public static File extractLibraryFile(String nativeLibraryFilePath, String libraryFileName, String targetFolder)
    {
        //String nativeLibraryFilePath = libFolderForCurrentOS + "/" + libraryFileName;

        // Attach UUID to the native library file to ensure multiple class loaders can read the libsnappy-java multiple times.
        String uuid = UUID.randomUUID().toString();


        String extractedLibFileName = "";
        extractedLibFileName = libraryFileName;

//        if(libraryFileName.contains("libzmq")){
//            extractedLibFileName = libraryFileName;
//        }else{
//            extractedLibFileName = String.format("%s-%s",uuid,libraryFileName);
//        }

//        extractedLibFileName = String.format("%s-%s",uuid,libraryFileName);


    //        String
        File extractedLibFile = new File(targetFolder, extractedLibFileName);

        try {
            // Extract a native library file into the target directory
            InputStream reader = null;
            FileOutputStream writer = null;
            try {
                reader = JZMQLoader.class.getResourceAsStream(nativeLibraryFilePath);
                try {
                    writer = new FileOutputStream(extractedLibFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead = 0;
                    while ((bytesRead = reader.read(buffer)) != -1) {
                        writer.write(buffer, 0, bytesRead);
                    }
                }
                finally {
                    if (writer != null) {
                        writer.close();
                    }
                }
            }
            finally {
                if (reader != null) {
                    reader.close();
                }

                // Delete the extracted lib file on JVM exit.
                extractedLibFile.deleteOnExit();
            }

            // Set executable (x) flag to enable Java to load the native library
            boolean success = extractedLibFile.setReadable(true) &&
                    extractedLibFile.setWritable(true, true) &&
                    extractedLibFile.setExecutable(true);
            if (!success) {
                // Setting file flag may fail, but in this case another error will be thrown in later phase
            }

            // Check whether the contents are properly copied from the resource folder
            {
                InputStream nativeIn = null;
                InputStream extractedLibIn = null;
                try {
                    nativeIn = JZMQLoader.class.getResourceAsStream(nativeLibraryFilePath);
                    extractedLibIn = new FileInputStream(extractedLibFile);

                    if (!contentsEquals(nativeIn, extractedLibIn)) {
                        throw new RuntimeException(String.format("Failed to write a native library file at %s", extractedLibFile));
                    }
                }
                finally {
                    if (nativeIn != null) {
                        nativeIn.close();
                    }
                    if (extractedLibIn != null) {
                        extractedLibIn.close();
                    }
                }
            }

            return new File(targetFolder, extractedLibFileName);
        }
        catch (IOException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    private static boolean hasResource(String path)
    {
        return JZMQLoader.class.getResource(path) != null;
    }

}


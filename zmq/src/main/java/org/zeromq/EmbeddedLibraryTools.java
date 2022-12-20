package org.zeromq;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class EmbeddedLibraryTools {

    public static final boolean LOADED_EMBEDDED_LIBRARY;

    private  static File targetFolder;
    private static  List<File> fileList = new ArrayList<File>();

    static {
        LOADED_EMBEDDED_LIBRARY = loadEmbeddedLibrary();
    }



    //public static void cleanUp(){

//        fileList.stream().forEach(new Consumer<File>() {
//            @Override
//            public void accept(File file) {
//                //file.deleteOnExit();
//                try {
//                    FileUtils.
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//            }
//        });

//        if(targetFolder!=null && targetFolder.exists()){
//            //targetFolder.delete();
//            try {
//                FileDeleteStrategy.FORCE.delete(targetFolder);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//        //return  true;
//    }

    //private  static File targetFolder = null;

    private EmbeddedLibraryTools() {


    }

    public static String getCurrentPlatformIdentifier() {
        String osName = System.getProperty("os.name");
        if (osName.toLowerCase().contains("windows")) {
            osName = "Windows";
        } else if (osName.toLowerCase().contains("mac os x")) {
            osName = "Mac";
        } else {
            osName = osName.replaceAll("\\s+", "_");
        }
        return System.getProperty("os.arch") + "/" + osName;
    }

    public static Collection<String> getEmbeddedLibraryList() {

        final Collection<String> result = new ArrayList<String>();
        final Collection<String> files = catalogClasspath();

        for (final String file : files) {
            if (file.startsWith("NATIVE")) {
                result.add(file);
            }
        }

        return result;

    }

    private static void catalogArchive(final File jarfile, final Collection<String> files) {
        JarFile j = null;
        try {
            j = new JarFile(jarfile);
            final Enumeration<JarEntry> e = j.entries();
            while (e.hasMoreElements()) {
                final JarEntry entry = e.nextElement();
                if (!entry.isDirectory()) {
                    files.add(entry.getName());
                }
            }

        } catch (IOException x) {
            System.err.println(x.toString());
        } finally {
            try {
                j.close();
            } catch (Exception e) {
            }
        }

    }

    private static Collection<String> catalogClasspath() {

        final List<String> files = new ArrayList<String>();
        final String[] classpath = System.getProperty("java.class.path", "").split(File.pathSeparator);

        for (final String path : classpath) {
            final File tmp = new File(path);
            if (tmp.isFile() && path.toLowerCase().endsWith(".jar")) {
                catalogArchive(tmp, files);
            } else if (tmp.isDirectory()) {
                final int len = tmp.getPath().length() + 1;
                catalogFiles(len, tmp, files);
            }
        }

        return files;

    }

    private static void catalogFiles(final int prefixlen, final File root, final Collection<String> files) {
        final File[] ff = root.listFiles();
        if (ff == null) {
            throw new IllegalStateException("invalid path listed: " + root);
        }

        for (final File f : ff) {
            if (f.isDirectory()) {
                catalogFiles(prefixlen, f, files);
            } else {
                files.add(f.getPath().substring(prefixlen));
            }
        }
    }

    private boolean makeZmq(){
        return true;
    }


    private static boolean loadEmbeddedLibrary() {

        boolean usingEmbedded = false;

        // attempt to locate embedded native library within JAR at following location:
        // /NATIVE/${os.arch}/${os.name}/libjzmq.[so|dylib|dll]
        String[] allowedExtensions = new String[]{"so", "dylib", "dll"};
        String[] libs;
        final String libsFromProps = System.getProperty("jzmq.libs");
        if (libsFromProps == null)
            libs = new String[]{"libzmq", "zmq","libjzmq", "jzmq","libsodium", "sodium" };
        else
            libs = libsFromProps.split(",");
        StringBuilder url = new StringBuilder();
        url.append("/NATIVE/");

//        String platformIdentifier = getCurrentPlatformIdentifier();

        url.append(OSInfo.getNativeLibFolderPathForCurrentOS()).append("/");


//        if(platformIdentifier.contains("64")){
//            url.append("64").append("/");
//        }else{
//            url.append("32").append("/");
//        }

        //url.append(getCurrentPlatformIdentifier()).append("/");


        File tempFolder = new File(System.getProperty("java.io.tmpdir"));

        String uuid = UUID.randomUUID().toString();

        if (!tempFolder.exists()) {

            if (!tempFolder.mkdirs()) {
                throw  new RuntimeException("fail create folder");
            }
        }

        String targetFolderPath = String.format("%s%s%s%s",tempFolder.getAbsolutePath(), File.separator,"lbase.",uuid);

        targetFolder = new File(targetFolderPath);

        if (!targetFolder.exists()) {
            if(!targetFolder.mkdirs()){
                throw  new RuntimeException("fail create target folder");
            }
        }


        for (String lib : libs) {
            URL nativeLibraryUrl = null;
            String nativeLibraryFilePath = "";
            // loop through extensions, stopping after finding first one

            String libraryName = "";
            for (String ext : allowedExtensions) {
                nativeLibraryFilePath = url.toString() + lib + "." + ext;
                libraryName = lib+"."+ext;
                nativeLibraryUrl = ZMQ.class.getResource(nativeLibraryFilePath);
                if (nativeLibraryUrl != null)
                    break;
            }

            if(nativeLibraryUrl != null){
                // Temporary folder for the native lib. Use the value of org.xerial.snappy.tempdir or java.io.tmpdir



                File file = JZMQLoader.extractLibraryFile(nativeLibraryFilePath,libraryName,targetFolder.getAbsolutePath());

                fileList.add(file);


                try{
                    if (file != null) {
                        System.load(file.getAbsolutePath());


                        usingEmbedded = true;
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }


//
//            if (nativeLibraryUrl != null) {
//                // native library found within JAR, extract and load
//                try {
//
//                    final File libfile = File.createTempFile(lib, ".lib");
//                    libfile.deleteOnExit(); // just in case
//
//                    final InputStream in = nativeLibraryUrl.openStream();
//                    final OutputStream out = new BufferedOutputStream(new FileOutputStream(libfile));
//
//                    int len = 0;
//                    byte[] buffer = new byte[8192];
//                    while ((len = in.read(buffer)) > -1)
//                        out.write(buffer, 0, len);
//                    out.close();
//                    in.close();
//                    System.load(libfile.getAbsolutePath());
//
//                    usingEmbedded = true;
//
//                } catch (IOException x) {
//                    // mission failed, do nothing
//                }
//
//            } // nativeLibraryUrl exists
        }
        return usingEmbedded;
    }
}

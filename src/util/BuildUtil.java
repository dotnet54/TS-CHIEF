package util;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

//9-5-2020
//https://stackoverflow.com/questions/3336392/java-print-time-of-last-compilation

public class BuildUtil {

    private static Date buildDate;

    static
    {
        try
        {
            buildDate = setBuildDate();
        } catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

//    public static String getBuildDate()
//    {
//        int style = DateFormat.FULL;
//        Locale locale = Locale.getDefault();
//        DateFormat dateFormat = DateFormat.getDateInstance(style, locale);
//        DateFormat timeFormat = DateFormat.getTimeInstance(style, locale);
//
//        return dateFormat.format(buildDate) + " " + timeFormat.format(buildDate);
//    }

    public static String getBuildDate()
    {
        if (buildDate != null){
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss-SSS");
            return simpleDateFormat.format(buildDate);
        }else {
            return "NA";
        }
    }

    private static Date setBuildDate()
    {
        try {
            if (runningFromIntelliJ())
            {
                return getClassBuildTime();
            } else
            {
                return getNewestFileDate();
            }
        }catch(Exception e){
            return null;
        }
    }

    private static Date getNewestFileDate() throws Exception
    {
        String filePath = getJARFilePath();
        File file = new File(filePath);
        ZipFile zipFile = new ZipFile(file);
        Enumeration entries = zipFile.entries();

        long millis = -1;

        while (entries.hasMoreElements())
        {
            ZipEntry entry = (ZipEntry) entries.nextElement();

            if (!entry.isDirectory())
            {
                FileTime fileTime = entry.getLastModifiedTime();
                long currentMillis = fileTime.toMillis();

                if (millis < currentMillis)
                {
                    millis = currentMillis;
                }
            }
        }

        return new Date(millis);
    }


    /**
     * Handles files, jar entries, and deployed jar entries in a zip file (EAR).
     *
     * @return The date if it can be determined, or null if not.
     */
    private static Date getClassBuildTime() throws IOException, URISyntaxException
    {
        Date date = null;
        Class<?> currentClass = new Object()
        {
        }.getClass().getEnclosingClass();
        URL resource = currentClass.getResource(currentClass.getSimpleName() + ".class");
        if (resource != null)
        {
            switch (resource.getProtocol())
            {
                case "file":
                    date = new Date(new File(resource.toURI()).lastModified());
                    break;
                case "jar":
                {
                    String path = resource.getPath();
                    date = new Date(new File(path.substring(5, path.indexOf("!"))).lastModified());
                    break;
                }
                case "zip":
                {
                    String path = resource.getPath();
                    File jarFileOnDisk = new File(path.substring(0, path.indexOf("!")));
                    try (JarFile jarFile = new JarFile(jarFileOnDisk))
                    {
                        ZipEntry zipEntry = jarFile.getEntry(path.substring(path.indexOf("!") + 2));//Skip the ! and the /
                        long zeTimeLong = zipEntry.getTime();
                        date = new Date(zeTimeLong);
                    }
                    break;
                }
            }
        }
        return date;
    }



    public static String getJARFilePath() throws URISyntaxException
    {
        return new File(MethodHandles.lookup().lookupClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
    }

    public static boolean runningFromJAR()
    {
        try
        {
            String jarFilePath = new File(MethodHandles.lookup().lookupClass().getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath()).
                    toString();
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");

            try (ZipFile zipFile = new ZipFile(jarFilePath))
            {
                ZipEntry zipEntry = zipFile.getEntry("META-INF/MANIFEST.MF");

                return zipEntry != null;
            }
        } catch (Exception exception)
        {
            return false;
        }
    }

    public static String getProgramDirectory()
    {
        if (runningFromJAR())
        {
            return getCurrentJARDirectory();
        } else
        {
            return getCurrentProjectDirectory();
        }
    }

    private static String getCurrentProjectDirectory()
    {
        return new File("").getAbsolutePath();
    }

    private static String getCurrentJARDirectory()
    {
        try
        {
            return new File(MethodHandles.lookup().lookupClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        } catch (URISyntaxException exception)
        {
            exception.printStackTrace();
        }

        return null;
    }

    public static boolean runningFromIntelliJ()
    {
        String classPath = System.getProperty("java.class.path");
        return classPath.contains("idea_rt.jar");
    }

//    //5-9-2020
////	https://stackoverflow.com/questions/3336392/java-print-time-of-last-compilation
//    private static Date getDateOfJar(String path) throws IOException
//    {
//        Date ret=null;
//        JarFile jarFile = new JarFile(path);
//        Enumeration ent = jarFile.entries();
//        while (ent.hasMoreElements())
//        {
//            JarEntry entry = (JarEntry) ent.nextElement();
//
//            String name = entry.getName();
//            if (name.equals("META-INF/MANIFEST.MF"))
//            {
//                ret = new Date(entry.getTime());
//                break;
//            }
//        }
//        jarFile.close();
//
//        return ret;
//    }
//
//    //5-9-2020
////	https://stackoverflow.com/questions/3336392/java-print-time-of-last-compilation
//    public static String getClassBuildTime(Object obj)
//    {
//        String ret = "unknown";
//        try
//        {
//            Class<?> currentClass = obj.getClass().getEnclosingClass();
//            URL resource = currentClass.getResource(currentClass.getSimpleName() + ".class");
//            if (resource != null)
//            {
//                if (resource.getProtocol().equals("file"))
//                {
//                    try
//                    {
//                        Date d = new Date(new File(resource.toURI()).lastModified());
//                        ret = ""+d;
//                    }
//                    catch (URISyntaxException ignored)
//                    {
//                    }
//                }
//                else if (resource.getProtocol().equals("jar"))
//                {
//                    String path = resource.getPath();
//                    Date d=getDateOfJar(path.substring(5, path.indexOf("!")));
//                    ret = ""+d;
//                }
//            }
//        }
//        catch (Exception e)
//        {
//            System.out.println("Error! FileLogger.getClassBuildTime() Exception e=" + e.getMessage());
//            e.printStackTrace();
//        }
//
//        return ret;
//    }

}

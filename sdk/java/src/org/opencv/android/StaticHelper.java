package org.opencv.android;

import org.opencv.core.Core;

import java.util.StringTokenizer;
import android.util.Log;

class StaticHelper {

    public static boolean initOpenCV(boolean InitCuda)
    {
        boolean result;
        String libs = "";

        if(InitCuda)
        {
            loadLibrary("cudart");
            loadLibrary("nppc");
            loadLibrary("nppi");
            loadLibrary("npps");
            loadLibrary("cufft");
            loadLibrary("cublas");
        }

        Log.i(TAG, "Trying to get library list");

        try
        {
            System.loadLibrary("opencv_info");
            libs = getLibraryList();
        }
        catch(UnsatisfiedLinkError e)
        {
            Log.e(TAG, "OpenCV error: Cannot load info library for OpenCV");
        }

        Log.i(TAG, "Library list: \"" + libs + "\"");
        Log.i(TAG, "First attempt to load libs");
        if (initOpenCVLibs(libs))
        {
            Log.i(TAG, "First attempt to load libs is OK");
            String eol = System.getProperty("line.separator");
            for (String str : Core.getBuildInformation().split(eol))
                Log.i(TAG, str);

            result = true;
        }
        else
        {
            Log.i(TAG, "First attempt to load libs fails");
            result = false;
        }

        return result;
    }

    private static boolean loadLibrary(String Name)
    {
        boolean result = true;

        Log.i(TAG, "Trying to load library " + Name);
        try
        {
            System.loadLibrary(Name);
            Log.i(TAG, "Library " + Name + " loaded");
        }
        catch(UnsatisfiedLinkError e)
        {
            Log.i(TAG, "Cannot load library \"" + Name + "\"");
            e.printStackTrace();
            result = false;
        }

        return result;
    }

    private static boolean initOpenCVLibs(String Libs)
    {
        Log.i(TAG, "Trying to init OpenCV libs");

        boolean result = true;

        if ((null != Libs) && (Libs.length() != 0))
        {
            Log.i(TAG, "Trying to load libs by dependency list");
            StringTokenizer splitter = new StringTokenizer(Libs, ";");
            while(splitter.hasMoreTokens())
            {
                result &= loadLibrary(splitter.nextToken());
            }
        }
        else
        {
            // If dependencies list is not defined or empty.
            result = loadLibrary("opencv_java4");
        }

        return result;
    }

    private static final String TAG = "OpenCV/StaticHelper";

    private static native String getLibraryList();
}

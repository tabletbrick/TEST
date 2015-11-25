/*
 * Copyright (C) 2014 iWedia S.A. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package com.iwedia.activities;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.iwedia.dtv.DVBManager;
import com.iwedia.dtv.DVBManager.DVBStatus;
import com.iwedia.dtv.IPService;
import com.iwedia.dtv.types.InternalException;
import com.iwedia.epg.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Parent class off all activities. This class contains connection to dtv
 * service through dtv manager object.
 */
public abstract class DTVActivity extends FragmentActivity {
    public static final String TAG = "DTVExample-Channel Zapp";
    public static final String FINISH_ACTIVITIES_MESSAGE = "activity_finish";
    public static final String EXTERNAL_MEDIA_PATH = "/mnt/media/";
    public static final String IP_CHANNELS = "ip_service_list.txt";
    protected static DTVActivity sInstance = null;
    /** List of IP channels. */
    public static ArrayList<IPService> sIpChannels = null;
    /** DVB manager instance. */
    protected DVBManager mDVBManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        /** Set Full Screen Application. */
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFormat(PixelFormat.RGBA_8888);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
        getWindow().getDecorView().getBackground().setDither(true);
        /** Creates DTV manager object and connects it to service. */
        try {
            mDVBManager = DVBManager.getInstance();
        } catch (InternalException e) {
            Log.e(TAG, "There was an error in initializing DVB Manager", e);
            finish();
        }
        mDVBManager.setDVBStatus(mDvbStatusCallBack);
        initializeIpChannels();
    }

    public void finishActivity() {
        Toast.makeText(this,
                "Error with connection happened, closing application...",
                Toast.LENGTH_LONG).show();
        super.finish();
    }

    /**
     * Initialize IP.
     */
    private void initializeIpChannels() {
        copyFile(IP_CHANNELS);
    }

    /**
     * Copy configuration file.
     */
    private void copyFile(String filename) {
        ContextWrapper contextWrapper = new ContextWrapper(this);
        String file = contextWrapper.getFilesDir().getPath() + "/" + filename;
        File fl = new File(file);
        if (!fl.exists())
            copyAssetToData(fl);
    }

    /**
     * Copy configuration file from assets to data folder.
     * 
     * @param strFilename
     */
    private void copyAssetToData(File file) {
        /** Open your local db as the input stream */
        try {
            InputStream myInput = getAssets().open(file.getName());
            String outFileName = file.getPath();
            /** Open the empty db as the output stream. */
            OutputStream myOutput = new FileOutputStream(outFileName);
            /** Transfer bytes from the inputfile to the outputfile. */
            byte[] buffer = new byte[1024];
            int length;
            while ((length = myInput.read(buffer)) > 0) {
                myOutput.write(buffer, 0, length);
            }
            /** Close the streams. */
            myOutput.flush();
            myOutput.close();
            myInput.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Read the configuration file with built-in application which will be
     * displayed in Content list.
     */
    public static void readFile(Context ctx, String filePath,
            ArrayList<IPService> arrayList) {
        File file = new File(filePath);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            String[] separated = new String[2];
            while ((line = br.readLine()) != null) {
                separated = line.split("#");
                arrayList.add(new IPService(separated[0], separated[1]));
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        br = null;
    }

    public void loadIPChannelsFromExternalStorage(
            ArrayList<IPService> ipChannels) {
        ArrayList<File> ipServiceListFiles = new ArrayList<File>();
        File[] storages = new File(EXTERNAL_MEDIA_PATH).listFiles();
        if (storages != null) {
            /**
             * Loop through storages.
             */
            for (File storage : storages) {
                File[] foundIpFiles = storage.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.getName().equalsIgnoreCase(IP_CHANNELS)) {
                            return true;
                        }
                        return false;
                    }
                });
                /**
                 * Files with given name are found in this array.
                 */
                if (foundIpFiles != null) {
                    for (File ip : foundIpFiles) {
                        ipServiceListFiles.add(ip);
                    }
                }
            }
            /**
             * Loop through found files and add it to IP service list.
             */
            for (File ipFile : ipServiceListFiles) {
                readFile(this, ipFile.getPath(), ipChannels);
            }
            /**
             * No files found.
             */
            if (ipServiceListFiles.size() == 0) {
                Toast.makeText(this,
                        "No files found with name: " + IP_CHANNELS,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * DVB CallBack.
     */
    private DVBStatus mDvbStatusCallBack = new DVBStatus() {
        @Override
        public void channelIsScrambled() {
            Toast.makeText(getApplicationContext(), R.string.scrambled,
                    Toast.LENGTH_SHORT).show();
        }
    };
}

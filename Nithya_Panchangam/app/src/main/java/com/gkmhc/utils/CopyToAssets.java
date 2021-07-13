package com.gkmhc.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * To copy metadata files to "Assets" to be used under Ephemeris.
 * (Note: This file is reused from SwissEph)
 *
 * @author GKM Heritage Creations, 2021
 *
 * This whole software project is distributed under GNU GPL:
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Use of this software as a whole or in parts to copy, modify, redistribute shall be in
 * accordance with terms & conditions in GNU GPL license.
 */
public class CopyToAssets {
    String patternToMatch;
    Context context;

    public CopyToAssets(String patternToMatch, Context context) {
        this.patternToMatch = patternToMatch;
        this.context = context;
    }

    /**
     * Copy files to Assets path as required by SwissEph.
     */
    public void copy() {
        AssetManager assetManager = context.getAssets();
        String[] files;
        try {
            files = assetManager.list("");

            String outdir = context.getFilesDir() + File.separator + "/ephe";
            boolean retCode = new File(outdir).mkdirs();
            if (retCode) {
                outdir += File.separator;

                for (String filename : files) {
                    if (new File(outdir + filename).exists() ||
                            !filename.matches(patternToMatch)) {
                        continue;
                    }

                    InputStream in;
                    OutputStream out;
                    try {
                        in = assetManager.open(filename);
                        File outFile = new File(outdir, filename);
                        out = new FileOutputStream(outFile);
                        copyFile(in, out);
                        in.close();
                        out.flush();
                        out.close();
                    } catch (Exception e) {
                        Log.e("tag", "Failed to copy asset file: " + filename, e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
    }

    /**
     * Utility function to copy file contents.
     */
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}

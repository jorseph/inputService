package io.xrspace.controllers;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public class AssetsFileToXimmerseConfig {
    private static final String TAG = "AssetsFileToXimmerseConfig";
    private static final String PATH = "/Ximmerse/Runtime";
    private Context mContext;

    public AssetsFileToXimmerseConfig() {}

    public void SetAssetsFileToXimmerseConfig(Context mContext) {
        this.mContext = mContext;
    }

    private void writeToFile(String data) {
        File folder = new File(Environment.getExternalStorageDirectory() + PATH);
        boolean success = true;
        if(!folder.exists()) {
            Log.v(TAG,"create the folder!");
            success = folder.mkdirs();
        }

        if(success) {
            File file = new File(Environment.getExternalStorageDirectory() + PATH + File.separator + "common.ini");
            try {
                FileOutputStream stream = new FileOutputStream(file);
                try {
                    stream.write(data.getBytes());
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                }
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.toString());
                try {
                    file.createNewFile();
                } catch (IOException er) {
                    Log.e(TAG, er.toString());
                }
            }
        } else {
            Log.e(TAG,"Create the ini file error!");
        }
    }

    private String readFromFile() {
        String contents = "";
        AssetManager mgr = mContext.getAssets();
        try {
            InputStream in = mgr.open("common.ini", AssetManager.ACCESS_BUFFER);
            contents = StreamToString(in);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } finally {
            mgr.close();
        }
        return contents;
    }

    private static String StreamToString(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
        }
        return writer.toString();
    }

    public void WriteXimmerseConfig(String address) {
        Log.v(TAG, "WriteXimmerseConfig");
        String oldData = readFromFile();
        String[] data = address.split(":");
        String newData = oldData.replace("X3C01-", "X3C01-" + data[4] + data[5]);
        newData = newData.replace("Address=", "Address=" + address);
        writeToFile(newData);
    }

    public String ReadXimmerseConfig() {
        Log.v(TAG, "ReadXimmerseConfig");
        String oldData = ReadFromIni();
        int index = oldData.indexOf("Address=");
        String address = oldData.substring(index+8, index+25);
        return address;
    }

    private String ReadFromIni() {
        File file = new File(Environment.getExternalStorageDirectory() + "/Ximmerse/Runtime/common.ini");
        //Read text from file
        StringBuilder text = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            //You'll need to add proper error handling here
        }
        return text.toString();
    }

}

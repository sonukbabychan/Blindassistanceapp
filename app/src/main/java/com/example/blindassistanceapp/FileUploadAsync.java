package com.example.blindassistanceapp;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

public class FileUploadAsync extends AsyncTask<Map, Void, String> {

    private String url;
    private HttpURLConnection con;
    private OutputStream os;

    private String delimiter = "--";
    private String boundary =  "SwA"+Long.toString(System.currentTimeMillis())+"SwA";

    public JsonResponse json_response = null;

    FileUploadAsync(String ur) {
        url = ur;
    }

    @Override
    protected String doInBackground(Map... strings) {
        try {
            con = (HttpURLConnection) ( new URL(url)).openConnection();
            con.setRequestMethod("POST");
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestProperty("Connection", "Keep-Alive");
            con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            con.connect();
            os = con.getOutputStream();

            Iterator it = strings[0].entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                if (pair.getKey().toString().equals("image")) {
                    addFilePart(pair.getKey().toString(), "abc.jpg", (byte[]) pair.getValue());
                }
                else {
                    addFormPart(pair.getKey().toString(), new String((byte[]) pair.getValue()));
                }
            }

            finishMultipart();
            return getResponse();
        } catch (Exception e) {
            Log.v("++++++++++", e.toString());
        }
        return "";
    }

    public void addFilePart(String paramName, String fileName, byte[] data) throws Exception {
        os.write( (delimiter + boundary + "\r\n").getBytes());
        os.write( ("Content-Disposition: form-data; name=\"" + paramName +  "\"; filename=\"" + fileName + "\"\r\n"  ).getBytes());
        os.write( ("Content-Type: application/octet-stream\r\n"  ).getBytes());
        os.write( ("Content-Transfer-Encoding: binary\r\n"  ).getBytes());
        os.write("\r\n".getBytes());

        os.write(data);

        os.write("\r\n".getBytes());
    }

    public void addFormPart(String paramName, String value) throws Exception {
        writeParamData(paramName, value);
    }

    private void writeParamData(String paramName, String value) throws Exception {
        os.write( (delimiter + boundary + "\r\n").getBytes());
        os.write( "Content-Type: text/plain\r\n".getBytes());
        os.write( ("Content-Disposition: form-data; name=\"" + paramName + "\"\r\n").getBytes());;
        os.write( ("\r\n" + value + "\r\n").getBytes());
    }

    public void finishMultipart() throws Exception {
        os.write( (delimiter + boundary + delimiter + "\r\n").getBytes());
    }

    public String getResponse() throws Exception {
        InputStream is = con.getInputStream();
        byte[] b1 = new byte[1024];
        StringBuffer buffer = new StringBuffer();

        while ( is.read(b1) != -1)
            buffer.append(new String(b1));

        con.disconnect();

        return buffer.toString();
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        try {
            JSONObject jo = new JSONObject(s);
            json_response.response(jo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}

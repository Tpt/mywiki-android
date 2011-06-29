package android.mywiki;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

class Api {
    protected static String lang = "";
    protected static String wiki = "";
    protected static boolean isConnected = false;
    protected static byte[] sBuffer = new byte[1024];

    public static class ApiException extends Exception {
        private static final long serialVersionUID = 404;

        public ApiException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public ApiException(String detailMessage) {
            super(detailMessage);
        }
    }


    public static String getUrl(String url) {
        return "http://" + Api.lang + ".m." + Api.wiki + "/" + url;
    }

    public static synchronized String getContent(String url) throws ApiException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                throw new ApiException("Invalid response from server: " + status.toString());
            }
            HttpEntity entity = response.getEntity();
            InputStream inputStream = entity.getContent();
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            // Read response into a buffered stream
            int readBytes = 0;
            while ((readBytes = inputStream.read(sBuffer)) != -1) {
                content.write(sBuffer, 0, readBytes);
            }
            return new String(content.toByteArray());
        } catch (IOException e) {
            Log.e("Wikipedia-api", e.getMessage());
            throw new ApiException("Problem communicating with API", e);
        }
    }

    //A revoir : bug pour le random
    public static synchronized String getRedirect(String url) throws ApiException {
        HttpClient client = new DefaultHttpClient();
        HttpHead request = new HttpHead(url);
        try {
            HttpResponse response = client.execute(request);
            StatusLine status = response.getStatusLine();
            Header header = response.getFirstHeader("location");
            if(header != null) {
                return response.getFirstHeader("location").getValue();
            } else {
                throw new ApiException("Invalid response from server: " + status.toString());
            }
        } catch (IOException e) {
            throw new ApiException("Invalid response from server: " + e.toString());
        }
    }


    public static String getContent(InputStream in) throws ApiException {
		StringBuffer buf = new StringBuffer();
    	if (in != null) {
			try {
    			InputStreamReader tmp = new InputStreamReader(in);
    			BufferedReader reader = new BufferedReader(tmp);
    			String str;
				while ((str = reader.readLine()) != null) {
					buf.append(str + "\n");
				}
	    		in.close();
	        } catch (IOException e) {
	            throw new ApiException("Error in the reading of the file: " + e.toString());
	        }
    	}
		return buf.toString();
    }
}

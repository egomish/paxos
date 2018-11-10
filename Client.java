import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.ProtocolException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.List;

public class Client {


public int getResponseCode ()
{
    return resCode;
}

public String getResponseBody ()
{
    return resBody;
}

//XXX: exceptions should be re-thrown instead of logged
public void sendAsync ()
{
    //set URL
    String urlstr = ip + service;
    try {
        url = new URL("http://" + urlstr);
    } catch (MalformedURLException e) {
        System.err.println("malformed url: " + e.getMessage());
        resCode = 400;
        resBody = ResponseBody.clientError().toJSON();
        return;
    }

    //open connection
    try {
        conn = (HttpURLConnection)url.openConnection();
    } catch (IOException e) {
        System.err.println("could not connect: " + e.getMessage());
        resCode = 400;
        resBody = ResponseBody.clientError().toJSON();
        return;
    }

    try {
        conn.setRequestMethod(method);
    } catch (ProtocolException e) {
        System.err.println("bad method: " + e.getMessage());
        resCode = 400;
        resBody = ResponseBody.clientError().toJSON();
        return;
    }

    //set request body
    try {
        if (body != null) {
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            out.write(body.getBytes());
            out.flush();
            out.close();
        }
    } catch (IOException e) {
        System.err.println("could not create body: " + e.getMessage());
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }
}

public void receiveAsync ()
{
    //send request
    try {
        conn.connect();
    } catch (ConnectException e) {
        System.err.println("server at " + url + " is down: " + e.getMessage());
        resCode = 501;
        resBody = ResponseBody.serverError().toJSON();
    } catch (NoRouteToHostException e) {
        System.err.println("server at " + url + " not reachable: " + e.getMessage());
        resCode = 501;
        resBody = ResponseBody.serverError().toJSON();
    } catch (IOException e) {
        System.err.println("I/O exception while sending request.");
        e.printStackTrace();
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
    }

    //get response
    try {
        resCode = conn.getResponseCode();
    } catch (IOException e) {
        System.err.println("bad rescode while handling response to " + url);
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }

    InputStream in = null;
    try {
        resHeaders = conn.getHeaderFields();
        if ((200 <= resCode) && (resCode < 300)) {
            in = conn.getInputStream();
        } else {
            in = conn.getErrorStream();
        }
    } catch (IOException e) {
        System.err.println("bad response body: " + e.getMessage());
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }

    //store response
    try {
        resBody = "";
        int b = in.read();
        while (b != -1) {
            resBody += (char)b;
            b = in.read();
        }
    } catch (IOException e) {
        System.err.println("could not read response body: " + e.getMessage());
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }
}

public Client (String i, String m, String s, String b)
{
    ip = i;
    method = m;
    service = s;
    body = b;

    url = null;
    conn = null;
    int resCode = 0;
    resBody = null;
}


private String ip;
private String method;
private String service;
private String body;

private URL url;
private HttpURLConnection conn;
private int resCode;
private Map<String, List<String>> resHeaders;
private String resBody;

}

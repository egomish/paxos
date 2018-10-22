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

public class ClientRequest {


public static String createQuery (String[][] pairs)
{
    String query = "";
    for (int i = 0; i < pairs.length; i += 1) {
        if (pairs[i].length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        if (i != 0) {
            query += "&";
        }
        for (int j = 0; j < pairs[i].length; j += 1) {
            query += pairs[i][j];
            if (j % 2 == 0) {
                query += "=";
            }
        }
    }
    return query;
}

public static String inputStreamToString (InputStream in)
{
    String str = "";
    try {
        int b = in.read();
        while (b != -1) {
            str += (char)b;
            b = in.read();
        }
    } catch (IOException e) {
        System.err.println("I/O exception while converting input stream to string.");
        str = null;
    }

    if (str.length() == 0) {
        str = null;
    }

    return str;
}

public static HttpResponse sendGetRequest (String ip, String service, String query)
{
    return sendRequest(ip, "GET", service, query, null);
}

public static HttpResponse sendPostRequest (String ip, 
                                      String service, 
                                      String query, 
                                      String body)
{
    return sendRequest(ip, "POST", service, query, body);
}

public static HttpResponse sendPutRequest (String ip, 
                                     String service, 
                                     String query, 
                                     String body)
{
    return sendRequest(ip, "PUT", service, query, body);
}

public static HttpResponse sendDeleteRequest (String ip, 
                                        String service, 
                                        String query,
                                        String body)
{
    return sendRequest(ip, "DELETE", service, query, body);
}

public static HttpResponse sendRequest (String ip, 
                                        String method, 
                                        String service, 
                                        String query, 
                                        String body)
{
    ClientRequest request = new ClientRequest();
    boolean success = true;

    String urlstr = ip + service;
    if (query != null) {
        urlstr += "?" + query;
    }
    success = request.setURL(urlstr);

    if (success) {
        success = request.doRequest(method, body);
    }

    if (success) {
        success = request.handleResponse();
    }

    HttpResponse response = new HttpResponse();
    response.setResponseCode(request.getResponseCode());
    response.setResponseBody(request.getResponseBody());

    return response;
}

private boolean setURL (String urlstr)
{
    //set URL
    try {
        url = new URL("http://" + urlstr);
    } catch (MalformedURLException e) {
        System.out.println("malformed url: " + e.getMessage());
        return false;
    }
    return true;
}

//XXX: rescode and resbody aren't set on every error
private boolean doRequest (String method, String body)
{
    //open connection
    try {
        conn = (HttpURLConnection)url.openConnection();
    } catch (IOException e) {
        System.out.println("I/O exception while opening connection.");
        e.printStackTrace();
        return false;
    }

    try {
        conn.setRequestMethod(method);
    } catch (ProtocolException e) {
        System.out.println("bad method: " + e.getMessage());
        return false;
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
        System.out.println("I/O exception while creating body.");
        e.printStackTrace();
        return false;
    }

    //send request
    try {
        conn.connect();
    } catch (ConnectException e) {
        System.out.println("server at " + url + " is down");
        System.out.println(e.getMessage());
        return false;
    } catch (NoRouteToHostException e) {
        System.out.println("server at " + url + " is not reachable by client");
        System.out.println(e.getMessage());
        rescode = 404;
        resbody = ResponseBody.serverError().toJSON();
        return true;
    } catch (IOException e) {
        System.out.println("I/O exception while sending request.");
        e.printStackTrace();
        return false;
    }
    return true;
}

private boolean handleResponse ()
{
    InputStream in = null;

    //get response
    try {
        rescode = conn.getResponseCode();
        headers = conn.getHeaderFields();
        if ((200 <= rescode) && (rescode < 300)) {
            in = conn.getInputStream();
        } else if ((400 <= rescode) && (rescode < 500)) {
            in = conn.getErrorStream();
        } else if (500 <= rescode) {
            in = conn.getErrorStream();
        }
    } catch (IOException e) {
            System.out.println("rescode: " + rescode);
            System.out.println("I/O exception while getting response.");
            System.out.println(e.getMessage());
            return false;
    }

    //store response
    try {
        int b = in.read();
        while (b != -1) {
            resbody += (char)b;
            b = in.read();
        }
    } catch (IOException e) {
        System.out.println("I/O exception while reading response.");
        e.printStackTrace();
        resbody = null;
        return false;
    }
    return true;
}

private int getResponseCode ()
{
    return rescode;
}

private String getResponseBody ()
{
    return resbody;
}

public ClientRequest ()
{
    url = null;
    conn = null;
    int rescode = 0;
    resbody = "";
}


private URL url;
private HttpURLConnection conn;
int rescode;
Map<String, List<String>> headers;
String resbody;

}
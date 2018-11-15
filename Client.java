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
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class Client implements Runnable {


/*
public static ExecutorService threadPool;

{
    threadPool = Executors.newCachedThreadPool();
}
*/

public static String fromInputStream (InputStream in)
{
    String str = "";

    try {
        int b = in.read();
        while (b != -1) {
            str += (char)b;
            b = in.read();
        }
    } catch (IOException e) {
        System.err.println("I/O exception: " + e.getMessage());
        str = "";
    }

    if (str.length() == 0) {
        str = null;
    }

    return str;
}

public static Client[] readyMulticast (String[] n, String m, String s, String b)
{
    Client[] clients = new Client[n.length];
    for (int i = 0; i < n.length; i += 1) {
        Client cl = new Client(n[i], m, s, b);
        clients[i] = cl;
    }
    return clients;
}

public static boolean doneMajority (Client[] clients)
{
    int majority = clients.length / 2 + 1;
    int rollcall = 0;

    for (Client cl : clients) {
        if (cl.done()) {
            rollcall += 1;
        }
    }

    if (rollcall < majority) {
        return false;
    }
    return true;
}

//interface Runnable
public void run ()
{
    sendAsync();
    receiveAsync();
    messageComplete = true;
}

public String getDestIP ()
{
    return request.getDestIP();
}

public int getResponseCode ()
{
    return resCode;
}

public String getResponseBody ()
{
    return resBody;
}

public boolean done ()
{
    if (messageComplete) {
        try {
            messageThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    return messageComplete;
}

public void fireAsync ()
{
    messageComplete = false;
    messageThread = new Thread(this);
    messageThread.start();
}

public void doSync ()
{
    this.sendAsync();
    this.receiveAsync();
}

//XXX: exceptions should be re-thrown instead of logged
private void sendAsync ()
{
    //set URL
    String urlstr = request.getDestIP() + request.getService();
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
        conn.setRequestMethod(request.getMethod());
    } catch (ProtocolException e) {
        System.err.println("bad method: " + e.getMessage());
        resCode = 400;
        resBody = ResponseBody.clientError().toJSON();
        return;
    }

    //set request body
    try {
        if (request.getBody() != null) {
            conn.setDoOutput(true);
            OutputStream out = conn.getOutputStream();
            out.write(request.getBody().getBytes());
            out.flush();
            out.close();
        }
    } catch (IOException e) {
        System.err.println("could not create body: " + e.getMessage());
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }

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
        return;
    } catch (IOException e) {
        System.err.println("I/O exception while sending request.");
        e.printStackTrace();
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }
}

private void receiveAsync ()
{
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
    resBody = Client.fromInputStream(in);
    if (resBody == null) {
        System.err.println("failed to read response body");
        resCode = 500;
        resBody = ResponseBody.serverError().toJSON();
        return;
    }
}

public Client (String ip, String method, String service, String body)
{
    request = new POJORequest(ip, method, service, body);
    url = null;
    conn = null;
    resCode = 0;
    resBody = null;
    messageComplete = true;
}

public Client (POJORequest req)
{
    request = req;
    url = null;
    conn = null;
    resCode = 0;
    resBody = null;
    messageComplete = true;
}


private POJORequest request;

private URL url;
private HttpURLConnection conn;
private int resCode;
private Map<String, List<String>> resHeaders;
private String resBody;
private Thread messageThread;
private boolean messageComplete;

}

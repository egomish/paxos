import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;


public class SmallServer {


protected void logReceive (String method, String path)
{
    System.err.println("[" + this.getClass().getName() + "] " + 
                       "Received " + method + " " + path + ".");
}

protected void logRespond (HttpRes response)
{
    System.err.println("[" + this.getClass().getName() + "] " + 
                       "Responding with " + response.resCode + ".");
}

protected void sendResponse (HttpExchange exch, HttpRes res)
{
    this.logRespond(res);
    try {
        if (res.contentType != null) {
            exch.getResponseHeaders().set("Content-Type", res.contentType);
        }
        exch.sendResponseHeaders(res.resCode, 0);
        OutputStream out = exch.getResponseBody();
        out.write(res.resBody.getBytes());
        out.close();
    } catch (Exception e) {
        //some kind of catastrophic error occurred
        e.printStackTrace();
        System.err.println("message not sent: " + e.getMessage());
    }
}

protected POJOReq[] getHistory ()
{
    return reqHistory.toArray(new POJOReq[0]);
}

public static void init_server ()
{
    /*
     *  Initialize ip, port, and pid.
     */
    String ipport = System.getenv().get("IP_PORT");
    if (ipport != null) {
        ipAndPort = ipport;
    } else {
        ipAndPort = "localhost:8080";
    }

    try {
        String[] strarr = ipAndPort.split(":");
        serverIP = strarr[0];
        serverPort = Integer.parseInt(strarr[1]);
    } catch (ArrayIndexOutOfBoundsException e) {
        //it would be really weird if we couldn't split ipAndPort on ':'
        e.printStackTrace();
        serverIP = null;
        serverPort = 0;
    } catch (NumberFormatException e) {
        //it would be really weird if we couldn't parse the port as an int
        e.printStackTrace();
        serverIP = null;
        serverPort = 0;
    }

    //XXX: won't work if the last char of the port is the same for any nodes
    String pid = String.valueOf(ipAndPort.charAt(ipAndPort.length() - 1));
    try {
    } catch (NumberFormatException e) {
        //something really bad has happened if pid isn't a number
        e.printStackTrace();
        processID = 0;
    }

    /*
     *  Initialize the view of the distributed system.
     */
    String nodes = System.getenv().get("VIEW");
    nodeView = new HashSet<String>();
    if (nodes != null) {
        String[] view = nodes.split(",");
        for (String node : view) {
            nodeView.add(node);
        }
    } else {
        nodeView.add(ipAndPort);
    }

    /*
     *  Initialize the total order of requests.
     */
    reqHistory = new ArrayList<POJOReq>();
    reqIndex = 0;
}

/* 
 *  These environment variables must be set as follows:
 *  IP_PORT: <ip>:<port>
 *  VIEW:    <ip>:<port>[,<ip>:<port>...]
 */
public static void
main(String[] args) throws Exception
{
    /*
     *  Initialize global variables.
     */
    SmallServer.init_server();

    /*
     *  Bind the server to the specified ip and port.
     */
    HttpServer server = HttpServer.create(new InetSocketAddress(serverIP, serverPort), 0);
    System.err.println("Server running at " + ipAndPort + ".");

    /*
     *  Add contexts to the server to respond to RESTful API calls.
     */
    server.createContext("/hello", new ContextHello());
    server.createContext("/test", new ContextTest());
    server.createContext("/keyValue-store", new ContextKVS());
    server.createContext("/keyValue-store/search", new ContextKVSSearch());

    /*
     *  Allow the server to use threads to handle requests.
     */
    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());

    /*
     *  Start accepting requests.
     */
    server.start();
}

public static String ipAndPort;
public static String serverIP;
public static int serverPort;
public static int processID;

public static HashSet<String> nodeView;

public static ArrayList<POJOReq> reqHistory;
public static int reqIndex;

}

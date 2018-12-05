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


public class SmallServer implements HttpHandler {

public void handle (HttpExchange exch)
{
    String query = exch.getRequestURI();
    if (query == null) {
        query = "";
    }

    if (this.isBackdoorEndpoint(path)) {
        //execute the request directly
        this.contextAt(path).handle(exch);
    } else if (query.contains("fromhistory=true")) {
        //the request is in the history--execute it
        this.contextAt(path).handle(exch);
    } else {
        //get consensus and add the request to the system's history
        this.getConsensus(exch);
    }
}

private boolean isBackdoorEndpoint (String path)
{
    if (path.startsWith("/hello")) {
        return true;
    } else if (path.startsWith("/test")) {
        return true;
    } else if (path.startsWith("/paxos")) {
        return true;
    } else if (path.startsWith("/history")) {
        return true;
//    } else if (path.startsWith("/shard")) {
//        return true;
    }
    return false;
}

protected void logReceive (String method, String path)
{
//    this.log("Received " + method + " " + path + ".");
}

protected void logRespond (HttpRes response)
{
//    this.log("Responding with " + response.resCode + ".");
}

protected void log (String str)
{
//    System.err.println("(" + Thread.currentThread().getName() + ") " + str);
    System.err.println("(" + Thread.currentThread().getName() + ") [" + this.getClass().getName() + "] " + str);
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
        if (res.resBody != null) {
            out.write(res.resBody.getBytes());
        }
        out.close();
    } catch (Exception e) {
        //some kind of catastrophic error occurred
        e.printStackTrace();
        System.err.println("message not sent: " + e.getMessage());
    }
}

protected String[] getNodeView ()
{
    return nodeView.toArray(new String[0]);
}

protected boolean nodeViewContains (String ipport)
{
    return nodeView.contains(ipport);
}

protected boolean addToView (String ipport)
{
    return nodeView.add(ipport);
}

protected int getQuorumSize ()
{
    return (nodeView.size() / 2 + 1);
}

/*
 *  Adds the request to the history and returns the index it was added at.
 */
protected int getConsensus (HttpExchange exch)
{
    int reqindex = -1;

    //create request to get consensus on
    String ip = this.ipAndPort;
    String method = exch.getRequestMethod();
    String url = exch.getRequestURI().toString();
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOReq prop = new POJOReq(ip, method, url, reqbody);
    String body = prop.toJSON();

    //send the request for consensus
    Client cl = new Client(new POJOReq(ip, "POST", "/paxos/propose", body));
    cl.doSync();
    HttpRes res = cl.getResponse();
    if (res.resCode == 200) {
        try {
            reqindex = Integer.valueOf(res.resBody);
        } catch (NumberFormatException e) {
            //something has gone horribly wrong if resbody isn't reqindex
            e.printStackTrace();
        }
    } else {
        //catastrophic failure occurred while getting consensus
        System.err.println("bad consensus: " + res.resCode + " " + res.resBody);
    }
    return reqindex;
}

protected POJOReq[] getHistory ()
{
    //----| begin transaction |----
    POJOReq[] arr = new POJOReq[reqHistory.size()];
    for (int i = 0; i < reqHistory.size(); i += 1) {
        POJOReq req = reqHistory.get(i).clientReq;
        arr[i] = req;
    }
    //----|  end transaction  |----
    return arr;
}

protected POJOReq getHistoryAt (Integer index)
{
    return reqHistory.get(index).clientReq;
}

protected int getNextHistoryIndex()
{
    return commitIndex + 1;
}

/*
 *  Returns true if the history did not already contain an entry at <reqindex>.
 */
protected boolean addToHistoryAt (int reqindex, HistoryElement elem)
{
    HistoryElement preventry = reqHistory.put(reqindex, elem);
    if (preventry == null) {
        return true;
    }
    return false;
}

protected void exposeHistoryAt (int reqindex)
{
    //XXX: what if reqindex > commitIndex + 1?
    if (reqindex == commitIndex + 1) {
        commitIndex = reqindex;
    } else if (reqindex < commitIndex + 1) {
       //do nothing--history[reqindex] has already been exposed
    }
}

protected void playHistoryTo (int endindex)
{
    for (runIndex = runIndex; runIndex <= endindex; runIndex += 1) {
        if (runIndex > commitIndex) {
            //TODO: figure out if this is possible
            break;
        }
        HistoryElement fromhistory = reqHistory.get(runIndex);
        if (fromhistory.shardID == this.shardID) {
            String destip = this.ipAndPort;
            String method = fromhistory.reqMethod;
            String url = fromhistory.reqURL + "?fromhistory=true"; //XXXESG hack
            String body = fromhistory.reqBody;
            POJOReq request = new POJOReq(destip, method, url, body);
            Client cl = new Client(request);
            cl.doSync();
            fromhistory.reqResponse = cl.getResponse();
        }
    }
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
        processID = Integer.valueOf(pid);
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
    reqHistory = new HashMap<Integer, HistoryElement>();
    commitIndex = -1;
    runIndex = 0;

    /*
     *  Add endpoints to the server.
     */
    server.serverEndpoints = new HashMap<String, Endpoint>();
    server.serverEndpoints.put("/hello", new ContextHello());
    server.serverEndpoints.put("/test", new ContextTest());
    server.serverEndpoints.put("/keyValue-store", new ContextKVS());
    server.serverEndpoints.put("/keyValue-store/search", new ContextKVSSearch());
    server.serverEndpoints.put("/paxos", new ContextPaxos());
    server.serverEndpoints.put("/view", new ContextView());
    server.serverEndpoints.put("/history", new ContextHistory());
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
    server.createContext("/", server);

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

public static HashMap<String, SmallServer> serverEndpoints;

public static HashMap<Integer, HistoryElement> reqHistory;
public static int runIndex;
public static int commitIndex;

}

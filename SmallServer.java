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
import java.util.TreeMap;


public class SmallServer {

public POJOReq parseRequest (HttpExchange exch)
{
    String ip = this.ipAndPort;
    String method = exch.getRequestMethod();
    String url = exch.getRequestURI().toString();
    String body = Client.fromInputStream(exch.getRequestBody());
    POJOReq request = new POJOReq(ip, method, url, body);

    request.urlPath = exch.getRequestURI().getPath();
    request.urlQuery = exch.getRequestURI().getQuery();
    request.charEncoding = exch.getRequestHeaders().getFirst("Content-type");

    return request;
}

public POJOReq consensusProtocol (POJOReq request)
{
    String path = request.urlPath;
    String query = request.urlQuery;

    if (isBackdoorEndpoint(path)) {
        //handle request directly
        return request;
    } else if (isFromHistory(query)) {
        //handle request directly
        return request;
    } else {
        //get consensus for the request before handling it
        int reqindex = this.getConsensus(request);
        request.historyIndex = reqindex;
        return request;
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
    }
    return false;
}

private boolean isFromHistory (String query)
{
    if ((query != null) && (query.contains("fromhistory=true"))) {
        return true;
    }
    return false;
}

public POJOReq shardProtocol (POJOReq request, String key)
{
    Integer shardid = keyDir.get(key);
    if (shardid != null) {
        request.shardID = shardid;
        return request;
    }
    shardid = this.getSparsestShard();
    keyDir.put(key, shardid);
    request.shardID = shardid;
    return request;
}

protected int getSparsestShard ()
{
    int smalli = 0;
    for (int i = 0; i < shardDir.size(); i += 1) {
        if (shardDir.get(i).size() < shardDir.get(smalli).size()) {
            smalli = i;
        }
    }
    return smalli;
}

protected int getDensestShard ()
{
    int largei = 0;
    for (int i = 0; i < shardDir.size(); i += 1) {
        if (shardDir.get(i).size() > shardDir.get(largei).size()) {
            largei = i;
        }
    }
    return largei;
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
    System.err.println("(" + Thread.currentThread().getName() + ") " + str);
//    System.err.println("(" + Thread.currentThread().getName() + ") [" + this.getClass().getName() + "] " + str);
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

protected boolean removeFromView (String ipport)
{
    boolean removed = nodeView.remove(ipport);
    return removed;
}

protected int getQuorumSize ()
{
    return (nodeView.size() / 2 + 1);
}

/*
 *  Uses Paxos to place the request in the history, adds metadata, then 
 *  returns the request.
 */
protected int getConsensus (POJOReq request)
{
    String ip = request.destIP;
    String body = request.toJSON();
    Client cl = new Client(new POJOReq(ip, "POST", "/paxos/propose", body));
    cl.doSync();

    int reqindex;
    HttpRes res = cl.getResponse();
    if (res.resCode == 200) {
        try {
            reqindex = Integer.valueOf(res.resBody);
        } catch (NumberFormatException e) {
            //something has gone horribly wrong if resbody isn't reqindex
            e.printStackTrace();
            reqindex = -1;
        }
    } else {
        //catastrophic failure occurred while getting consensus
        System.err.println("bad consensus: " + res.resCode + " " + res.resBody);
        reqindex = -1;
    }
    return reqindex;
}

protected POJOReq[] getHistory ()
{
    //----| begin transaction |----
    POJOReq[] arr = new POJOReq[reqHistory.size()];
    for (int i = 0; i < reqHistory.size(); i += 1) {
        POJOReq req = reqHistory.get(i);
        arr[i] = req;
    }
    //----|  end transaction  |----
    return arr;
}

protected POJOReq getHistoryAt (Integer index)
{
    return reqHistory.get(index);
}

protected int getNextHistoryIndex()
{
    return commitIndex + 1;
}

/*
 *  Returns true if the history did not already contain an entry at <reqindex>.
 */
protected boolean addToHistoryAt (int reqindex, POJOReq request)
{
    POJOReq preventry = reqHistory.put(reqindex, request);
    if (preventry == null) {
        return true;
    }
    return false;
}

protected synchronized void replayHistory ()
{
    int endindex = commitIndex;
    commitIndex = 0;
    this.kvStore = new HashMap<String, String>();

    for (runIndex = 0; runIndex <= endindex; runIndex += 1) {
        POJOReq fromhistory = reqHistory.get(runIndex);
        if (fromhistory.shardID == this.shardID) {
            String destip = this.ipAndPort;
            String method = fromhistory.reqMethod;
            String url = fromhistory.reqURL + "?fromhistory=true"; //XXXESG hack
            String body = fromhistory.reqBody;
            POJOReq request = new POJOReq(destip, method, url, body);
            Client cl = new Client(request);
            cl.doSync();
        }
    }
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
        ippIP = strarr[0];
        ippPort = Integer.parseInt(strarr[1]);
    } catch (ArrayIndexOutOfBoundsException e) {
        //it would be really weird if we couldn't split ipAndPort on ':'
        e.printStackTrace();
        ippIP = null;
        ippPort = 0;
    } catch (NumberFormatException e) {
        //it would be really weird if we couldn't parse the port as an int
        e.printStackTrace();
        ippIP = null;
        ippPort = 0;
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
    reqHistory = new HashMap<Integer, POJOReq>();
    commitIndex = -1;
    runIndex = 0;

    /*
     *  Initialize sharding.
     */
    shardID = 0;
    keyDir = new HashMap<String, Integer>();
    shardDir = new ArrayList<ArrayList<String>>();

    String shards = System.getenv().get("S");
    if (shards != null) {
        try {
            shardCount = Integer.valueOf(shards);
        } catch (NumberFormatException e) {
            shardCount = 1;
        }
    } else {
        shardCount = 1;
    }
    shardDir.add(new ArrayList<String>());
    shardView = new TreeMap<String, Integer>();
//    int count = 0;
//    for (int i = 0; i < shardCount; i += 1) {
//        String node = nodeView.get(i);
//        shardView.add(node, i % shardView.size());
//    }
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
    HttpServer server = HttpServer.create(new InetSocketAddress(ippIP, ippPort), 0);
    System.err.println("Server running at " + ipAndPort + ".");

    /*
     *  Add contexts to the server to respond to RESTful API calls.
     */
    server.createContext("/hello", new ContextHello());
    server.createContext("/test", new ContextTest());
    server.createContext("/keyValue-store", new ContextKVS());
    server.createContext("/paxos", new ContextPaxos());
    server.createContext("/view", new ContextView());
    server.createContext("/history", new ContextHistory());
    server.createContext("/shard", new ContextShard());

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
public static String ippIP;
public static int ippPort;
public static int processID;
public static HashSet<String> nodeView;

public static HashMap<String, String> kvStore;

public static HashMap<Integer, POJOReq> reqHistory;
public static int runIndex;
public static int commitIndex;

public static int shardID;
public static int shardCount;
public static TreeMap<String, Integer> shardView;
public static ArrayList<ArrayList<String>> shardDir;
public static HashMap<String, Integer> keyDir;

}

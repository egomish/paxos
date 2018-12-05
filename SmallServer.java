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


public class SmallServer {


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

protected HttpRes giveToShardOracle (HttpExchange exch)
{
    //package client request into request body
    String method = exch.getRequestMethod();
    String url = exch.getRequestURI().toString();
    String body = Client.fromInputStream(exch.getRequestBody());
    POJOReq clientreq = new POJOReq(this.ipAndPort, method, url, body);

    //get oracle address
    String ip = this.oracleIP;

    //send shard request to oracle
    POJOReq req = new POJOReq(ip, "POST", "/shard/process", clientreq);
    Client cl = new Client(req);
    cl.doSync();

    //return request response to client

    //process request
    String shardip = this.getShardAt(keyhash);
    String method = "POST";
    String url = "/shard/process";
    String reqbody = request.toJSON();
    POJOReq req = new POJOReq(shardip, method, url, reqbody);
    Client cl = new Client(request);
    cl.doSync();

    //return response
    HttpRes response =  cl.getResponse();
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

protected void exposeHistoryAt (int reqindex)
{
    //XXX: what if reqindex > commitIndex + 1?
    if (reqindex == commitIndex + 1) {
        commitIndex = reqindex;
    } else if (reqindex < commitIndex + 1) {
       //do nothing--history[reqindex] has already been exposed
    }
}

protected HttpRes playHistoryTo (int endindex)
{
    HttpRes response = null;
    for (runIndex = runIndex; runIndex <= endindex; runIndex += 1) {
        if (runIndex > commitIndex) {
            //TODO: figure out if this is possible
            break;
        }
        POJOReq fromhistory = reqHistory.get(runIndex);
        String destip = this.ipAndPort;
        String method = fromhistory.reqMethod;
        String url = fromhistory.reqURL + "?fromhistory=true"; //XXXESG a hack
        String body = fromhistory.reqBody;
        POJOReq request = new POJOReq(destip, method, url, body);
        Client cl = new Client(request);
        cl.doSync();
        response = cl.getResponse();
    }
    return response;
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
     *  Initialize the view of the shard directory.
     *  XXXESG: for now, we use a single server as a directory.
     */
    oracleIP = "10.0.0.2:4002";

    /*
     *  Initialize the total order of requests.
     */
    reqHistory = new HashMap<Integer, POJOReq>();
    commitIndex = -1;
    runIndex = 0;
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
public static String serverIP;
public static int serverPort;
public static int processID;
public static HashSet<String> nodeView;

public static HashMap<Integer, POJOReq> reqHistory;
public static int runIndex;
public static int commitIndex;

public static String oracleIP;

}

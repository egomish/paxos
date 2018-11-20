import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.OutputStream;
import java.util.HashSet;


public class SmallServer {


public static boolean DEBUG_loggingHigh = true;

public String toString ()
{
    return "[" + this.getClass().getName() + "]";
}

protected void printLog (String str)
{
    if (DEBUG_loggingHigh) {
        System.err.println(log(str));
    }
}

protected String log (String str)
{
    String log = "";
    log += "[" + this.getClass().getName() + "] ";
    log += str;
    return log;
}

protected String receiveLog (String method, String path)
{
    return log("Request: " + method + " " + path);
}

protected String respondLog (POJOResHttp response)
{
    return log("Responding: " + response.resCode);
}

protected POJOResHttp forwardToPrimary (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    Client cl = new Client(primaryIPAddress, method, path, reqbody);
    cl.doSync();
    POJOResHttp response = cl.getResponse();
    return response;
}

protected POJOResHttp doProposal (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String service = exch.getRequestURI().getPath();
    String body = Client.fromInputStream(exch.getRequestBody());
    POJOReqHttp req = new POJOReqHttp(ipAndPort, method, service, body);
    Client cl = new Client(ipAndPort, "POST", "/paxos/proposer", req.toJSON());
    cl.doSync();
    POJOResHttp response = cl.getResponse();
    return response;

}

protected void sendResponse (HttpExchange exch, int rescode, String resmsg)
{
    this.sendResponse(exch, new POJOResHttp(rescode, resmsg));
}

/*
 *  Note: response bodies are assumed to be JSON.
 */
protected void sendResponse (HttpExchange exch, POJOResHttp res)
{
//    System.err.println(this.respondLog(res));
    try {
        exch.getResponseHeaders().set("Content-Type", "application/json");
        exch.sendResponseHeaders(res.resCode, 0);
        OutputStream out = exch.getResponseBody();
        out.write(res.resBody.getBytes());
        out.close();
    } catch (Exception e) {
        System.err.println("message not sent: " + e.getMessage());
        //XXX: kills the server so that the connection terminates
        System.exit(1);
    }
}

protected String[] getNodeView ()
{
    return nodeView.toArray(new String[0]);
}

protected String getNodeViewAsString ()
{
    String view = "";
    for (String node : this.getNodeView()) {
        view += node + ",";
    }
    view = view.substring(0, view.length() - 1);
    return view;
}

protected String nodeViewAsString ()
{
    String str = "";
    for (String node : this.getNodeView()) {
        str += node + ",";
    }
    str = str.substring(0, str.length() - 1);
    return str;
}

protected boolean addToView (String ipport)
{
    return nodeView.add(ipport);
}

protected boolean removeFromView (String ipport)
{
    return nodeView.remove(ipport);
}

protected String nodeViewAsJSON ()
{
    String str = "";
    str += "{";
    str += "view: ";
    for (String node : this.nodeView) {
        str += node + ",";
    }
    str = str.substring(0, str.length() - 1);
    str += "}";
    return str;
}

protected void addToHistoryAt (int reqindex, String request)
{
    reqHistory.history.put(reqindex, request);
}

protected String getHistoryAt (int reqindex)
{
    String request = reqHistory.history.get(reqindex);
    return request;
}

protected void replayHistory (POJOHistory pojo)
{
    this.reqHistory = pojo;
    //reset the key-value store so history can be replayed
    ContextKVS.kvStore.clear();

    printLog("replaying history...");
    for (int i = 0; i < this.reqHistory.history.size(); i += 1) {
        String req = this.reqHistory.history.get(i);
        if (req == null) {
            //no request was specified for this index--wait to keep replaying
            //XXX: when could this possibly happen?
            System.err.println("no request for index " + i + ".");
            break;
        }
        POJOReqHttp request = POJOReqHttp.fromJSON(req);
        String ip = ipAndPort;
        String method = request.method;
        String service = request.service;
        //this is a terrible hack to ensure we don't paxos replayed requests
        service += "?consensus=true"; //XXXESG DEBUG
        String body = request.body;
        System.out.println("(" + i + ") [" + ip + "] " + method + " " + service);
        Client cl = new Client(ip, method, service, body);
        cl.doSync();
        POJOResHttp response = cl.getResponse();
        printLog("replayed " + request.method + " " + request.service);
    }
}

protected String getHistoryAsJSON ()
{
    String json = reqHistory.toJSON();
    return json;
}

private static void initServer ()
{
    /*
     * For setting self ip address and port.
     */
    String ipport = System.getenv().get("IP_PORT");
    if (ipport != null) {
         ipAndPort = ipport;
    } else {
         ipAndPort = "localhost:8080";
    }
    //TODO: replace with hash of ipAndPort instead of getting last char of port
    String pid = String.valueOf(ipAndPort.charAt(ipAndPort.length() - 1));
    try {
        processID = Integer.parseInt(pid);
    } catch (NumberFormatException e) {
        //something really really bad has happened here if pid isn't a number
        e.printStackTrace();
        processID = 0;
    }

    /*
     *  For creating a view of the distributed system.
     */
    String nodes = System.getenv().get("VIEW");
    nodeView = new HashSet<String>();
    if (nodes == null) {
        nodeView.add(ipAndPort);
    } else {
        String[] view = nodes.split(",");
        for (String node : view) {
            nodeView.add(node);
        }
    }

    /*
     *  For message forwarding.
     */
    String mainip = System.getenv().get("MAINIP");
    if (mainip != null) {
        primaryIPAddress = mainip;
        isPrimary = false;
    } else {
        primaryIPAddress = null;
        isPrimary = true;
    }

    /*
     *  For establishing a total order of requests.
     */
    reqHistory = new POJOHistory();
}

/*
 *  Set the following environment variables to run correctly:
 *  IP_PORT: <ip>:<port>
 *  VIEW:    <ip>:<port>[,<ip>:<port> ...]
 *  MAINIP:  <ip>:<port>
 */
public static void
main(String[] args) throws Exception
{
    /*
     *  Initialize global variables.
     */
    SmallServer.initServer();

    /*
     *  Parse the IP address and port number so the server can be bound.
     */
    String[] ipport = SmallServer.ipAndPort.split(":");
    String ip = ipport[0];
    int port;
    try {
        port = Integer.parseInt(ipport[1]);
    } catch (NumberFormatException e) {
        e.printStackTrace();
        port = 8080;
    }

    /*
     *  Bind the server to the specified ip and port.
     */
    HttpServer server = HttpServer.create(new InetSocketAddress(ip, port), 0);
    System.err.println("Server running at " + ipAndPort + ".");

    /*
     *  Add contexts to the server to respond to RESTful API calls.
     */
    server.createContext("/hello", new ContextHello());
    server.createContext("/test", new ContextTest());
    server.createContext("/keyValue-store", new ContextKVS());
    server.createContext("/paxos/proposer", new ContextPaxosProposer());
    server.createContext("/paxos/acceptor", new ContextPaxosAcceptor());
    server.createContext("/history", new ContextHistory());
    server.createContext("/view", new ContextView());
    server.createContext("/join", new ContextJoin());

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
public static int processID;

public static HashSet<String> nodeView;

public static String primaryIPAddress;
public static boolean isPrimary;

public static POJOHistory reqHistory;


}

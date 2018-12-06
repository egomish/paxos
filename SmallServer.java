import java.util.HashMap;
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
import java.util.Iterator;
import java.io.InputStream;


public class SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String ip = this.ipAndPort;
    String method = exch.getRequestMethod();
    String url = exch.getRequestURI().toString();
    String reqbody = SmallServer.fromInputStream(exch.getRequestBody());
    POJOReq request = new POJOReq(ip, method, url, reqbody);
    System.out.println("handling: " + request);

    HttpRes response;
    
    String path = exch.getRequestURI().getPath();
    String query = exch.getRequestURI().getQuery();
    if (query == null) {
        query = "";
    }

    if (this.isBackdoorEndpoint(path)) {
        //execute the request directly
        response = getEndpoint(path).handle(request);
    } else if (query.contains("fromhistory==true")) {
        //the request is in the history--execute it
        response = getEndpoint(path).handle(request);
    } else if (this.requiresConsensus(path)) {
        //get consensus and add the request to the system's history
        int reqindex = this.getConsensus(request);
        //TODO: get the result from history and return it as the response
        response = HttpRes.serverError();
    } else {
        response = new HttpRes(404, path + " not found");
        response.contentType = null;
    }

    sendResponse(exch, response);
}

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

private void sendResponse (HttpExchange exch, HttpRes response)
{
    try {
        if (response.contentType != null) {
            exch.getResponseHeaders().set("Content-Type", response.contentType);
        }
        exch.sendResponseHeaders(response.resCode, 0);
        OutputStream out = exch.getResponseBody();
        if (response.resBody != null) {
            out.write(response.resBody.getBytes());
        }
        out.close();
    } catch (Exception e) {
        //some kind of catastrophic error occurred
        e.printStackTrace();
        System.err.println("message not sent: " + e.getMessage());
    }
}

private boolean isBackdoorEndpoint (String path)
{
    Iterator<String> it = backdoorEndpoints.iterator();
    while (it.hasNext()) {
        String endpoint = it.next();
        System.out.print(endpoint + " <= " + path + "?");
        if (path.startsWith(endpoint)) {
            System.out.print(" yes");
            return true;
        }
        System.out.println();
    }
    return false;
}

private boolean requiresConsensus (String path)
{
    Iterator<String> it = consensusEndpoints.iterator();
    while (it.hasNext()) {
        String endpoint = it.next();
        System.out.print(endpoint + " <= " + path + "?");
        if (path.startsWith(endpoint)) {
            System.out.print(" yes");
            return true;
        }
        System.out.println();
    }
    return false;
}

private Context getEndpoint (String path)
{
    for (String endpoint : allEndpoints.keySet()) {
        System.out.print(endpoint + " <= " + path + "?");
        if (path.startsWith(endpoint)) {
            System.out.println(" yes");
            return allEndpoints.get(endpoint);
        }
    }
    return null;
}

private int getConsensus (POJOReq request)
{
    int reqindex = -1;
    System.out.println("consensus not implemented!!");
    return reqindex;
}

//XXX: if key contains '/' characters, key will be parsed as just the 
//     characters following the final slash in the key
private static String parseKeyFromPath (String path)
{
    if (path == null) {
        return null;
    }
    String[] strarr = path.split("/");
    String key = strarr[strarr.length - 1];
    //make sure key is actually the whole key
    return key;
}

//XXX: only handles requests with no content type or form-urlecoded
//     assumes that if there is an encoding, only the first one matters
private static String parseValueFromRequest (HttpExchange exch)
{
    RequestBody reqbody = new RequestBody(exch.getRequestBody());
    String value = reqbody.value();

    String contenttype = exch.getRequestHeaders().getFirst("Content-type");
    if (contenttype != null) {
        if (contenttype.equals("application/x-www-form-urlencoded")) {
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                System.err.println("bad encoding: " + e.getMessage());
            }
        } else {
            System.out.println("the content type is " + contenttype);
        }
    }

    return value;
}

private static boolean keyIsValid (String key)
{
    if (key.length() == 0) {
        return false;
    }
    if (key.length() > 200) {
        return false;
    }
    for (int i = 0; i < key.length(); i += 1) {
        if ('a' <= key.charAt(i) && key.charAt(i) <= 'z') {
            continue;
        }
        if ('A' <= key.charAt(i) && key.charAt(i) <= 'Z') {
            continue;
        }
        if ('0' <= key.charAt(i) && key.charAt(i) <= '9') {
            continue;
        }
        if (key.charAt(i) == '_') {
            continue;
        }
        return false;
    }
    return true;
}

private static boolean valueIsValid (String value)
{
    if (value == null) {
        return false;
    } else if (value.getBytes().length > 1000000) {
        return false;
    }
    return true;
}

/*
private void doHello (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();
    int rescode = 200;
    String resmsg = "";
    if (method.equals("GET")) {
        resmsg = "Hello world!";
    } else {
        rescode = 405;
        resmsg = method + " " + path + " not allowed";
    }
    sendResponse(exch, rescode, resmsg, null);
}

private void doTest (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();
    String query = uri.getQuery();
    int rescode = 200;
    String resmsg = "";
    if (method.equals("GET")) {
        resmsg = "GET request received";
    } else if (method.equals("POST")) {
        if (query == null) {
            resmsg = "POST message received: null";
        } else {
            int eqindex = query.indexOf("=");
            resmsg = "POST message received: " + query.substring(eqindex + 1);
        }
    } else {
        rescode = 405;
        resmsg = method + " " + path + " not allowed";
    }
    sendResponse(exch, rescode, resmsg, null);
}

private void doKVS (HttpExchange exch)
{
    //request fields
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String urlkey = parseKeyFromPath(uri.getPath());

    //response fields
    int rescode = 200;
    String restype = "";
    String resmsg = "";

    String subpath = uri.getPath().substring(16); //subtract "/keyValue-store/"
    if (subpath.startsWith("search/")) {
        String subsubpath = subpath.substring(7); //subtract "search/"
        if (!method.equals("GET")) {
            rescode = 405;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody(false, "method not allowed");
            resmsg = resbody.toJSON();
        } else {
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            String value = kvStore.get(urlkey);
            if (value != null) {
                rescode = 200;
                resbody.setResult(true, "key exists");
                resbody.setMessageString("Success");
                resbody.setExistFlag("true");
            } else {
                rescode = 404;
                resbody.setResult(false, "key does not exist");
                resbody.setMessageString("Error");
                resbody.setErrorString("Key does not exist");
                resbody.setExistFlag("false");
            }
            resmsg = resbody.toJSON();
        }
    } else {
        if (method.equals("GET")) {
            String value = kvStore.get(urlkey);
            if (value == null) {
                rescode = 404;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(false, "key does not exist");
                resbody.setMessageString("Error");
                resbody.setErrorString("Key does not exist");
                resmsg = resbody.toJSON();
            } else {
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(true, value);
                resbody.setMessageString("Success");
                resbody.setValueString(value);
                resmsg = resbody.toJSON();
            }
        } else if (method.equals("PUT")) {
            String reqvalue = parseValueFromRequest(exch);
            if (!SmallServer.keyIsValid(urlkey)) {
                rescode = 422;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(false, "key is invalid");
                resbody.setMessageString("Error");
                resbody.setErrorString("Key not valid");
                resmsg = resbody.toJSON();
            } else if (!SmallServer.valueIsValid(reqvalue)) {
                rescode = 422;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(false, "value is invalid");
                resbody.setMessageString("Error");
                resbody.setErrorString("Value is missing");
                resmsg = resbody.toJSON();
            } else if (kvStore.get(urlkey) == null) {
                kvStore.put(urlkey, reqvalue);
                rescode = 201;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(true, "key added");
                resbody.setReplacedFlag(0);
                resbody.setMessageString("Added successfully");
                resmsg = resbody.toJSON();
            } else {
                kvStore.put(urlkey, reqvalue);
                rescode = 200;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(true, "key updated");
                resbody.setDebugString(reqvalue);
                resbody.setMessageString("Updated successfully");
                resbody.setReplacedFlag(1);
                resmsg = resbody.toJSON();
            }
        } else if (method.equals("DELETE")) {
            String value = kvStore.get(urlkey);
            if (value != null) {
                kvStore.remove(urlkey);
                rescode = 200;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(true, "key deleted");
                resbody.setMessageString("Success");
                resmsg = resbody.toJSON();
            } else {
                rescode = 404;
                restype = "application/json";
                ResponseBody resbody = new ResponseBody(false, "key does not exist");
                resbody.setMessageString("Error");
                resbody.setErrorString("Key does not exist");
                resmsg = resbody.toJSON();
            }
        } else {
            rescode = 405;
            resmsg = method + " " + uri + " not allowed";
        }
    }
    sendResponse(exch, rescode, resmsg, restype);
}
*/

private void addEndpoint (String path, Context context, boolean consensus)
{
    allEndpoints.put(path, context);
    if (consensus) {
        consensusEndpoints.add(path);
    } else {
        backdoorEndpoints.add(path);
    }
}

private SmallServer()
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
     *  Initialize endpoints for the server.
     */
    allEndpoints = new HashMap<String, Context>();
    backdoorEndpoints = new HashSet<String>();
    consensusEndpoints = new HashSet<String>();

    boolean needconsensus = true;
    addEndpoint("/hello", new ContextHello(this), !needconsensus);
    addEndpoint("/keyValue-store", new ContextKVS(this), needconsensus);
}

public static void
main(String[] args) throws Exception
{
    SmallServer ss = new SmallServer();
    HttpServer server = HttpServer.create(new InetSocketAddress(ss.serverIP, ss.serverPort), 0);
    System.err.println("Server running at " + ss.ipAndPort + ".");
    server.createContext("/", ss);
    server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
    server.start();
}

public String ipAndPort;
public String serverIP;
public int serverPort;
public int processID;

public HashMap<String, Context> allEndpoints;
private HashSet<String> backdoorEndpoints;
private HashSet<String> consensusEndpoints;

}

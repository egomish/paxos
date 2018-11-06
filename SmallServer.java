import java.util.Random;




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


public class SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    System.err.println("Handling " + exch.getRequestMethod() + " request...");
    if (!isPrimary) {
        HttpResponse response = forwardRequestToPrimary(exch);
        sendResponse(exch, response.getResponseCode(), response.getResponseBody(), null);
        return;
    }

    String path = exch.getRequestURI().getPath();
    if (path.startsWith("/hello")) {
        doHello(exch);
    } else if (path.startsWith("/test")) {
        doTest(exch);
    } else if (path.startsWith("/keyValue-store")) {
        doKVS(exch);
    } else {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
    }
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

private HttpResponse forwardRequestToPrimary (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();
    String query = exch.getRequestURI().getQuery();
    String reqbody = ClientRequest.inputStreamToString(exch.getRequestBody());
    HttpResponse response = ClientRequest.sendRequest(primaryIPAddress, method, path, query, reqbody);
    return response;
}

//XXX: exceptions are caught here regardless of type
//XXX: on any failure, server is stopped
public void sendResponse (HttpExchange exch, 
                           int rescode, 
                           String resmsg, 
                           String restype)
{
    try {
        String method = exch.getRequestMethod();
        System.err.println("Responding to " + method + " with " + rescode + ".");
        if (restype != null) {
            exch.getResponseHeaders().set("Content-Type", restype);
        }
        exch.sendResponseHeaders(rescode, 0);
        OutputStream out = exch.getResponseBody();
        out.write(resmsg.getBytes());
        out.close();
    } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
    }
}

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

public SmallServer()
{
    String mainip = System.getenv().get("MAINIP");
    if (mainip == null) {
        primaryIPAddress = null;
        isPrimary = true;
        kvStore = new HashMap<String, String>();
    } else {
        primaryIPAddress = mainip;
        isPrimary = false;
        kvStore = null;
    }
}

public static void
main(String[] args) throws Exception
{
    int port = 8080;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    System.err.println("Server running on port " + port + ".");
    Random rand = new Random();
    String[] nodes = {"localhost:8080"};
    server.createContext("/", new PaxosServer(rand.nextInt(5), nodes));
    server.setExecutor(null); // creates a default executor
    server.start();
}

private String primaryIPAddress;
private boolean isPrimary;
private HashMap<String, String> kvStore;

}

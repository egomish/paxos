import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ConnectException;
import java.io.InputStream;

import java.util.HashMap;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    if (path.startsWith("/hello")) {
        doHello(exch);
    } else if (path.startsWith("/test")) {
        doTest(exch);
    } else if (path.startsWith("/kvs")) {
        doKVS(exch);
    } else {
        String method = exch.getRequestMethod();
        sendResponse(exch, 404, "", method + " " + path + " not found");
    }
}

private static String parseKeyFromQuery (String query)
{
    if (query == null) {
        return null;
    }
    String[] pairs = query.split("&");
    for (int i = 0; i < pairs.length; i += 1) {
        String[] split = pairs[i].split("=");
        if (split[0].equals("key")) {
            return split[1];
        }
    }
    return null;
}

private static boolean keyIsValid (String key)
{
    if (key.length() == 0) {
        return false;
    }
    if (key.length() > 250) {
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

private void sendResponse (HttpExchange exch, 
                           int rescode, 
                           String resmsg, 
                           String restype)
{
    try {
        String method = exch.getRequestMethod();
        System.err.println("Responding to " + method + " with " + rescode);
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
    String urlkey = SmallServer.parseKeyFromQuery(uri.getQuery());

    //response fields
    int rescode = 200;
    String restype = "";
    String resmsg = "";

    if (method.equals("GET")) {
        String value = kvStore.get(urlkey);
        if (value == null) {
            rescode = 404;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setReplacedFlag(0);
            resbody.setMessageString("error");
            resbody.setErrorString("key does not exist");
            resmsg = resbody.toJSON();
        } else {
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setMessageString("success");
            resbody.setValueString(value);
            resmsg = resbody.toJSON();
        }
    } else if (method.equals("PUT")) {
        RequestBody reqbody = new RequestBody(exch.getRequestBody());
        if (!SmallServer.keyIsValid(reqbody.key())) {
            rescode = 400;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setMessageString("error");
            resbody.setErrorString("key is invalid");
            resmsg = resbody.toJSON();
        } else if (reqbody.value() == null) {
            rescode = 400;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setMessageString("error");
            resbody.setErrorString("no value given");
            resmsg = resbody.toJSON();
        } else if (kvStore.get(reqbody.key()) == null) {
            kvStore.put(reqbody.key(), reqbody.value());
            rescode = 201;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setReplacedFlag(0);
            resbody.setMessageString("success");
            resmsg = resbody.toJSON();
        } else {
            kvStore.put(reqbody.key(), reqbody.value());
            rescode = 200;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setReplacedFlag(1);
            resbody.setMessageString("success");
            resmsg = resbody.toJSON();
        }
    } else if (method.equals("DELETE")) {
        String value = kvStore.get(urlkey);
        if (value != null) {
            kvStore.remove(urlkey);
            rescode = 200;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setMessageString("success");
            resmsg = resbody.toJSON();
        } else {
            rescode = 404;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody();
            resbody.setMessageString("error");
            resbody.setErrorString("key does not exist");
            resmsg = resbody.toJSON();
        }
    } else {
        rescode = 405;
        resmsg = method + " " + uri + " not allowed";
    }
    sendResponse(exch, rescode, resmsg, restype);
}

private void sendMessageOnSubnet (int port, 
                                  String method, 
                                  String uri, 
                                  String msg)
{
    URL url = null;
    HttpURLConnection conn = null;
    InputStream in = null;

    //set URL
    try {
        url = new URL("http://localhost:" + port + uri);
    } catch (MalformedURLException e) {
        System.out.println("malformed url " + e.getMessage());
        return;
    }

    //open connection to server
    try {
        conn = (HttpURLConnection)url.openConnection();
    } catch (ConnectException e) {
        System.out.println("server at " + port + uri + " is down");
        System.out.println(e.getMessage());
        return;
    } catch (IOException e) {
        System.out.println("I/O exception while opening connection.");
        e.printStackTrace();
        return;
    }

    //send request to server
    try {
        conn.setRequestMethod(method);
        conn.connect();
    } catch (ConnectException e) {
        System.out.println("server at " + port + uri + " is down");
        System.out.println(e.getMessage());
        return;
    } catch (IOException e) {
        System.out.println("I/O exception while sending request.");
        e.printStackTrace();
        return;
    }

    //get response
    try {
        in = conn.getInputStream();
    } catch (IOException e) {
        System.out.println("I/O exception while getting response.");
        System.out.println(e.getMessage());
        //handle error based on rescode
        //int rescode = conn.getResponseCode();
        return;
    }

    //log response to STDOUT
    try {
        int b = in.read();
        while (b != -1) {
            System.out.print((char)b);
            b = in.read();
        }
        System.out.println();
    } catch (IOException e) {
        System.out.println("I/O exception while reading response.");
        e.printStackTrace();
        return;
    }
}

private void sendHelloWorld (int port)
{
    URL url = null;
    HttpURLConnection conn = null;
    try {
        url = new URL("http://localhost:" + port + "/hello");
        conn = (HttpURLConnection)url.openConnection();
        conn.connect();
        InputStream in = conn.getInputStream();
        int b = in.read();
        while (b != -1) {
            System.out.print((char)b);
            b = in.read();
        }
        System.out.println();
    } catch (MalformedURLException e) {
        System.out.println("bad url " + url);
        System.exit(7);
    } catch (IOException e) {
        System.out.println("could not open connection to " + port + "/hello");
        System.exit(7);
    }
}

private SmallServer()
{
    kvStore = new HashMap<String, String>();
}

public static void
main(String[] args) throws Exception
{
    int port = 8080;
    SmallServer ss = new SmallServer();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    System.err.println("Server running on port " + port + ".");
    server.createContext("/", ss);
    server.setExecutor(null); // creates a default executor
    server.start();
    ss.sendHelloWorld(port);
    ss.sendMessageOnSubnet(port, "POST", "/hello", "msg='BenderIsGreat'");
}

private HashMap<String, String> kvStore;

}

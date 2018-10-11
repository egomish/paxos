import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;


public class SmallServer implements HttpHandler
{

public void handle (HttpExchange exch) throws IOException
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();
    String resmsg = "";
    int rescode = 200;
    try {
        if (path.startsWith("/hello")) {
            resmsg = doHello(method, uri);
        } else if (path.startsWith("/test")) {
            resmsg = doTest(method, uri);
        } else if (path.startsWith("/kvs")) {
            resmsg = doKVS(method, uri);
        } else {
            rescode = 404;
            resmsg = method + " " + uri + ": " + rescode + " not found.";
        }
    } catch (MalformedURLException e) {
        rescode = 405;
        resmsg = method + " " + uri + ": " + rescode + " not allowed.";
    }
    try {
        System.err.println("Responding to " + method + " with " + rescode);
        exch.sendResponseHeaders(rescode, 0);
        OutputStream out = exch.getResponseBody();
        out.write(resmsg.getBytes());
        out.close();
    } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
    }
}

private String doHello (String method, URI uri) throws MalformedURLException
{
    String path = uri.getPath();
    String resmsg = "";
    if (method.equals("GET")) {
        resmsg = "Hello world!";
    } else {
        throw new MalformedURLException();
    }
    return resmsg;
}

//TODO: handle multiple queries for POST
//      (split on '&', then split on '=')
private String doTest (String method, URI uri) throws MalformedURLException
{
    String path = uri.getPath();
    String query = uri.getQuery();
    String resmsg = "";
    if (method.equals("GET")) {
        resmsg = "GET request received";
    } else if (method.equals("POST")) {
        String[] strarr = query.split("=");
        if (strarr[0].equals("msg")) {
            resmsg = "POST message received: " + strarr[1];
        }
    } else {
        throw new MalformedURLException();
    }
    return resmsg;
}

private String doKVS (String method, URI uri) throws MalformedURLException
{
    String path = uri.getPath();
    String query = uri.getQuery();
    String resmsg = "";
    if (method.equals("GET")) {
        resmsg = "GET for KVS!!";
    } else {
        throw new MalformedURLException();
    }
    return resmsg;
}

private SmallServer()
{
}

public static void
main(String[] args) throws Exception
{
    int port = 8080;
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    System.out.println("Server running on port " + port + ".");
    server.createContext("/", new SmallServer());
    server.setExecutor(null); // creates a default executor
    server.start();
}

}

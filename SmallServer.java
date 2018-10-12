import java.util.HashMap;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
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
    String body = readBody(exch.getRequestBody());
    String resmsg = "";
    int rescode = 200;
    try {
        if (path.startsWith("/hello")) {
            resmsg = doHello(method, uri);
        } else if (path.startsWith("/test")) {
            resmsg = doTest(method, uri);
        } else if (path.startsWith("/kvs")) {
            resmsg = doKVS(method, uri, body);
        } else {
            throw new URISyntaxException(path, "404");
        }
    } catch (URISyntaxException e) {
        rescode = Integer.parseInt(e.getReason());
        resmsg = e.getInput() + ": " + e.getReason() + " not found.";
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

private String readBody (InputStream reqin)
{
    String body = "";
    try {
        int b = reqin.read();
        while (b != -1) {
            body += b;
        }
    } catch (IOException e) {
        System.err.println("I/O exception occurred while reading request body.");
        return null;
    }
    System.out.println("Body: '" + body + "'");
    return body;
}

private String[][] parseQuery (String query)
{
    String[] pairs = query.split("&");
    String[][] splitpairs = new String[pairs.length][];
    for (int i = 0; i < pairs.length; i += 1) {
        splitpairs[i] = pairs[i].split("=");
    }
    return splitpairs;
}

private String doHello (String method, URI uri) throws URISyntaxException
{
    String path = uri.getPath();
    String resmsg = "";
    if (method.equals("GET")) {
        resmsg = "Hello world!";
    } else {
        throw new URISyntaxException(method + " " + path, "405");
    }
    return resmsg;
}

//TODO: handle multiple queries for POST
//      (split on '&', then split on '=')
private String doTest (String method, URI uri) throws URISyntaxException
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
        throw new URISyntaxException(method + " " + path, "405");
    }
    return resmsg;
}

/*
 *  URI syntax:
 *      GET:    key=<key>
 *      PUT:    key=<key>&value=<value>
 *      DELETE: key=<key>
 *  We require that the queries are always ordered key[, value]
 */
private String doKVS (String method, URI uri, String body)
               throws URISyntaxException
{
    String path = uri.getPath();
    String[][] query = parseQuery(uri.getQuery());
    String resmsg = "";
    if (query.length == 0) {
        throw new URISyntaxException(method + " " + path, "400");
    }
    if (method.equals("GET")) {
        if (!query[0][0].equals("key")) {
            throw new URISyntaxException(method + " " + path, "400");
        }
        byte[] value = kvStore.get(query[0][1]);
        if (value == null) {
            throw new URISyntaxException(method + " " + path, "404");
        }
        ResponseBody resbody = new ResponseBody(1, "success", null);
        resmsg = resbody.toJSON();
        System.out.println("resmsg: " + resmsg);
    } else {
        throw new URISyntaxException(method + " " + path, "405");
    }
    return resmsg;
}

private SmallServer()
{
    kvStore = new HashMap<String, byte[]>();
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

private HashMap<String, byte[]> kvStore;

}

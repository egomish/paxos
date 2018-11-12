import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextTest extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    System.err.println("[" + this.getClass().getName() + "] " + 
                       "Handling " + exch.getRequestMethod() + " request...");
    if (!isPrimary) {
        HttpResponse response = forwardRequestToPrimary(exch);
        sendResponse(exch, response.getResponseCode(), response.getResponseBody(), null);
        return;
    }

    String path = exch.getRequestURI().getPath();
    if (!path.startsWith("/test")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    doTest(exch);
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


}

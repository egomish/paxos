import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextHello extends BaseContext implements HttpHandler
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
    if (!path.startsWith("/hello")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    doHello(exch);
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


}

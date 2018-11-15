import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextHistory extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this + " Request: " + exch.getRequestMethod() + " " + path);
    if (!path.startsWith("/history")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    doHistory(exch);
}

private void doHistory (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();
    int rescode = 200;
    String resmsg = "";
    if (method.equals("GET")) {
        System.out.println(BaseContext.requestHistory.toString());
        resmsg = BaseContext.requestHistory.toString();
    } else {
        rescode = 405;
        resmsg = method + " " + path + " not allowed";
    }
    sendResponse(exch, rescode, resmsg, null);
}


}

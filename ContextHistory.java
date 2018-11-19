import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextHistory extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));
    doHistory(exch);
}


private void doHistory (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();

    int rescode;
    String resmsg = "";
    if (method.equals("GET")) {
        rescode = 200;
        resmsg = this.reqHistory.toJSON();
    } else {
        rescode = 405;
        resmsg = method + " " + path + " not allowed";
    }
    sendResponse(exch, rescode, resmsg);
}


}

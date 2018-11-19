import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextHello extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    if (!isPrimary) {
        POJOResHttp response = forwardToPrimary(exch);
        sendResponse(exch, response);
        return;
    }

    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));

    doHello(exch);
}

private void doHello (HttpExchange exch)
{
    String method = exch.getRequestMethod();
    URI uri = exch.getRequestURI();
    String path = uri.getPath();

    int rescode = 200;
    String resmsg;

    if (method.equals("GET")) {
        resmsg = new POJOResBody(true, "Hello world!").toJSON();
    } else {
        rescode = 405;
        String info = method + " " + path + " not allowed";
        resmsg = new POJOResBody(false, info).toJSON();
    }
    sendResponse(exch, rescode, resmsg);
}


}

import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;


public abstract class BaseContext {

public String[] getNodeView ()
{
    return nodeView;
}

protected HttpResponse forwardRequestToPrimary (HttpExchange exch)
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
protected void sendResponse (HttpExchange exch,
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

protected BaseContext()
{
    String mainip = System.getenv().get("MAINIP");
    if (mainip == null) {
        primaryIPAddress = null;
        isPrimary = true;
    } else {
        primaryIPAddress = mainip;
        isPrimary = false;
    }
}

protected String[] nodeView;
protected String primaryIPAddress;
protected boolean isPrimary;

}

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
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    Client cl = new Client(primaryIPAddress, method, path, reqbody);
    cl.doSync();
    HttpResponse response = new HttpResponse();
    response.setResponseCode(cl.getResponseCode());
    response.setResponseBody(cl.getResponseBody());
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

    String nodes = System.getenv().get("VIEW");
    if (nodes == null) {
        nodeView = new String[2];
        nodeView[0] = "localhost:4001";
        nodeView[1] = "localhost:4002";
    } else {
        nodeView = nodes.split(",");
        for (int i = 0; i < nodeView.length; i += 1) {
            //XXX: this seems like a really dumb hack
            //add port number to known IP addresses
            nodeView[i] += ":8080";
        }
    }

    String pid = System.getenv().get("PID");
    if (pid == null) {
        processID = 0;
    } else {
        try {
            processID = Integer.parseInt(pid);
        } catch (NumberFormatException e) {
            System.err.println("bad pid " + pid);
            processID = 0;
        }
    }
}

protected String[] nodeView;
protected int processID;
protected String primaryIPAddress;
protected boolean isPrimary;

}

import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosAcceptor extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    System.err.println("[" + this.getClass().getName() + "] " + 
                       "Handling " + exch.getRequestMethod() + " request...");

    String path = exch.getRequestURI().getPath();
    if (!path.startsWith("/paxos/acceptor")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    } else {
        String subpath = path.substring(15); //subtract "/paxos/acceptor"
        if (subpath.startsWith("/prepare")) {
            doPaxosAcceptorPrepare(exch);
        } else if (subpath.startsWith("/accept")) {
            doPaxosAcceptorAccept(exch);
        } else {
            int rescode = 404;
            String restype = "application/json";
            String resmsg = ResponseBody.clientError().toJSON();
            sendResponse(exch, rescode, resmsg, restype);
            return;
        }
    }
}

private void doPaxosAcceptorPrepare (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    PaxosProposal received = PaxosProposal.fromJSON(reqbody);
    System.out.println("received: " + reqbody);

    int rescode;
    String restype;
    String resmsg;

    int seqnum = received.getSequenceNumber();
    if (seqnum > theProposal.getSequenceNumber()) {
        theProposal.setSequenceNumber(seqnum);
        System.out.println("ACK(" + theProposal.toString() + ").");
        rescode = 200;
        restype = "application/json";
        resmsg = theProposal.toJSON();
    } else {
        //the received proposal is old--refuse it
        System.out.println("NAK(" + theProposal.getSequenceNumber() + ").");
        rescode = 409;
        restype = "application/json";
        resmsg = ResponseBody.clientError().toJSON();
    }
    sendResponse(exch, rescode, resmsg, restype);
}

private void doPaxosAcceptorAccept (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    PaxosProposal received = PaxosProposal.fromJSON(reqbody);

    int rescode;
    String restype;
    String resmsg;

    if (theProposal.getSequenceNumber() >= received.getSequenceNumber()) {
        theProposal = received;
    }
    rescode = 200;
    restype = "application/json";
    resmsg = theProposal.toJSON(); //XXX: only seqnum is needed
    System.out.println("ACK(" + theProposal.toString() + ").");
    sendResponse(exch, rescode, resmsg, restype);
}

protected ContextPaxosAcceptor ()
{
    theProposal = new PaxosProposal();
}


private PaxosProposal theProposal;

}

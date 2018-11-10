import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosAcceptor extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    System.err.println("[PaxosAcceptor] Handling " + exch.getRequestMethod() + " request...");
    if (!isPrimary) {
        HttpResponse response = forwardRequestToPrimary(exch);
        sendResponse(exch, response.getResponseCode(), response.getResponseBody(), null);
        return;
    }

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
    String reqbody = ClientRequest.inputStreamToString(exch.getRequestBody());
    PaxosProposal received = PaxosProposal.fromJSON(reqbody);

    int rescode;
    String restype;
    String resmsg;

    int seqnum = received.getSequenceNumber();
    if (seqnum > theProposal.getSequenceNumber()) {
        theProposal.setSequenceNumber(seqnum);
        rescode = 200;
        restype = "application/json";
        resmsg = theProposal.toJSON();
        sendResponse(exch, rescode, resmsg, restype);
    } else {
        //the received proposal is old--ignore it
    }
}

private void doPaxosAcceptorAccept (HttpExchange exch)
{
    String reqbody = ClientRequest.inputStreamToString(exch.getRequestBody());
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
    sendResponse(exch, rescode, resmsg, restype);
}

protected ContextPaxosAcceptor ()
{
    theProposal = new PaxosProposal();
}


private PaxosProposal theProposal;

}

import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosAcceptor extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this + " Request: " + exch.getRequestMethod() + " " + path);
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
        } else if (subpath.startsWith("/commit")) {
            doPaxosAcceptorCommit(exch);
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

    int rescode;
    String restype;
    String resmsg;

    int seqnum = received.getSequenceNumber();
    if (seqnum > theProposal.getSequenceNumber()) {
        theProposal.setSequenceNumber(seqnum);
        System.out.println(seqnum + " -> ACK(" + theProposal.toString() + ").");
        rescode = 200;
        restype = "application/json";
        resmsg = theProposal.toJSON();
    } else {
        //the received proposal is old--refuse it
        System.out.println(seqnum + " -> NAK(" + theProposal.getSequenceNumber() + ").");
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

    int seqnum = received.getSequenceNumber();
    if (seqnum >= theProposal.getSequenceNumber()) {
        //take the offered proposal because it's later (or the same)
        theProposal = received;
        System.out.println(received.getSequenceNumber() + 
                           " -> ACK(" + theProposal.toString() + ").");
    } else {
        //discard the offered proposal because it's outdated
        System.out.println(received.getSequenceNumber() + 
                           " -> NAK(" + theProposal.toString() + ").");
    }
    rescode = 200;
    restype = "application/json";
    resmsg = theProposal.toJSON(); //XXX: only seqnum is needed
    sendResponse(exch, rescode, resmsg, restype);
}

private void doPaxosAcceptorCommit (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    PaxosProposal received = PaxosProposal.fromJSON(reqbody);
    String tocommit = received.getAcceptedValue();

    //store tocommit in requestHistory
    int seqnum = received.getSequenceNumber();
    String value = received.getAcceptedValue();
    BaseContext.requestHistory.put(seqnum, value);
    System.out.println("put " + BaseContext.requestHistory.get(seqnum));
    System.out.println(BaseContext.requestHistory.toString());

    //parse tocommit into a API call
    POJORequest request = POJORequest.fromJSON(value);
    //XXX: clients damn well better not set ip address in their proposals
    request.setDestIP(this.ipAndPort);

    //reset the proposal value so that new proposals can get accepted
    //XXX: this resets the seqnum and the value, not just the value
    theProposal = new PaxosProposal();

    //execute the API call
    Client cl = new Client(request);
    cl.doSync();

    int rescode = cl.getResponseCode();
    String restype = "application/json";
    String resmsg = cl.getResponseBody();
    System.out.println(received.getSequenceNumber() + 
                       " -> ACK(" + tocommit + ")");
    sendResponse(exch, rescode, resmsg, restype);
}

protected ContextPaxosAcceptor ()
{
    theProposal = new PaxosProposal();
}


private PaxosProposal theProposal;

}

import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosProposer extends BaseContext implements HttpHandler
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
    if (!path.startsWith("/paxos/proposer")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    doPaxosProposer(exch);
}

private void doPaxosProposer (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());

    int rescode;
    String resmsg;
    String restype;

    //increment seqnum
    this.updateSequenceNumber(this.getSequenceNumber());
    theProposal.setSequenceNumber(this.getSequenceNumber());

    System.out.println("sending prepare...");
    //prepare
    String value = sendPrepare();
    if (value != null) {
        theProposal.setAcceptedValue(value);
    } else {
        //set the value in theProposal to incoming argument
        theProposal.setAcceptedValue(reqbody);
    }

    System.out.println("sending accept...");
    //accept
    int latestseqnum = sendAccept();

    if (latestseqnum == this.getSequenceNumber()) {
        System.out.println("sending commit...");
        //commit
        boolean success = sendCommit();
        if (success) {
            rescode = 200;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody(true, "success is not implemented");
            resmsg = resbody.toJSON();
        } else {
            //TODO: rebroadcast instead of failing out
            rescode = 522;
            restype = "application/json";
            ResponseBody resbody = new ResponseBody(false, "broadcast failed, not rebroadcasting");
            resmsg = resbody.toJSON();
        }
    } else {
        this.updateSequenceNumber(latestseqnum);
        //XXX: this drops the proposal on the floor
        rescode = 501;
        restype = "application/json";
        ResponseBody resbody = new ResponseBody(false, "commit was refused");
        resmsg = resbody.toJSON();
    }

    sendResponse(exch, rescode, resmsg, restype);
}

//XXX: prepare messages are sent serially
private String sendPrepare ()
{
    String value = null;
    String reqbody;
    reqbody = theProposal.toJSON();

    System.out.println("preparing: " + reqbody);
    Client[] multi = Client.readyMulticast(this.getNodeView(), "POST", "/paxos/acceptor/prepare", reqbody);
    for (Client cl : multi) {
        cl.sendAsync();
        cl.receiveAsync();
        PaxosProposal resprop = PaxosProposal.fromJSON(cl.getResponseBody());
        if (resprop.getAcceptedValue() != null) {
            value = resprop.getAcceptedValue();
        }
    }
    return value;
}

private int sendAccept ()
{
    String reqbody = theProposal.toJSON();
    Client[] multi = Client.readyMulticast(this.getNodeView(), "POST", "/paxos/acceptor/accept", reqbody);
    int latestseqnum = 0;
    for (Client cl : multi) {
        cl.sendAsync();
        cl.receiveAsync();
        PaxosProposal resprop = PaxosProposal.fromJSON(cl.getResponseBody());
        int seqnum = resprop.getSequenceNumber();
        if (seqnum > latestseqnum) {
            latestseqnum = seqnum;
        }
    }
    return latestseqnum;
}

//TODO: use reliable broadcast
private boolean sendCommit ()
{
    String reqbody = theProposal.toJSON();
    Client[] multi = Client.readyMulticast(this.getNodeView(), "POST", "/paxos/commit", reqbody);
    for (Client cl : multi) {
        cl.sendAsync();
        cl.receiveAsync();
        int rescode = cl.getResponseCode();
        if (rescode != 200) {
            return false;
        }
    }
    return true;
}

private int getSequenceNumber ()
{
    return (monotonicNumber * 100) + this.processID;
}

private void updateSequenceNumber (int seqnum)
{
    monotonicNumber = ((seqnum / 100) + 1);
}

protected ContextPaxosProposer ()
{
    monotonicNumber = 0;
    theProposal = new PaxosProposal();
}


private int monotonicNumber;
private PaxosProposal theProposal;

}

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.ArrayList;


public class ContextPaxosProposer extends BaseContext implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    if (!isPrimary) {
        HttpResponse response = forwardRequestToPrimary(exch);
        sendResponse(exch, response.getResponseCode(), response.getResponseBody(), null);
        return;
    }

    String path = exch.getRequestURI().getPath();
    System.err.println(this + " Request: " + exch.getRequestMethod() + " " + path);
    if (!path.startsWith("/paxos/proposer")) {
        int rescode = 404;
        String restype = "application/json";
        String resmsg = ResponseBody.clientError().toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    //HERE
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    doPaxosProposer(exch);
}

private void doPaxosProposer (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());

    //prepare
    this.incrementSequenceNumber();
    theProposal.setSequenceNumber(this.getSequenceNumber());

    String value;
    try {
        value = sendPrepare();
    } catch (TimeoutException e) {
        bufferProposal(reqbody);
        int rescode = 501;
        String restype = "application/json";
        ResponseBody resbody = new ResponseBody(false, "prepare failed");
        String resmsg = resbody.toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    if (value == null) {
        //set the value in theProposal to incoming argument
        theProposal.setAcceptedValue(reqbody);
    } else {
        //set the value in theProposal to what was already accepted
        bufferProposal(reqbody);
        theProposal.setAcceptedValue(value);
    }
    System.out.println("agreed on: '" + theProposal.getAcceptedValue() + "'");

    //accept
    int latestseqnum = sendAccept();

    if (latestseqnum != this.getSequenceNumber()) {
        int rescode = 501;
        String restype = "application/json";
        ResponseBody resbody = new ResponseBody(false, "accept failed");
        String resmsg = resbody.toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }

    //commit
    boolean success = sendCommit();
    if (!success) {
        //TODO: rebroadcast instead of failing out
        int rescode = 522;
        String restype = "application/json";
        ResponseBody resbody = new ResponseBody(false, "broadcast failed, not rebroadcasting");
        String resmsg = resbody.toJSON();
        sendResponse(exch, rescode, resmsg, restype);
        return;
    }
    int rescode = 200;
    String restype = "application/json";
    ResponseBody resbody = new ResponseBody(true, "success is not implemented");
    String resmsg = resbody.toJSON();

    if (requestBuffer.size() == 0) {
        sendResponse(exch, rescode, resmsg, restype);
    }
    //XXX: HERE
    unbufferNextMessage();
}

private String sendPrepare () throws TimeoutException
{
    String value = null;
    String reqbody;
    reqbody = theProposal.toJSON();

    System.out.println("preparing: " + reqbody);

    //broadcast prepare to cluster
    Client[] multi = Client.readyMulticast(this.getNodeView(), 
                                           "POST", "/paxos/acceptor/prepare", 
                                           reqbody);
    for (Client cl : multi) {
        cl.fireAsync();
    }

    //wait until a majority has responded
    while (!Client.doneMajority(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    //process the responses
    int votenum = 0;
    for (Client cl : multi) {
        if (cl.done()) {
            PaxosProposal result = PaxosProposal.fromJSON(cl.getResponseBody());
            if (result.getAcceptedValue() == null) {
                votenum += 1;
            } else {
                value = result.getAcceptedValue();
                //there's already an accepted value--just propagate that
                break;
            }
        }
    }
    if (votenum < (multi.length / 2 + 1)) {
        //too many servers are down or not responding--fail the proposal
        //XXX: this isn't quite right--
        //     we would be better off waiting until we get all of them, 
        //     and stopping early if we get a majority of ACKs or NAKs
        throw new TimeoutException();
    }
    return value;
}

private int sendAccept ()
{
    String reqbody = theProposal.toJSON();
    Client[] multi = Client.readyMulticast(this.getNodeView(), "POST", "/paxos/acceptor/accept", reqbody);
    int latestseqnum = 0;
    for (Client cl : multi) {
        cl.fireAsync();
    }
    while (!Client.doneMajority(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }
    for (Client cl : multi) {
        int seqnum = 0;
        if (cl.done()) {
            PaxosProposal result = PaxosProposal.fromJSON(cl.getResponseBody());
            seqnum = result.getSequenceNumber();
        }
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
    Client[] multi = Client.readyMulticast(this.getNodeView(), "POST", "/paxos/acceptor/commit", reqbody);
    for (Client cl : multi) {
        //TODO: replace with fireAsync
        cl.doSync();
        int rescode = cl.getResponseCode();
        if (rescode != 200) {
            return false;
        }
    }
    return true;
}

private void bufferProposal (String reqbody)
{
    requestBuffer.addLast(reqbody);
}

private String unbufferNextMessage ()
{
    POJORequest request = new POJORequest(this.ipAddress, 
                                          "POST", "paxos/proposer", 
                                          requestBuffer.removeFirst());
    Client cl = new Client(request);
    cl.doSync();
    if (cl.getResponseCode() != 200) {
        //TODO: figure out what to do here
        System.out.println(cl.getResponseBody());
    }
    return cl.getResponseBody();
}

private void doFailProposal (HttpExchange exch)
{
    int rescode = 507;
    String restype = "application/json";
    ResponseBody resbody = new ResponseBody(false, "proposal refused");
    String resmsg = resbody.toJSON();
    sendResponse(exch, rescode, resmsg, restype);
}

//XXX: this magic number 100 is the max number of processes in the paxos cluster
private int getSequenceNumber ()
{
    return (monotonicNumber * 100) + this.processID;
}

private void incrementSequenceNumber ()
{
    monotonicNumber += 1;
}

protected ContextPaxosProposer ()
{
    requestBuffer = new ArrayList<String>();
    monotonicNumber = 0;
    theProposal = new PaxosProposal();
}


private ArrayList<String> requestBuffer;
private int monotonicNumber;
private PaxosProposal theProposal;

}

import java.io.IOException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;


public class ContextPaxosAcceptor extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));

try {
    if (path.startsWith("/paxos/acceptor/prepare")) {
        doPaxosAcceptorPrepare(exch);
    } else if (path.startsWith("/paxos/acceptor/accept")) {
        doPaxosAcceptorAccept(exch);
    } else if (path.startsWith("/paxos/acceptor/commit")) {
        doPaxosAcceptorCommit(exch);
    } else {
        sendResponse(exch, 404, POJOResBody.clientError().toJSON());
        return;
    }
} catch (Exception e) {
    e.printStackTrace();
    System.exit(8);
}
}

private void doPaxosAcceptorPrepare (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOPaxosBody recv = POJOPaxosBody.fromJSON(reqbody);

    POJOResHttp response;

    if (recv.seqNum > this.seqNum) {
        //promise to agree and send currently accepted value
        this.seqNum = recv.seqNum;
        printLog(recv.seqNum + " -> ACK(prepare)");
        String info = this.accValue;
        POJOResBody resbody = new POJOResBody(true, info);
        response = new POJOResHttp(200, resbody.toJSON());
    } else {
        //the recv proposal is old--refuse it
        //but also send the request for that index so the tardy node catches up
        printLog(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        String info = this.getHistoryAt(recv.reqIndex);
        POJOResBody resbody = new POJOResBody(false, info);
        response = new POJOResHttp(409, resbody.toJSON());
    }
    sendResponse(exch, response);
}

/*
 *  We deviate from Paxos here in that, if the proposal is old, we return 
 *  the committed request at the proposed index instead of the latest 
 *  proposal number. This is to help the tardy node catch up its history 
 *  before it commits any more requests.
 */
private void doPaxosAcceptorAccept (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOPaxosBody recv = POJOPaxosBody.fromJSON(reqbody);

    POJOResHttp response;

    if (recv.seqNum >= this.seqNum) {
        //take the offered proposal because it's later (or the same)
        this.seqNum = recv.seqNum;
        this.accValue = recv.accValue;
        printLog(recv.seqNum + " -> ACK(accept)");
        String info = Integer.toString(recv.seqNum);
        POJOResBody resbody = new POJOResBody(true, info);
        response = new POJOResHttp(200, resbody.toJSON());
    } else {
        //discard the offered proposal because it's outdated
        //but return history[reqindex] so the tardy node can catch up
        printLog(recv.seqNum + " -> NAK(" + this.toString() + ").");
        String info = this.getHistoryAt(recv.reqIndex);
        POJOResBody resbody = new POJOResBody(false, info);
        response = new POJOResHttp(409, resbody.toJSON());
    }
    sendResponse(exch, response);
}

private void doPaxosAcceptorCommit (HttpExchange exch)
{
    String reqbody = Client.fromInputStream(exch.getRequestBody());
    POJOPaxosBody recv = POJOPaxosBody.fromJSON(reqbody);

    //parse the value into an API call
    //XXX: if the client set the ip, it'll be clobbered
    //     (but why would the client ever set the ip?)
    POJOReqHttp request = POJOReqHttp.fromJSON(recv.accValue);
    request.ip = this.ipAndPort;

    //add the request to the history
    this.addToHistoryAt(recv.reqIndex, recv.accValue);

    //reset the proposal value so that new proposals can get accepted
    this.accValue = null;

    printLog(recv.seqNum + " -> ACK(committed history[" + recv.reqIndex + "])");
    POJOResBody resbody = new POJOResBody(true, Integer.toString(recv.reqIndex));
    //TODO: propagate commit (reliable broadcast)
    printLog("Reliable broadcast for commit not implemented.");

    sendResponse(exch, new POJOResHttp(200, resbody.toJSON()));
}

public String toString ()
{
    String str = "";
    str += seqNum;
    str += ": ";
    if (accValue == null) {
        str += "null";
    } else {
        str += accValue;
    }
    return str;
}

protected ContextPaxosAcceptor ()
{
    seqNum = 0;
    accValue = null;
}


private int seqNum;
private String accValue;

}

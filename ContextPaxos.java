import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.ArrayList;

public class ContextPaxos extends SmallServer implements HttpHandler {


public void handle (HttpExchange exch)
{
    this.logReceive(exch.getRequestMethod(), exch.getRequestURI().getPath());

    String method = exch.getRequestMethod();
    String path = exch.getRequestURI().getPath();

    HttpRes response;

    if (path.startsWith("/paxos/propose")) {
        response = doPaxosPropose(exch);
    } else if (path.startsWith("/paxos/prepare")) {
        response = doPaxosPrepare(exch);
    } else if (path.startsWith("/paxos/accept")) {
        response = doPaxosAccept(exch);
    } else if (path.startsWith("/paxos/commit")) {
        response = doPaxosCommit(exch);
    } else {
        String resmsg = method + " " + path + " not allowed";
        response = new HttpRes(405, resmsg);
        response.contentType = null;
    }

    sendResponse(exch, response);
}

private HttpRes doPaxosPropose (HttpExchange exch)
{
    //extract the KVS request from the HTTP request and add it to the queue
    String reqstr = Client.fromInputStream(exch.getRequestBody());
    POJOReq request = POJOReq.fromJSON(reqstr);

    HttpRes paxosres = null;

    //do some paxos!
    while (request != null) {
        int seqnum = this.nextProposalNumber();
        int reqindex = this.getNextHistoryIndex();
        POJOPaxos prop = new POJOPaxos(seqnum, reqindex, null);

        //prepare
        int didprepare = 0;
        POJOReq value = null;
        for (String node : this.getNodeView()) {
            POJOReq req = new POJOReq(node, "POST", "/paxos/prepare", prop.toJSON());
            Client cl = new Client(req);
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode == 200) {
                //this proposal is recent enough
                didprepare += 1;
                POJOPaxos prepres = POJOPaxos.fromJSON(res.resBody);
                if (prepres.accValue != null) {
                    value = prepres.accValue;
                }
            } else if (res.resCode == 408) {
                //this proposal is too old--catch up history and try again
                System.out.println("catch up on prepare not implemented!!");
            } else {
                //something went wrong--drop the response on the floor
                System.out.println("something went wrong");
            }
        }
        if (didprepare < this.getQuorumSize()) {
            continue;
        }

        //accept
        //----| begin transaction |----
        prop.accValue = value;
        if (value == null) {
            prop.accValue = request;
        }
        //----|  end transaction  |----
        int didaccept = 0;
        for (String node : this.getNodeView()) {
            POJOReq req = new POJOReq(node, "POST", "/paxos/accept", prop.toJSON());
            Client cl = new Client(req);
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode == 200) {
                //this proposal was accepted
                didaccept += 1;
            } else if (res.resCode == 408) {
                //this proposal is too old--catch up history and try again
                System.out.println("catch up on accept not implemented!!");
            } else {
                //something went wrong--drop the response on the floor
                System.out.println("something went wrong");
            }
        }
        if (didaccept < this.getQuorumSize()) {
            continue;
        }

        //commit
        boolean success = true;
        for (String node : this.getNodeView()) {
            POJOReq req = new POJOReq(node, "POST", "/paxos/commit", prop.toJSON());
            Client cl = new Client(req);
            cl.doSync();
            HttpRes res = cl.getResponse();
            if (res.resCode != 200) {
                System.out.println("failed to commit on " + node);
                success = false;
            }
        }
        if (success) {
            request = null;
            paxosres = new HttpRes(200, Integer.toString(prop.reqIndex));
            paxosres.contentType = null;
        } else {
            request = null;
            paxosres = new HttpRes(513, "commit failed");
            paxosres.contentType = null;
        }
        break;
    }

    //return the result of the consensus
    return paxosres;
}

private HttpRes doPaxosPrepare (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    if (recv.seqNum > this.seqNum) {
        //promise to agree and respond with accepted value
        this.seqNum = recv.seqNum;
        this.log(recv.seqNum + " -> ACK(prepare).");
        response = new HttpRes(200, this.accValue.toJSON());
    } else {
        //the proposal is old--refuse it
        this.log(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        response = new HttpRes(408, "history not implemented");
        response.contentType = null;
    }

    return response;
}

private HttpRes doPaxosAccept (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    if (recv.seqNum >= this.seqNum) {
        //accept the proposal
        //----| begin transaction |----
        this.seqNum = recv.seqNum;
        this.accValue = recv.accValue;
        //----|  end transaction  |----
        response = new HttpRes(200, "accepted");
        response.contentType = null;
    } else {
        //discard the proposal because it's outdated
        //but return the latest seqnum so the tardy node can participate later
        this.log(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        response = new HttpRes(408, Integer.toString(this.seqNum));
        response.contentType = null;
    }

    return response;
}

private HttpRes doPaxosCommit (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    //parse the proposal value into an API call
    POJOReq request = recv.accValue;

    //add the request to history
    this.addToHistoryAt(recv.reqIndex, request);

    //propagate the commit (reliable broadcast)
    this.log("(reliable broadcast not implemented)");

    //reset the proposal value to new proposals can get accepted
    this.accValue = new POJOReq();

    this.log(recv.seqNum + " -> ACK(committed [" + recv.reqIndex + "]).");
    response = new HttpRes(200, "committed");
    response.contentType = null;

    return response;
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
protected int nextProposalNumber ()
{
    this.monotonicNumber += 1;
    return (this.monotonicNumber * 100) + this.processID;
}

protected ContextPaxos ()
{
    monotonicNumber = 0;
    seqNum = 0;
    accValue = new POJOReq();
}


private int monotonicNumber;
private int seqNum;
private POJOReq accValue;

}

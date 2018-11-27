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
    //extract the KVS request from the HTTP request
    String reqstr = Client.fromInputStream(exch.getRequestBody());
    POJOReq request = POJOReq.fromJSON(reqstr);

    HttpRes res = null;
    int finali = -1;
    while (finali == -1) {
        System.out.println("haven't added proposal to history");
        int seqnum = nextProposalNumber();
        int reqindex = this.getNextHistoryIndex();
        finali = doProposal(seqnum, reqindex, request);
    }
    System.out.println("successfully added proposal to history at " + finali);
    HttpRes response = new HttpRes(200, Integer.toString(finali));
    return response;
}

private int doProposal (int seqnum, int reqindex, POJOReq request)
{
    POJOPaxos prop = new POJOPaxos(seqnum, reqindex, null);

    //prepare
    POJOReq value = null;
    int didprepare = 0;
    for (String node : this.getNodeView()) {
        POJOReq req = new POJOReq(node, "POST", "/paxos/prepare", prop.toJSON());
        Client cl = new Client(req);
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode == 200) {
            //the proposal is recent
            didprepare += 1;
            //TODO: choose latest accValue, not last
            POJOPaxos prepres = POJOPaxos.fromJSON(res.resBody);
            if (!prepres.accValue.isEmpty()) {
                value = prepres.accValue;
            }
        } else if (res.resCode == 408) {
            //the acceptor thinks this proposal is old--drop response on floor
/*
            //this proposal is old--catch up if there's new history
            POJOPaxos histreq = POJOPaxos.fromJSON(res.resBody);
            if (!histreq.accValue.isEmpty()) {
                //there is new history--propagate that first
                int s = nextProposalNumber(histreq.seqNum);
                int i = reqindex;
                POJOReq r = histreq.accValue;
                //TODO: return request so that it eventually gets committed
                int innerres = doProposal(s, i, r);
                //XXX: what if innerres is -1?
                return -1; //request isn't in history yet
            }
*/
        } else {
            //something went wrong--drop the response on the floor
        }
    }
    if (didprepare < this.getQuorumSize()) {
        //failed to reach majority on prepare--try again
        int s = nextProposalNumber();
        int i = reqindex;
        POJOReq r = request;
        return doProposal(s, i, r);
    }

    //accept
    prop.accValue = value;
    if (value == null) {
        prop.accValue = request;
    } else {
        //the request isn't getting into history with this proposal
        //finali = -1
    }
    int didaccept = 0;
    for (String node : this.getNodeView()) {
        POJOReq req = new POJOReq(node, "POST", "/paxos/accept", prop.toJSON());
        Client cl = new Client(req);
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode == 200) {
            didaccept += 1;
            //Lamport's paxos would have us verify the seqnum, but 200 means OK
        } else if (res.resCode == 408) {
            //this proposal is old--catch up if there's new history
            POJOPaxos histreq = POJOPaxos.fromJSON(res.resBody);
            if (!histreq.accValue.isEmpty()) {
                //there is new history--propagate that first
                int s = nextProposalNumber(histreq.seqNum);
                int i = reqindex;
                POJOReq r = histreq.accValue;
                int innerres = doProposal(s, i, r);
                //XXX: what if innerres is -1?
                return -1; //request isn't in history yet
            }
        } else {
            //something went wrong--drop the response on the floor
        }
    }
    //XXX: not majority--all!!!
    if (didaccept < this.getQuorumSize()) {
        //failed to reach majority on prepare--try again
        int s = nextProposalNumber();
        int i = reqindex;
        POJOReq r = request;
        return doProposal(s, i, r);
    }
    /*
     *  Consensus! Tell the cluster so they can get consensus on something else.
     */

    //commit
    boolean success = true;
    for (String node : this.getNodeView()) {
        POJOReq req = new POJOReq(node, "POST", "/paxos/commit", prop.toJSON());
        Client cl = new Client(req);
        cl.doSync();
        HttpRes res = cl.getResponse();
        if (res.resCode != 200) {
            success = false;
        }
    }

    int rescode;
    if (success) {
        //all the nodes in this node's view committed the request
        rescode = 200;
    } else {
        //there's a network partition, so some nodes didn't get the commit
        rescode = 202;
    }
    POJOPaxos resbody = new POJOPaxos(seqnum, reqindex, null);
    HttpRes response = new HttpRes(rescode, resbody.toJSON());
    //XXX: we discard information about the network partition
    return reqindex; //request is in history at reqindex
}

private HttpRes doPaxosPrepare (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    //get the request from history if it's already there, or 
    //                from accValue if a proposal is in-progress
    //XXX: What if recv.reqIndex != this.reqIndex?
    POJOReq hist = this.getHistoryAt(recv.reqIndex);
    if (hist == null) {
        hist = this.accValue;
    }

    if (recv.seqNum > this.seqNum) {
        //promise to agree and respond with accepted value at reqindex
        this.log(recv.seqNum + " -> ACK(prepare [" + recv.reqIndex + "]).");
        this.seqNum = recv.seqNum;
        POJOPaxos resbody = new POJOPaxos(recv.seqNum, recv.reqIndex, hist);
        response = new HttpRes(200, resbody.toJSON());
    } else {
        //the proposal is old--refuse it, but help the tardy node catch up
        //XXX: we might be propagating too early here and hit duelling proposers
        this.log(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        POJOPaxos resbody = new POJOPaxos(this.seqNum, recv.reqIndex, hist);
        response = new HttpRes(408, resbody.toJSON());
    }

    return response;
}

private HttpRes doPaxosAccept (HttpExchange exch)
{
    String recvstr = Client.fromInputStream(exch.getRequestBody());
    POJOPaxos recv = POJOPaxos.fromJSON(recvstr);

    HttpRes response;

    if (recv.seqNum >= this.seqNum) {
        //the proposal is recent--accept it
        this.log(recv.seqNum + " -> ACK(accept [" + recv.reqIndex + "]).");
        //----| begin transaction |----
        this.seqNum = recv.seqNum;
        this.accValue = recv.accValue;
        //----|  end transaction  |----
        //respond with this.seqNum, recv.reqIndex, and this.accValue
        response = new HttpRes(200, recv.toJSON());
    } else {
        //the proposal is old--refuse it
        //but send the already-committed request so the tardy node catches up
        this.log(recv.seqNum + " -> NAK(" + this.seqNum + ").");
        //respond with history[recv.reqIndex], or 
        //             request currently being proposed for recv.reqIndex, or 
        //             null if not request is currently being proposed
        POJOReq hist = this.getHistoryAt(recv.reqIndex);
        if (hist == null) {
            hist = this.accValue;
        }
        POJOPaxos resbody = new POJOPaxos(this.seqNum, recv.reqIndex, hist);
        response = new HttpRes(408, resbody.toJSON());
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
    System.out.println("history before: " + reqHistory.size());
    this.addToHistoryAt(recv.reqIndex, request);
    System.out.println("history after: " + reqHistory.size());

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

//XXX: the magic number 100 is the max number of processes in the paxos cluster
protected int nextProposalNumber (int propnum)
{
    this.monotonicNumber = propnum / 100;
    return nextProposalNumber();
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

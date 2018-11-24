import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.net.URI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.util.LinkedList;


public class ContextPaxosProposer extends SmallServer implements HttpHandler
{


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    System.err.println(this.receiveLog(exch.getRequestMethod(), path));

    //extract the KVS request from the HTTP request and add it to the queue
    String request = Client.fromInputStream(exch.getRequestBody());
    this.queueProposal(request);

    PaxosProposal proposal = null;

    //get consensus about everything that's happened up to and 
    //including this most recent request
    while (this.hasProposalQueue()) {
         //sleep a random brief time to reduce the risk of duelling proposers
        try {
            int sleeptime = (int)(Math.random() * 80); //0-80ms
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
            //do nothing
        }
        try {
            //do some paxos!
            proposal = doPaxosProposal(this.popProposal());
            if (!proposal.canProceed) {
                //the proposal was refused because it was too old
                //but the returned proposal is the next one we're missing
                this.pushProposal(request);
                if (proposal.accValue != null) {
                    System.out.println("catching up at " + reqIndex);
                    this.addToHistoryAt(proposal.reqIndex, proposal.accValue);
                }
            }
        } catch (TimeoutException e) {
            //the proposal failed because of a network partition
            this.pushProposal(request);
        } catch (IllegalStateException e) {
            //the request was committed, but not every node knows
            System.err.println("WARNING: commit incomplete");
            e.printStackTrace();
        }
    }
    //XXX: what if proposal is null?
    String info = Integer.toString(proposal.reqIndex);
    POJOResBody resbody = new POJOResBody(true, info);
    sendResponse(exch, 200, resbody.toJSON());
}

private PaxosProposal doPaxosProposal (String request)
        throws TimeoutException
{
    /*
     *  Prepare.
     */
    PaxosProposal proposal = new PaxosProposal();
    proposal.seqNum = this.incrementSequenceNumber();
    proposal.reqIndex = this.getNextHistoryIndex();
    String value = null;

    proposal = sendPrepare(proposal);
    if (!proposal.canProceed) {
        //reqIndex was already agreed on--add that proposal to history
        System.out.println("reqIndex " + proposal.reqIndex + " was already agreed on");
        return proposal;
    }

    value = proposal.accValue;
    if (value == null) {
        //no value has been chosen, so it's okay to propose one
        proposal.accValue = request;
    }
    System.err.println(log("agreed on proposal " + proposal.seqNum));

    /*
     *  Accept.
     */
    proposal = sendAccept(proposal);
    if (!proposal.canProceed) {
        //someone else is proposing too--propose again, but make sure
        //the aborted proposal doesn't get added
        proposal.accValue = null;
        return proposal;
    }

    /*
     *  Consensus! Broadcast the accepted proposal.
     */
    boolean finished = sendCommit(proposal);
    if (!finished) {
        //TODO: use reliable broadcast to ensure this never happens
        System.err.println("commit failed--system may be inconsistent");
        throw new IllegalStateException();
    }
    return proposal;
}

private PaxosProposal sendPrepare (PaxosProposal proposal)
        throws TimeoutException
{
    POJOPaxosBody prop = new POJOPaxosBody(proposal.seqNum, 
                                           proposal.reqIndex, 
                                           proposal.accValue);
    System.err.println(log("preparing: " + prop.toJSON()));

    //broadcast prepare to cluster
    Client[] multi = Client.readyMulticast(this.getNodeView(), 
                                           "POST", "/paxos/acceptor/prepare", 
                                           prop.toJSON());
    for (Client cl : multi) {
        cl.fireAsync();
    }

    /*
     *  Paxos actually needs only a majority. Please see the notes for details.
     */
    //wait until all the nodes respond
    while (!Client.done(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    //process the responses
    int votenum = 0;
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        POJOResBody resbody = POJOResBody.fromJSON(res.resBody);
        if (res.resCode == 200) {
            if (resbody.info != null) {
                //there's already an accepted value--just propagate that
                proposal.accValue = resbody.info;
                return proposal;
            } else {
                votenum += 1;
            }
        } else if (res.resCode == 409) {
            //the response was old--get the more up-to-date history
            System.out.println("got committed request " + resbody.info);
            proposal.accValue = resbody.info;
            proposal.canProceed = false;
            return proposal; //HERE
        } else {
            //something bad happened--drop the response on the floor
        }
    }

    if (votenum < (multi.length / 2 + 1)) {
        //too many servers are down--fail the proposal
        proposal.accValue = null;
        proposal.canProceed = false;
        throw new TimeoutException();
    }
    return proposal;
}

private PaxosProposal sendAccept (PaxosProposal proposal)
        throws TimeoutException
{
    POJOPaxosBody prop = new POJOPaxosBody(proposal.seqNum, 
                                           proposal.reqIndex, 
                                           proposal.accValue);

    Client[] multi = Client.readyMulticast(this.getNodeView(), 
                                           "POST", "/paxos/acceptor/accept", 
                                           prop.toJSON());
    for (Client cl : multi) {
        cl.fireAsync();
    }

    /*
     *  Paxos actually needs only a majority. Please see the notes for details.
     */
    while (!Client.done(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    /*
     *  In Lamport's paxos, we would verify the response seqnum is 
     *  equal to the one we sent, and if it's higher we fail out and 
     *  set our seqnum to the higher value. However, we don't want to 
     *  skip proposals because we rebuild history from old proposals, 
     *  so we just fail out if the seqnum is not the same. 
     */
    //process the responses
    int votenum = 0;
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        if (res.resCode == 409) {
            //the response was old--get the more up-to-date history
            POJOResBody resbody = POJOResBody.fromJSON(res.resBody);
            proposal.accValue = resbody.info;
            proposal.canProceed = false;
            break;
        } else if (res.resCode == 200) {
             //200 rescode means accept was successful
             votenum += 1;
        } else {
            //something bad happened--drop the response on the floor
        }
    }

    if (votenum < (multi.length / 2 + 1)) {
        //too many servers are down--fail the proposal
        proposal.accValue = null;
        proposal.canProceed = false;
        throw new TimeoutException();
    }

    return proposal;
}

private boolean sendCommit (PaxosProposal proposal)
{
    POJOPaxosBody prop = new POJOPaxosBody(proposal.seqNum, 
                                           proposal.reqIndex, 
                                           proposal.accValue);

    Client[] multi = Client.readyMulticast(this.getNodeView(), 
                                           "POST", "/paxos/acceptor/commit", 
                                           prop.toJSON());

    for (Client cl : multi) {
        cl.fireAsync();
    }
    while (!Client.done(multi)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    //process the responses
    boolean success = true;
    for (Client cl : multi) {
        POJOResHttp res = cl.getResponse();
        if (res.resCode != 200) {
            //commit failed due to network partition--resend until successful
            //TODO: use reliable broadcast to ensure success
            System.out.println("got rescode " + res.resCode + 
                               " from " + cl.getDestIP());
            System.out.println("resbody: " + res.resBody);
            success = false;
        }
    }
    return success;
}

private boolean hasProposalQueue ()
{
    if (requestBuffer.size() != 0) {
        return true;
    }
    return false;
}

private String popProposal ()
{
    return requestBuffer.removeFirst();
}

private void pushProposal (String request)
{
    requestBuffer.addFirst(request);
}

private void queueProposal (String request)
{
    requestBuffer.addLast(request);
}

//XXX: the magic number 100 is the max number of processes in the paxos cluster
private int incrementSequenceNumber ()
{
    this.monotonicNumber += 1;
    return (this.monotonicNumber * 100) + this.processID;
}

protected ContextPaxosProposer ()
{
    monotonicNumber = 0;
    requestBuffer = new LinkedList<String>();
}


private int monotonicNumber;
private LinkedList<String> requestBuffer;

}

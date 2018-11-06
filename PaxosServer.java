import java.util.ArrayList;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class PaxosServer extends SmallServer {


public void handle (HttpExchange exch) throws IOException
{
    String path = exch.getRequestURI().getPath();
    if (path.startsWith("/paxos")) {
        doPaxos(exch);
    }
    super.handle(exch);
}

public void doPaxos (HttpExchange exch)
{
    this.getConsensus();

    System.out.println("paxos not implemented!!");
    int rescode = 501;
    String resmsg = "paxos not implemented";
    super.sendResponse(exch, rescode, resmsg, null);
}

public PaxosProposal[] doRecon ()
{
    System.out.println("doing recon...");
    PaxosProposal msg = new PaxosProposal("recon", null);
    PaxosProposal[] res = Paxos.timeoutBroadcast(msg, knownNodes);
    return res;
}

//do recon to get latest proposal
//propagate latest proposal
//recurse until consensus
public PaxosProposal getConsensus ()
{
    //do recon
    System.out.println("consensus: phase I (recon)...");
    PaxosProposal[] recon = doRecon();
    System.out.println("reconned proposals: " + recon);
    PaxosProposal latest = Paxos.latestProposal(recon);
    System.out.println("latest proposal: " + latest);
    if (latest == null) {
        //no nodes have accepted proposals
        acceptedProposal = Paxos.defaultPaxosProposal();
    }
    if (latest.isLaterThan(acceptedProposal)) {
        acceptedProposal = latest;
    }

    //propagate proposal
    System.out.println("consensus phase II (propagation)...");
    PaxosProposal[] res = Paxos.timeoutBroadcast(acceptedProposal, knownNodes);
    if (Paxos.atLeastOneNo(res)) {
        //there's a problem with the proposal--discard it
        acceptedProposal = null;
    } else if (Paxos.majorityYes(res)) {
        //consensus--return the proposal so it can be committed
        return acceptedProposal;
    }
    return getConsensus();
}

public PaxosProposal receiveProposal (RequestBody msg)
{
    String seqnum = msg.get("sequenceNumber");
    if (seqnum.equals("recon")) {
        //return latest seen proposal
        return acceptedProposal;
    } else {
        //vote on proposal
        return getConsensus();
    }
}

public PaxosServer (int pid, String[] nodes)
{
    System.out.println("initializing paxos for pid " + pid);
    acceptedProposal = null;
    proposalSequence = 0;
    processID = pid;
    knownNodes = nodes;
}


private PaxosProposal acceptedProposal;
private int proposalSequence;
private int processID;
private String[] knownNodes;

}

import java.util.ArrayList;

public class Paxos {


//XXX: for debugging
public static PaxosProposal defaultPaxosProposal ()
{
    return new PaxosProposal("foo", new PaxosMessage());
}

public static PaxosProposal latestProposal (PaxosProposal[] proposals)
{
    //return element where sequence number is latest
    PaxosProposal latest = proposals[0];
    for (int i = 1; i < proposals.length; i += 1) {
        PaxosProposal prop = proposals[i];
        if (prop.isLaterThan(latest)) {
            latest = prop;
        }
    }
    return latest;
}

//returns true if any node voted 'no'
public static boolean atLeastOneNo (PaxosProposal[] proposals)
{
    for (PaxosProposal prop : proposals) {
        if (prop == null) {
            continue;
        }
        if (prop.getVote() == false) {
            return true;
        }
    }
    return false;
}

//returns true if a majority of nodes responded with vote 'yes'
//                and no node voted 'no'
public static boolean majorityYes (PaxosProposal[] proposals)
{
    int yesvotes = 0;
    for (PaxosProposal prop : proposals) {
        if (prop == null) {
            continue;
        }
        if (prop.getVote() == false) {
            return false;
        }
        yesvotes +=1;
    }
    if (yesvotes < (proposals.length / 2) + 1) {
        return false;
    }
    return true;
}

public static PaxosProposal[] timeoutBroadcast (PaxosProposal msg, String[] nodes)
{
    System.out.println("broadcasting...");
    HttpResponse[] res = ClientRequest.timeoutBroadcastRequest(nodes, "POST", "/paxos", null, msg.toJSON(), 1);
    PaxosProposal[] props = Paxos.extractProposals(res);
    System.out.println("got broadcasts " + props);
    return props;
}

//XXX: probably belongs in ClientRequest
public static ArrayList<ResponseBody> broadcast (PaxosProposal msg, String[] nodes)
{
    System.out.println("broadcast not implemented!!");
    ArrayList<ResponseBody> list = new ArrayList<ResponseBody>();
    list.add(ResponseBody.clientError());
    return list;
}

public static PaxosProposal[] extractProposals (HttpResponse[] res)
{
    ArrayList<PaxosProposal> proposals = new ArrayList<PaxosProposal>();
    for (HttpResponse http : res) {
        String body = http.getResponseBody();
        PaxosProposal prop = new PaxosProposal(body);
        proposals.add(prop);
    }
    return proposals.toArray(new PaxosProposal[0]);
}


}

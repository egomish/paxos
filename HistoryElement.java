public class HistoryElement {


public HistoryElement (POJOReq req)
{
    paxosProvenance = new ArrayList<Integer>();
    clientReq = req;
    shardID = null;
    reqResponse = null;
}


public ArrayList<Integer> paxosProvenance;
public POJOReq clientReq;
public Integer shardID;
public String reqResponse;

}

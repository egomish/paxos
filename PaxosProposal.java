import com.google.gson.Gson;

public class PaxosProposal extends ResponseBody {


public int getSequenceNumber ()
{
    return sequenceNumber;
}

public int getProcessID ()
{
    return processID;
}

public String getAcceptedValue ()
{
    return acceptedValue;
}

public void setSequenceNumber (int n)
{
    sequenceNumber = n;
}

public void setAcceptedValue (String str)
{
    acceptedValue = str;
}

public void incrementSequenceNumber ()
{
    sequenceNumber += 1;
}

//XXX: test this or remove it
//XXX: untested--what happens is the arg isn't JSON?
//XXX: when the arg isn't JSON, a JsonSyntaxException is throws and an empty ResponseBody object is returned
public static PaxosProposal fromJSON (String json)
{
    PaxosProposal body;
    try {
        Gson gson = new Gson();
        body =  gson.fromJson(json, PaxosProposal.class);
    } catch (Exception e) {
        System.out.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        body = new PaxosProposal();
    }
    return body;
}

public PaxosProposal ()
{
    this(0);
}

public PaxosProposal (int pid)
{
    sequenceNumber = 0;
    processID = pid;
    acceptedValue = null;
}


private Integer sequenceNumber;
private Integer processID;
private String acceptedValue;

}

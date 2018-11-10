import com.google.gson.Gson;

public class PaxosProposal extends ResponseBody {


public int getSequenceNumber ()
{
    return sequenceNumber;
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
    PaxosProposal body = new PaxosProposal();
    try {
        Gson gson = new Gson();
        body =  gson.fromJson(json, body.getClass());
    } catch (Exception e) {
        System.out.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        body = new PaxosProposal();
    }
    return body;
}

public PaxosProposal ()
{
    sequenceNumber = 0;
    acceptedValue = null;
}


private Integer sequenceNumber;
private String acceptedValue;

}

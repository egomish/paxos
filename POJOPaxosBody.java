import com.google.gson.Gson;

public class POJOPaxosBody extends POJO {


//TODO: use Generics in base class
public static POJOPaxosBody fromJSON (String json)
{
    POJOPaxosBody pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOPaxosBody.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOPaxosBody();
    }
    return pojo;
}

public POJOPaxosBody ()
{
    this(null, null, null);
}

public POJOPaxosBody (Integer r, Integer s, String a)
{
    reqIndex = r;
    seqNum = s;
    accValue = a;
}

public Integer reqIndex;
public Integer seqNum;
public String accValue;

}

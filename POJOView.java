import com.google.gson.Gson;

public class POJOView extends POJO {


public String toJSON ()
{
    Gson gson = new Gson();
    String json = gson.toJson(this);
    return json;
}

public static POJOView fromJSON (String json)
{
    POJOView pojo;
    try {
        Gson gson = new Gson();
        pojo = gson.fromJson(json, POJOView.class);
    } catch (Exception e) {
        System.err.println("JSON exception: " + e.getMessage());
        //XXX: creating an empty object seems like a bad idea for debugging
        pojo = new POJOView();
    }
    return pojo;
}

public POJOView ()
{
    this(null);
}

public POJOView (String[] arr)
{
    view = "";
    if (arr != null) {
        for (String str : arr) {
            view += str + ",";
        }
        view = view.substring(0, view.length() - 1); //strip trailing ','
    }
}


public String view;

}

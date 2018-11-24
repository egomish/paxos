public class DockerTester {


public static String[] view = {"10.0.0.2:4002", 
                               "10.0.0.3:4003", "10.0.0.4:4004", 
//                               "10.0.0.5:4005", "10.0.0.6:4006", 
//                               "10.0.0.7:4007", "10.0.0.8:4008"
                               };

public static String leader_node = view[0];
public static String challenger_node = view[1];
public static String new_node = "10.0.0.9:4009";

public static String[] keys = {"key0", "key1", "key2", "key3", "key4", "key5"};
public static String[] vals = {"val0", "val1", "val2", "val3", "val4", "val5"};
public static int index = 0;


public static int next_i ()
{
    index += 1;
    return index;
}

//broadcast hello world
public static void test_hello ()
{
    Client[] multi = Client.readyMulticast(view, "GET", "/hello", null);
    for (Client cl : multi) {
        cl.doSync();
        System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
    }
}

//on leader_node, foo=bar
public static void test_put_key (String node, String key, String val)
{
    Client cl = new Client(node, "POST", "/keyValue-store/" + key, "{val: '" + val + "'}");
    cl.fireAsync();
    while (!cl.done()) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

//propose messages concurrently
public static void test_concurrent_put (int nodenum)
{
    Client[] clients = new Client[nodenum];
    for (int i = 0; i < clients.length; i += 1) {
        int index = next_i();
        Client cl = new Client(view[i], "POST", 
                                        "/keyValue-store/" + keys[index], 
                                        "{val: '" + vals[index] + "'}");
        clients[i] = cl;
        cl.fireAsync();
    }

    while (!Client.done(clients)) {
        try {
            Thread.sleep(200); //sleep 200ms
        } catch(InterruptedException e) {
            //do nothing
        }
    }

    for (int i = 0; i < clients.length; i += 1) {
        Client cl = clients[i];
        System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
    }
}

//get current view
public static void test_get_view ()
{
    Client cl = new Client(leader_node, "GET", "/view", null);
    cl.doSync();
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

//add node to view
public static void test_add_view (String ipport)
{
    Client cl = new Client(leader_node, "PUT", 
                                        "/view", 
                                        "{ip_port: '" + ipport + "'}");
    cl.doSync();

    cl = new Client(leader_node, "GET", "/view", null);
    cl.doSync();
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

//delete node from view
public static void test_delete_view (String ipport)
{
    Client cl = new Client(leader_node, "DELETE", 
                                        "/view", 
                                        "{ip_port: '" + ipport + "'}");
    cl.doSync();

    cl = new Client(leader_node, "GET", "/view", null);
    cl.doSync();
    System.out.println(cl.getDestIP() + ": " + cl.getResponse().resBody);
}

public static void test_put_add_put ()
{
    test_put_key(leader_node, keys[next_i()], vals[next_i()]);
    test_add_view(new_node);
    test_put_key(leader_node, keys[next_i()], vals[next_i()]);
}

public static void
main (String[] args)
{
//    test_add_view(new_node);
    test_concurrent_put(2);

    //TODO: sequence of concurrent requests
    //TODO: delete view
    //TODO: node crashes and immediately restarts
    //TODO: node crashes and never recovers
}


}

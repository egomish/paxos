rm *.class
javac -cp gson-2.8.5.jar:. SmallServer.java \
                           Client.java \
                           HttpRes.java \
                           \
                           POJO.java \
                           POJOKVStore.java POJOReq.java \
                           \
                           ContextHello.java \
                           ContextTest.java \
                           ContextKVS.java \
                           \


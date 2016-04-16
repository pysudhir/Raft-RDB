#resolve vertex dependencies (?)

#Compile RaftServer:
cd src;
javac -d build -cp ../lib/catalyst/catalyst-transport-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-buffer-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-serializer-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-netty-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-common-1.0.7-SNAPSHOT.jar:../lib/copycat/server/copycat-server-1.0.0-SNAPSHOT.jar:../lib/copycat/protocol/copycat-protocol-1.0.0-SNAPSHOT.jar:../log/copycat/external/logback-core-1.1.7.jar:../lib/copycat/external/netty-all-4.0.33.Final.jar:../lib/copycat/external/concurrentunit-0.4.2.jar:../lib/copycat/external/slf4j-api-1.7.7.jar:../testng-6.8.8.jar:../lib/copycat/external/mockito-core-1.10.19.jar:../lib/snakeyaml-1.10.jar:../lib/voldemort-0.96.jar:../lib/jdom-1.1.jar:../lib/g4j-1.2.17.jar:../lib/log4j-1.2.17.jar:../lib/copycat/external/slf4j-log4j12-1.7.7.jar:../lib/copycat/external/protobuf-java-2.2.0.jar:../lib/copycat/external/guava-11.0.2.jar:../lib/copycat/external/jsr-305.jar: RDBStateMachineServer.java

#Generate single jar from output classes to be used with RaftClient [in vertx server]
cd build; jar -cf raftServer.jar *

#Make sure all the details in configuration.yaml file are correct
# Run raftServer(s)
java -cp ../lib/catalyst/catalyst-transport-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-buffer-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-serializer-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-netty-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-common-1.0.7-SNAPSHOT.jar:../lib/copycat/server/copycat-server-1.0.0-SNAPSHOT.jar:../lib/copycat/protocol/copycat-protocol-1.0.0-SNAPSHOT.jar:../log/copycat/external/logback-core-1.1.7.jar:../lib/copycat/external/netty-all-4.0.33.Final.jar:../lib/copycat/external/concurrentunit-0.4.2.jar:../lib/copycat/external/slf4j-api-1.7.7.jar:../testng-6.8.8.jar:../lib/copycat/external/mockito-core-1.10.19.jar:../lib/snakeyaml-1.10.jar:../lib/voldemort-0.96.jar:../lib/jdom-1.1.jar:../lib/g4j-1.2.17.jar:../lib/log4j-1.2.17.jar:./build/raftServer.jar:../lib/copycat/external/slf4j-log4j12-1.7.7.jar:../lib/copycat/external/protobuf-java-2.2.0.jar:../lib/copycat/external/guava-11.0.2.jar:../lib/copycat/external/jsr-305.jar: RDBStateMachineServer Master tmp

#compile and run vertx server: 
vertx run -cp ../lib/catalyst/catalyst-transport-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-buffer-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-serializer-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-netty-1.0.7-SNAPSHOT.jar:../lib/catalyst/catalyst-common-1.0.7-SNAPSHOT.jar:../lib/copycat/server/copycat-server-1.0.0-SNAPSHOT.jar:../lib/copycat/protocol/copycat-protocol-1.0.0-SNAPSHOT.jar:../lib/snakeyaml-1.10.jar:../lib/voldemort-0.96.jar:../lib/jdom-1.1.jar:../lib/g4j-1.2.17.jar:build/raftServer.jar:../lib/copycat/client/copycat-client-1.0.0-SNAPSHOT.jar: Server.java

#Unit test - Go to the browser and enter the url in the following format:
http://localhost:8080/put?key=value

#To compile the client: 
javac -cp lib/commons-logging-1.2.jar:lib/core-0.1.4.jar:lib/httpclient-4.5.2.jar:lib/httpcore-4.4.4.jar:lib/json-20140107.jar: YcsbClient.java

#To run the client: 
java -cp lib/commons-logging-1.2.jar:lib/core-0.1.4.jar:lib/httpclient-4.5.2.jar:lib/httpcore-4.4.4.jar:lib/json-20140107.jar: com.yahoo.ycsb.Client -t -db YcsbClient -P ../workloads/workloadd

/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

/* catalyst imports - Transport */
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;
import io.atomix.catalyst.serializer.Serializer;

/* copycat imports - RAFT library */
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;

/* YAML imports */
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;

/**
 * RDB (Replicated DB) state machine server.
 *
 */
public class RDBStateMachineServer {
  private final String configFile = "../configuration.yaml";
  private final String serverName;
  private final String path;
  private Address self = null;
  private List<Address> members = null;

  public RDBStateMachineServer(String serverName, String path) {
	this.serverName = serverName;
	this.path = path;
	configure(); /* Discover other members */
  }

  /**
   * Starts the server.
   */
  private void run() throws Exception {
	assert (path != null);
    CopycatServer server = CopycatServer.builder(self)
      .withStateMachine(RDBStateMachine::new)
      .withTransport(new NettyTransport())
      .withStorage(Storage.builder()
        .withDirectory(path)
        .withMaxSegmentSize(1024 * 1024 * 32)
        .withMinorCompactionInterval(Duration.ofMinutes(1))
        .withMajorCompactionInterval(Duration.ofMinutes(15))
        .build())
      .build();

    server.serializer().register(SetCommand.class, 1);
    server.serializer().register(GetQuery.class, 2);
    server.serializer().register(DeleteCommand.class, 3);

    server.bootstrap(members).join();

    while (server.isRunning()) {
      Thread.sleep(1000);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2)
      throw new IllegalArgumentException("Usage: RDBStateMachineServer <serverName> <path>");

	String serverName = args[0];
	String path = args[1];
	RDBStateMachineServer server = new RDBStateMachineServer(serverName, path);
	server.run();
  }

  private void configure() {
	  members = new ArrayList<>();
	  /* Parse and extract server replicas */
	  try {
		  InputStream ios = new FileInputStream(new File(configFile));
		  Constructor c = new Constructor(ParsedConfiguration.class);
		  Yaml yaml = new Yaml(c);
		  ParsedConfiguration result = (ParsedConfiguration) yaml.load(ios);
		  for (User user : result.configuration) {
			  if(user.name.equalsIgnoreCase(serverName)) {
				self = new Address(user.ip, user.port);
			  } else {
				members.add(new Address(user.ip, user.port));
			  }
		  }
		  assert ((members.size() % 2) == 0); /* Sanity check to ensure there are odd number of servers */
	  } catch (Exception e) {
		  e.printStackTrace();
	  }
  }

}

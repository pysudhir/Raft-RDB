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

import io.atomix.copycat.server.Commit;
import io.atomix.copycat.server.StateMachine;
import io.atomix.copycat.server.StateMachineExecutor;

/* Voldemort imports - Storage engine */
import voldemort.client.StoreClientFactory;
import voldemort.client.SocketStoreClientFactory;
import voldemort.client.StoreClient;
import voldemort.client.ClientConfig;
import voldemort.versioning.Versioned;
import voldemort.versioning.Version;

/**
 * RDB (Replicated DB) state machine.
 *
 */
public class RDBStateMachine extends StateMachine {
  private Commit<SetCommand> entry;
  private final String db = "test";
  private final String dbUrl = "tcp://127.0.0.1:6666";
  private StoreClientFactory dbfactory = null;
  private StoreClient<String, String> dbclient = null;

  public RDBStateMachine() {
	dbfactory = new SocketStoreClientFactory(new ClientConfig().setBootstrapUrls(dbUrl));
	dbclient = dbfactory.getStoreClient(db);
  }

  @Override
  protected void configure(StateMachineExecutor executor) {
    executor.register(SetCommand.class, this::set);
    executor.register(GetQuery.class, this::get);
    executor.register(DeleteCommand.class, this::delete);
  }

  /**
   * Sets the DB entry.
   */
  private Object set(Commit<SetCommand> commit) {
    try {
	  dbclient.put(commit.operation().key().toString(), commit.operation().value().toString());
	  commit.close();
      return null;
    } catch (Exception e) {
      commit.close();
      throw e;
	}
  }

  /**
   * Gets the value.
   */
  private Object get(Commit<GetQuery> commit) {
	String result;
    try {
	  Versioned<String> version = dbclient.get(commit.operation().key().toString());
	  result = (version == null) ? null : version.getValue();
    } finally {
      commit.close();
    }
	return result;
  }

  /**
   * Deletes the key.
   */
  private void delete(Commit<DeleteCommand> commit) {
    try {
		dbclient.delete(commit.operation().key().toString());
    } finally {
      commit.close();
    }
  }

}

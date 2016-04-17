import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import java.util.*;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.*;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;

import com.hazelcast.core.*;
import com.hazelcast.config.*;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/* atomix/catalyst imports NettyTransport library */
import io.atomix.catalyst.transport.Address;
import io.atomix.catalyst.transport.NettyTransport;

/* atomix/copycat imports - RAFT library */
import io.atomix.copycat.server.CopycatServer;
import io.atomix.copycat.server.storage.Storage;
import io.atomix.copycat.client.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Server extends AbstractVerticle {
	private final String configFile = "../configuration.yaml"; // The path of your YAML file.
	private ParsedConfiguration result;
	private boolean raftClientInitialized;

	private CopycatClient raftClient = null;
	private List<Address> raftServers = null;

	private void handleGet(HttpServerRequest req, String query) {
		vertx.executeBlocking(future -> {
			try {
				String result = raftClient.submit(new GetQuery(query)).get().toString();	
				req.response().setStatusCode(200);
				req.response().headers()
				.add("Content-Length", String.valueOf(result.toString().length()))
				.add("Content-Type", "text/html; charset=UTF-8");
				req.response().write(result.toString());
				System.out.println("returning to client "+result.toString());
				req.response().end();
			} catch(Exception e) {
				handleInvalidReq(req);
			}
			
			future.complete();
		}, res -> {
		});
	}

	private void handlePut(HttpServerRequest req, String query) {
		String key = query.substring(0, query.indexOf('='));
		String value = query.substring(query.indexOf('=')+1);

		vertx.executeBlocking(future -> {
			raftClient.submit(new SetCommand(key, value)).thenRun(() -> {
				req.response().setStatusCode(200);
				req.response().headers()
				.add("Content-Length", String.valueOf(16))
				.add("Content-Type", "text/html; charset=UTF-8");
				req.response().write("write successful");
				req.response().end();
			});
			future.complete();
		}, res -> {
		});
	}

	private void handleDelete(HttpServerRequest req, String query) {
		String key = query;

		vertx.executeBlocking(future -> {
			raftClient.submit(new DeleteCommand(key)).thenRun(() -> {
				req.response().setStatusCode(200);
				req.response().headers()
				.add("Content-Length", String.valueOf(17))
				.add("Content-Type", "text/html; charset=UTF-8");
			req.response().write("delete successful");
			req.response().end();
			});
			future.complete();
		}, res -> {
		});
	}

	private void handleInvalidReq(HttpServerRequest req) {
		req.response().setStatusCode(400); /* Bad Request */
		req.response().headers()
			.add("Content-Length", String.valueOf(15))
			.add("Content-Type", "text/html; charset=UTF-8");

		req.response().write("Invalid request");
		req.response().end();
	}

	private void handleUninitialized(HttpServerRequest req) {
		req.response().setStatusCode(501); /* Bad Request */
		req.response().headers()
			.add("Content-Length", String.valueOf(21))
			.add("Content-Type", "text/html; charset=UTF-8");

		req.response().write("Internal server error");
		req.response().end();
	}

	public void start() {
		raftClientInitialized = false;
	  	configure();
		HttpServer server = vertx.createHttpServer();
		server.requestHandler(new Handler<HttpServerRequest>() {
			public void handle(HttpServerRequest req) {
				int i;
				String uri = req.uri();
				String query = req.query();
				String command = "";

				if(uri.contains("?")) {
					command = uri.substring(1, uri.indexOf('?'));
				}

				if(command.equals("")) {
					handleInvalidReq(req);
					return;
				}

				if(!raftClientInitialized) {
					handleUninitialized(req);
					return;
				}

				if(command.equalsIgnoreCase("get")) {
					handleGet(req, query);
				} else if (command.equalsIgnoreCase("put")) {
					handlePut(req, query);
				} else if (command.equalsIgnoreCase("delete")) {
					handleDelete(req, query);
				} else {
					handleInvalidReq(req);
				}
				System.out.println("command:"+command);
			}
		}).listen(8080);
	}

	private void configure() {
		raftServers = new ArrayList<>();
		/* Parse and extract server replicas */
		try {
			InputStream ios = new FileInputStream(new File(configFile));
			Constructor c = new Constructor(ParsedConfiguration.class);
			Yaml yaml = new Yaml(c);
			result = (ParsedConfiguration) yaml.load(ios);
			for (User user : result.configuration) {
				raftServers.add(new Address(user.ip, user.port));
			}
			assert ((raftServers.size() % 2) == 0); /* Sanity check to ensure there are odd number of servers */
		} catch (Exception e) {
			e.printStackTrace();
		}

		vertx.executeBlocking(future -> {
			raftClient = CopycatClient.builder(raftServers)
			.withTransport(new NettyTransport())
			.withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
			.withRecoveryStrategy(RecoveryStrategies.RECOVER)
			.withServerSelectionStrategy(ServerSelectionStrategies.LEADER)
			.build();

			raftClient.serializer().register(SetCommand.class, 1);
			raftClient.serializer().register(GetQuery.class, 2);
			raftClient.serializer().register(DeleteCommand.class, 3);

			raftClient.connect().join();
			future.complete();
		}, res -> {
			raftClientInitialized = true;
			System.out.println("Raft client initialized");
		});
	}
}

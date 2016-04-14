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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class Server extends AbstractVerticle {
	private final String configFile = "../configuration.yaml"; // The path of your YAML file.
	private ParsedConfiguration result;

	private CopycatClient raftClient = null;
	private List<Address> raftServers = null;

	private void handleGet(HttpServerRequest req, String query) {
		raftClient.submit(new GetQuery(query)).thenAccept(result -> {
			req.response().setStatusCode(200);
			req.response().headers()
			.add("Content-Length", String.valueOf(result.length()))
			.add("Content-Type", "text/html; charset=UTF-8");
			req.response().write(result);
			req.response().end();
		});
	}

	private void handlePut(HttpServerRequest req, String query) {
		String key = query.substring(0, query.indexOf('='));
		String value = query.substring(0, query.indexOf('='))

		raftClient.submit(new SetCommand(key, value))->thenRun(() -> {
			req.response().setStatusCode(200);
			req.response().headers()
			.add("Content-Length", String.valueOf(16))
			.add("Content-Type", "text/html; charset=UTF-8");
			req.response().write("write successful");
			req.response().end();
		});

		/* // TODO: Ensure I can use CompletableFuture rather than executeBlocking
		vertx.executeBlocking(future -> {
			future.complete();
		}, res -> {
		});
		*/
	}

	private void handleDelete(HttpServerRequest req, String query) {
		String key = query;

		raftClient.submit(new DeleteCommand(key))->thenRun(() -> {
			req.response().setStatusCode(200);
			req.response().headers()
			.add("Content-Length", String.valueOf(17))
			.add("Content-Type", "text/html; charset=UTF-8");
			req.response().write("delete successful");
			req.response().end();
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

	public void start() {
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
					return handleInvalidRequest(req);
				}

				if(command.equalsIgnoreCase("get")) {
					handleGet(req, query);
				} else if (command.equalsIgnoreCase("put")) {
					handlePut(req);
				} else if (command.equalsIgnoreCase("delete")) {
					handleDelete(req);
				} else {
					handleInvalidRequest(req);
				}
			}
		}).listen(8080);
	}

	private void configure() {
		raftServers = ArrayList<>();
		/* Parse and extract server replicas */
		try {
			InputStream ios = new FileInputStream(new File(configFile));
			Constructor c = new Constructor(ParsedConfiguration.class);
			Yaml yaml = new Yaml(c);
			result = (ParsedConfiguration) yaml.load(ios);
			for (User user : result.configuration) {
				raftServers.add(new Address(user.ip, user.port));
			}
			assert (raftServers.size() % 2); /* Sanity check to ensure there are odd number of servers */
		} catch (Exception e) {
			e.printStackTrace();
		}

		raftClient = CopycatClient.builder(raftServers).
			.withTransport(new NettyTransport())
			.withConnectionStrategy(ConnectionStrategies.FIBONACCI_BACKOFF)
			.withRecoveryStrategy(RecoveryStrategies.RECOVER)
			.withServerSelectionStrategy(ServerSelectionStrategies.LEADER)
			.build();

		client.serializer().register(SetCommand.class, 1);
		client.serializer().register(GetQuery.class, 2);
		client.serializer().register(DeleteCommand.class, 3);

		client.connect().join();
	}
}

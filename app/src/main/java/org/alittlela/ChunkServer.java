package org.alittlela;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ChunkServer {

    private static final Logger logger = Logger.getLogger(ChunkServer.class.getName());
    private Server server;

    public ChunkServer() {
    }

    public void run(ChunkServerConfig config) throws IOException, InterruptedException {
        // launch a server for backend clients to access
        start(config);
        blockUntilShutdown();
    }

    private void start(ChunkServerConfig config) throws IOException {
        /* The port on which the server should run */
        int port = config.listeningPort;
        server = ServerBuilder.forPort(port)
                .addService(new HelloWorldServer.GreeterImpl())
                .build()
                .start();
        logger.info("ChunkServer started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                ChunkServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public record ChunkServerConfig(int listeningPort) {
    }

    static class ChunkServerImpl extends ChunkServerGrpc.ChunkServerImplBase {
        @Override
        public void chunkRead(ChunkReadReq request, StreamObserver<ChunkData> responseObserver) {
            // TODO:
            byte[] testData = new byte[]{'b', 'e', 'e', 'f'};
            ChunkData data = ChunkData.newBuilder().setData(ByteString.copyFrom(testData)).build();
            responseObserver.onNext(data);
            responseObserver.onCompleted();
        }

    }
}

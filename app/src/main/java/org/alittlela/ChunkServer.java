package org.alittlela;

import com.google.protobuf.ByteString;

import org.alittlela.util.ResultUtil;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ChunkServer {

    private static final Logger logger = Logger.getLogger(ChunkServer.class.getName());
    private Server server;
    // other masters
    private String[] masters;
    private String[] chunkServers;
    private HashMap<String, byte[]> pendingAppends = new HashMap<>();

    public ChunkServer() {
    }

    public void run(ChunkServerConfig config) throws IOException, InterruptedException {
        // launch a server for backend clients to access
        start(config);
        this.masters = config.masters;
        this.chunkServers = config.chunkServers;
        blockUntilShutdown();
    }

    private void start(ChunkServerConfig config) throws IOException {
        /* The port on which the server should run */
        int port = config.listeningPort;
        server = ServerBuilder.forPort(port).addService(new HelloWorldServer.GreeterImpl()).build().start();
        logger.info("ChunkServer started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown
            // hook.
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
     * Await termination on the main thread since the grpc library uses daemon
     * threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public record ChunkServerConfig(int listeningPort, String[] masters, String[] chunkServers) {
    }

    public byte[] chunkRead(String id, int start, int end) {
        // TODO
        byte[] testData = new byte[] { 'b', 'e', 'e', 'f' };
        return testData;
    }

    public void appendPrepare(String id, byte[] data) {
        pendingAppends.put(id, data);
    }

    public Result appendExec(String id, String[] secondaries) {
        // TODO
        byte[] data = pendingAppends.get(id);
        if (data == null) {
            return ResultUtil.newResult(ResultUtil.NO_SUCH_APPEND);
        }
        Result result = ResultUtil.success();
        // Ask all secondary to execute the append
        int offset = 0;
        for (String secondary : secondaries) {
            try {
                RpcClient client = RpcClient.getRpcClient(secondary);
                Result clientResult = client.issueSecondaryAppend(id, offset);
                if (!ResultUtil.isOk(clientResult)) {
                    result = clientResult;
                    break;
                }
            } catch (Exception e) {
                result = ResultUtil.error();
            }
        }
        return result;
    }

    /**
     * as a secondary, execute a pending append of {@link chunkId}
     * @param appendId the id of pending append
     * @return Result
     */
    public Result secondaryAppend(String appendId, int offset) {
        Result result = ResultUtil.success();
        // TODO: fs operations
        return result;
    }


    public HashMap<String, byte[]> getPendingAppends() {
        return pendingAppends;
    }

    /**
     * RPC implementation
     */
    public class ChunkServerImpl extends DfsServiceGrpc.DfsServiceImplBase {
        @Override
        public void chunkRead(ChunkReadReq request, StreamObserver<ChunkData> responseObserver) {
            // TODO:
            String id = request.getId().getId();
            int start = request.getStart();
            int end = request.getEnd();
            byte[] readData = ChunkServer.this.chunkRead(id, start, end);
            ChunkData data = ChunkData.newBuilder().setData(ByteString.copyFrom(readData)).build();
            responseObserver.onNext(data);
            responseObserver.onCompleted();
        }

        @Override
        public void appendPrepare(AppendPrepareReq req, StreamObserver<Result> result) {
            Id id = req.getId();
            ChunkData data = req.getData();
            ChunkServer.this.appendPrepare(id.getId(), data.getData().toByteArray());
            Result res = Result.getDefaultInstance();
            result.onNext(res);
            result.onCompleted();
        }

        @Override
        public void primaryAppendExec(AppendReq appendReq, StreamObserver<Result> responseObserver) {
            String id = appendReq.getId().getId();
            String[] secondaries = (String[]) appendReq.getSecondariesList().toArray();
            Result result = ChunkServer.this.appendExec(id, secondaries);
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        }

        @Override
        public void secondaryAppendExec(SecondaryAppendReq request, StreamObserver<Result> responseObserver) {
            String id = request.getId().getId();
            int offset = request.getOffset();
            ChunkServer.this.secondaryAppend(id, offset);
            responseObserver.onCompleted();
        }
    }
}

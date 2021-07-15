package org.alittlela;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import org.alittlela.fs.Fs;
import org.alittlela.util.ResultUtil;

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

    private HashMap<String, AppendOp> pendingAppends = new HashMap<>();
    private String baseDir;

    public ChunkServer() {
        this.baseDir = "chunk/";
    }

    public ChunkServer(String baseDir) {
        this.baseDir = baseDir;
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
        server = ServerBuilder.forPort(port).addService(new ChunkServer.ChunkServerImpl()).build().start();
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

    private String buildPath(String filename) {
        return this.baseDir + filename;
    }

    public record ChunkServerConfig(int listeningPort, String[] masters, String[] chunkServers) {
    }

    private record AppendOp(String chunkId, byte[] data) {
    }

    public byte[] chunkRead(String id, int start, int end) throws Exception {
        System.out.println("chunkRead" + id + " " + start + " " + end);
        String path = buildPath(id);
        byte[] data = new byte[0];
        data = Fs.read(path, start, end);
        return data;
    }

    public void appendPrepare(String chunkId, String appendId, byte[] data) {
        pendingAppends.put(appendId, new AppendOp(chunkId, data));
    }

    public Result appendExec(String id, String[] secondaries) {
        byte[] data = pendingAppends.get(id).data();
        if (data == null) {
            return ResultUtil.newResult(ResultUtil.NO_SUCH_APPEND);
        }
        AppendOp op = pendingAppends.remove(id);
        int offset = 0;
        try {
            offset = Fs.append(buildPath(op.chunkId), op.data());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.error(e.getMessage());
        }
        Result result = ResultUtil.success();
        // Ask all secondary to execute the append
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
     * as a secondary, execute a pending append of appendId
     *
     * @param appendId the id of pending append
     * @return Result
     */
    public Result secondaryAppend(String appendId, int offset) {
        Result result = ResultUtil.success();
        AppendOp op = pendingAppends.get(appendId);
        String filename = buildPath(op.chunkId);
        byte[] data = op.data();
        // fs operations
        try {
            Fs.write(filename, offset, data);
        } catch (Exception e) {
            e.printStackTrace();
            result = ResultUtil.error(e.getMessage());
        }
        return result;
    }

    public HashMap<String, AppendOp> getPendingAppends() {
        return pendingAppends;
    }

    /**
     * RPC implementation
     */
    public class ChunkServerImpl extends DfsServiceGrpc.DfsServiceImplBase {
        @Override
        public void chunkRead(ChunkReadReq request, StreamObserver<ChunkData> responseObserver) {
            String id = request.getId().getId();
            int start = request.getStart();
            int end = request.getEnd();
            byte[] readData = new byte[0];
            try {
                readData = ChunkServer.this.chunkRead(id, start, end);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ChunkData data = ChunkData.newBuilder().setData(ByteString.copyFrom(readData)).build();
            responseObserver.onNext(data);
            responseObserver.onCompleted();
        }

        @Override
        public void appendPrepare(AppendPrepareReq req, StreamObserver<AppendPrepareResult> result) {
            Id id = req.getId();
            String appendId = req.getAppendId().getId();
            ChunkData data = req.getData();
            ChunkServer.this.appendPrepare(id.getId(), appendId, data.getData().toByteArray());
            AppendPrepareResult res = AppendPrepareResult.newBuilder().setStatus(0).build();
            result.onNext(res);
            result.onCompleted();
        }

        @Override
        public void primaryAppendExec(AppendReq appendReq, StreamObserver<Result> responseObserver) {
            String appendId = appendReq.getAppendId().getId();
            String[] secondaries = appendReq.getSecondariesList().toArray(new String[0]);
            Result result = ChunkServer.this.appendExec(appendId, secondaries);
            responseObserver.onNext(result);
            responseObserver.onCompleted();
        }

        @Override
        public void secondaryAppendExec(SecondaryAppendReq request, StreamObserver<Result> responseObserver) {
            String appendId = request.getApppendId().getId();
            int offset = request.getOffset();
            ChunkServer.this.secondaryAppend(appendId, offset);
            responseObserver.onCompleted();
        }
    }
}

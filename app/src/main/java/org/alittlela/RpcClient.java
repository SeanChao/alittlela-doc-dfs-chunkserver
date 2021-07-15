package org.alittlela;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.alittlela.util.ResultUtil;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * RpcClient for ChunkServer
 */
public class RpcClient {
	private static final Logger logger = Logger.getLogger(ChunkServer.class.getName());

	private final DfsServiceGrpc.DfsServiceBlockingStub blockingStub;

	/**
	 * Construct client for accessing HelloWorld server using the existing channel.
	 */
	public RpcClient(Channel channel) {
		// 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
		// responsibility to
		// shut it down.

		// Passing Channels to code makes code easier to test and makes it easier to
		// reuse Channels.
		blockingStub = DfsServiceGrpc.newBlockingStub(channel);
	}

	public static RpcClient getRpcClient(String target) throws InterruptedException {
		ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
				// Channels are secure by default (via SSL/TLS). For the example we disable TLS
				// to avoid
				// needing certificates.
				.usePlaintext().build();
		RpcClient client;
		try {
			client = new RpcClient(channel);
		} finally {
			// ManagedChannels use resources like threads and TCP connections. To prevent
			// leaking these
			// resources the channel should be shut down when it will no longer be used. If
			// it may be used
			// again leave it running.
			channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
		}
		return client;
	}

	public Result issueSecondaryAppend(String idString, int offset) {
		Id id = Id.newBuilder().setId(idString).build();
		SecondaryAppendReq request = SecondaryAppendReq.newBuilder().setApppendId(id).setOffset(offset).build();
		Result result;
		try {
			result = blockingStub.secondaryAppendExec(request);
		} catch (StatusRuntimeException e) {
			logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
			return ResultUtil.error(e.toString());
		}
		return result;
	}
}

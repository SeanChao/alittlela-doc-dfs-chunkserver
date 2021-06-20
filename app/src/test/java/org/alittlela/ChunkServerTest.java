package org.alittlela;

import com.google.protobuf.ByteString;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class ChunkServerTest {
  /**
   * This rule manages automatic graceful shutdown for the registered servers and channels at the
   * end of test.
   */
  @Rule
  public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  /**
   * To test the server, make calls with a real stub using the in-process channel, and verify
   * behaviors or state changes from the client side.
   */
  @Test
  public void chunkReadImpl() throws Exception {
    // Generate a unique in-process server name.
    String serverName = InProcessServerBuilder.generateName();

    // Create a server, add service, start, and register for automatic graceful shutdown.
    grpcCleanup.register(InProcessServerBuilder
            .forName(serverName).directExecutor().addService(new ChunkServer.ChunkServerImpl()).build().start());

    ChunkServerGrpc.ChunkServerBlockingStub blockingStub = ChunkServerGrpc.newBlockingStub(
            // Create a client channel and register for automatic graceful shutdown.
            grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));


    ChunkData reply =
            blockingStub.chunkRead(ChunkReadReq.newBuilder().setUuid("").build());

    byte[] testData = new byte[]{'b', 'e', 'e', 'f'};
    assertEquals(ByteString.copyFrom(testData), reply.getData());
  }
}

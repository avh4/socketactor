package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public abstract class SocketChannelReaderContract<L extends Disconnectable> {
    protected SocketChannelReader<L> subject;
    protected L listener;
    @Mock protected SocketChannelActor master;
    @Mock protected SocketChannel channel;
    @Mock private IOException exception;
    protected ActorRef<L> listenerRef;

    protected abstract SocketChannelReader<L> createSubject() throws IOException;

    protected abstract L createListener();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        listener = createListener();
        listenerRef = ActorRef.wrap(listener);
        subject = createSubject();
        subject.setReceiver(listenerRef);
    }

    @Test
    public void selected_withData_withNoRequests_waits() throws Exception {
        subject.selected();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void selected_whenChannelIsAtEof_withPendingRequest_disconnects() throws Exception {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toReturn(-1);
        subject.next();
        subject.selected();

        verify(master).disconnect(same(listenerRef), any(Exception.class));
    }

    @Test
    public void selected_whenChannelIsAtEof_withNoRequests_waits() throws Exception {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toReturn(-1);
        subject.selected();

        verifyNoMoreInteractions(master);
    }

    @Test
    public void selected_withIOException_withPendingRequest_disconnects() throws Exception {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toThrow(exception);
        subject.next();
        subject.selected();

        verify(master).disconnect(listenerRef, exception);
    }

    @Test
    public void selected_withIOException_withNoRequests_waits() throws Exception {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toThrow(exception);
        subject.selected();

        verifyNoMoreInteractions(master);
    }

    @Test
    public void closed_withPendingRequest_disconnects() throws Exception {
        subject.next();
        subject.closed();
        verify(master).disconnect(same(listenerRef), any(Exception.class));
    }

    @Test
    public void closed_withNoRequests_waits() throws Exception {
        subject.closed();
        verifyNoMoreInteractions(master);
    }

    @Test
    public void next_withQueuedDisconnect_disconnects() throws Exception {
        subject.closed();
        subject.next();
        verify(master).disconnect(same(listenerRef), any(Exception.class));
    }

    protected void stubChannelRead(final String newData) throws IOException {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toAnswer(new Answer<Integer>() {
            @Override public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
                buffer.put(newData.getBytes());
                return newData.getBytes().length;
            }
        });
    }
}

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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class LinesSocketChannelReaderTest {

    private SocketChannelReader subject;
    @Mock private SocketChannelActor master;
    @Mock private LineListener listener;
    @Mock private SocketChannel channel;
    @Mock private IOException exception;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        stubChannelRead("ABC\n");
        subject = new LinesSocketChannelReader(master, ActorRef.wrap(listener), 256, channel);
    }

    @Test
    public void selected_withData_withPendingRequest_sendsData() throws Exception {
        subject.next();
        subject.selected();

        verify(listener).receivedLine("ABC");
    }

    @Test
    public void selected_withData_withNoRequests_waits() throws Exception {
        subject.selected();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void selected_withPartialLine_waits() throws Exception {
        stubChannelRead("AB");
        subject.next();
        subject.selected();

        verifyNoMoreInteractions(listener);
    }

    @Test
    public void selected_withRemainderOfPartialLine_sendsData() throws Exception {
        subject.next();
        stubChannelRead("AB");
        subject.selected();
        stubChannelRead("C\nD");
        subject.selected();

        verify(listener).receivedLine("ABC");
    }

    @Test
    public void next_withPendingData_sendsData() throws Exception {
        subject.selected();
        subject.next();

        verify(listener).receivedLine("ABC");
    }

    @Test
    public void extraTestCase_makesMeFeelSafer() throws Exception {
        subject.selected();
        subject.next();
        subject.next();
        subject.selected();

        verify(listener, times(2)).receivedLine("ABC");
    }

    @Test
    public void next_withMultiplePendingData_sendsNextLine() throws Exception {
        subject.selected();
        subject.selected();
        subject.next();

        verify(listener).receivedLine("ABC");
    }

    @Test
    public void next_withMorePendingData_sendsNextLine() throws Exception {
        subject.selected();
        subject.selected();
        subject.next();
        reset(listener);
        subject.next();

        verify(listener).receivedLine("ABC");
    }

    @Test
    public void selected_whenChannelIsAtEof_disconnects() throws Exception {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toReturn(-1);
        subject.selected();

        verify(master).disconnect(anyString());
    }

    @Test
    public void selected_withIOException_disconnects() throws Exception {
        reset(channel);
        stub(channel.read(any(ByteBuffer.class))).toThrow(exception);
        subject.selected();

        verify(master).disconnect(exception);
    }

    @Test
    public void closed_disconnects() throws Exception {
        subject.closed();
        verify(master).disconnect(anyString());
    }

    private void stubChannelRead(final String newData) throws IOException {
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

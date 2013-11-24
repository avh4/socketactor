package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class LineBufferTest {

    private LineBuffer subject;
    @Mock private LineBuffer.Listener listener;
    @Mock private Throwable cause;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        subject = new LineBuffer(ActorRef.wrap(listener));
    }

    @Test
    public void receivingALineOfData_forwardsTheLine() throws Exception {
        subject.received("full line\n".getBytes());
        verify(listener).receivedLine("full line");
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void disconnected_forwardsDisconnect() throws Exception {
        subject.disconnected(cause);
        verify(listener).disconnected(cause);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void receivingMultipleLines_forwardsEachLine() throws Exception {
        subject.received("line 1\nline 2\n".getBytes());
        verify(listener).receivedLine("line 1");
        verify(listener).receivedLine("line 2");
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void receivingPartialLine_waitsForMoreData() throws Exception {
        subject.received("partial ".getBytes());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void receivingCompletionOfLine_fowardsFullLine() throws Exception {
        subject.received("partial ".getBytes());
        subject.received("line\n".getBytes());
        verify(listener).receivedLine("partial line");
        verifyNoMoreInteractions(listener);
    }
}

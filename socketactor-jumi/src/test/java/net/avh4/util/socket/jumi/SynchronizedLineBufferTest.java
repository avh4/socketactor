package net.avh4.util.socket.jumi;

import fi.jumi.actors.ActorRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class SynchronizedLineBufferTest {

    private SynchronizedLineBuffer subject;
    @Mock private LineBuffer.Listener listener;
    @Mock private Throwable cause;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        subject = new SynchronizedLineBuffer(ActorRef.wrap(listener));
    }

    @Test
    public void receivingALineOfData_waitsForNext() throws Exception {
        subject.received("full line\n".getBytes());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void next_withDataReady_forwardsNextLine() throws Exception {
        subject.received("full line\n".getBytes());
        subject.next();
        verify(listener).receivedLine("full line");
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void receivingData_withPendingNext_forwardsNextLine() throws Exception {
        subject.next();
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
    public void receivingMultipleLines_forwardsFirstLine() throws Exception {
        subject.next();
        subject.received("line 1\nline 2\n".getBytes());
        verify(listener).receivedLine("line 1");
        subject.next();
        verify(listener).receivedLine("line 2");
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void receivingPartialLine_waitsForMoreData() throws Exception {
        subject.received("partial ".getBytes());
        subject.next();
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void receivingCompletionOfLine_forwardsFullLine() throws Exception {
        subject.received("partial ".getBytes());
        subject.next();
        subject.received("line\n".getBytes());
        verify(listener).receivedLine("partial line");
        verifyNoMoreInteractions(listener);
    }
}

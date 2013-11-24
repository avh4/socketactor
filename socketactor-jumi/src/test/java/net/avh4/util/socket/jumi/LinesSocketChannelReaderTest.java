package net.avh4.util.socket.jumi;

import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class LinesSocketChannelReaderTest extends SocketChannelReaderContract<LineListener> {

    @Override protected SocketChannelReader<LineListener> createSubject() throws IOException {
        stubChannelRead("ABC\n");
        return new LinesSocketChannelReader(master, 256, channel);
    }

    @Override protected LineListener createListener() {
        return Mockito.mock(LineListener.class);
    }

    @Test
    public void selected_withData_withPendingRequest_sendsData() throws Exception {
        subject.next();
        subject.selected();

        verify(listener).receivedLine("ABC");
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
}

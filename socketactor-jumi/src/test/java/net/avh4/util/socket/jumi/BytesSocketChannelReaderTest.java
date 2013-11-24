package net.avh4.util.socket.jumi;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class BytesSocketChannelReaderTest extends SocketChannelReaderContract<BytesListener> {

    @Override protected SocketChannelReader<BytesListener> createSubject() throws IOException {
        stubChannelRead("ABC");
        return new BytesSocketChannelReader(master, 256, channel);
    }

    @Override protected BytesListener createListener() {
        return Mockito.mock(BytesListener.class);
    }

    @Test
    public void selected_withData_withPendingRequest_sendsData() throws Exception {
        subject.next();
        subject.selected();

        verify(listener).received("ABC".getBytes());
    }

    @Test
    public void next_withPendingData_sendsData() throws Exception {
        subject.selected();
        subject.next();

        verify(listener).received("ABC".getBytes());
    }

    @Test
    public void extraTestCase_makesMeFeelSafer() throws Exception {
        subject.selected();
        subject.next();
        subject.next();
        subject.selected();

        verify(listener, times(2)).received("ABC".getBytes());
    }

    @Test
    public void next_withMultiplePendingData_sendsAllData() throws Exception {
        subject.selected();
        subject.selected();
        subject.next();

        verify(listener).received("ABCABC".getBytes());
    }
}

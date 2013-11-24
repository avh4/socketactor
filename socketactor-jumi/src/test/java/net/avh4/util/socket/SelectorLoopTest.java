package net.avh4.util.socket;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.channels.Selector;
import java.util.concurrent.Semaphore;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

public class SelectorLoopTest {
    private SelectorLoop subject;
    @Mock private SelectorLoop.Delegate delegate;
    @Mock private Selector selector;
    @Mock private Semaphore semaphore;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        stub(selector.isOpen()).toReturn(true);

        subject = new SelectorLoop(selector, delegate);
        subject.prepare();
    }

    @Test
    public void whenSelectorSelects_tellsListener() throws Exception {
        stub(selector.select()).toReturn(1);
        subject.loop();

        verify(delegate).selected();
    }

    @Test
    public void whenSelectorSelects_keepsLooping() throws Exception {
        stub(selector.select()).toReturn(1);
        final boolean keepGoing = subject.loop();

        assertThat(keepGoing, is(true));
    }

    @Test
    public void whenSelectorWakesUp_tellsListener() throws Exception {
        stub(selector.select()).toReturn(0);
        subject.loop();

        verify(delegate).selected();
    }

    @Test
    public void whenSelectorWakesUp_keepsLooping() throws Exception {
        stub(selector.select()).toReturn(0);
        final boolean keepGoing = subject.loop();

        assertThat(keepGoing, is(true));
    }

    @Test
    public void whenSelectorCloses_tellsListener() throws Exception {
        stub(selector.select()).toReturn(0);
        stub(selector.isOpen()).toReturn(false);
        subject.loop();

        verify(delegate).closed();
    }

    @Test
    public void whenSelectorCloses_stopsLooping() throws Exception {
        stub(selector.select()).toReturn(0);
        stub(selector.isOpen()).toReturn(false);
        final boolean keepGoing = subject.loop();

        assertThat(keepGoing, is(false));
    }

    @Test
    public void cleanup_shouldCloseSelector() throws Exception {
        subject.cleanup();

        verify(selector).close();
    }
}

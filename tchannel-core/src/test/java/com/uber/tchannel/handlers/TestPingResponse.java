/*
 * Copyright (c) 2015 Uber Technologies, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.uber.tchannel.handlers;

import com.uber.tchannel.codecs.MessageCodec;
import com.uber.tchannel.framing.TFrame;
import com.uber.tchannel.messages.MessageType;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestPingResponse {

    @Test
    public void shouldInterceptPing() {

        EmbeddedChannel channel = new EmbeddedChannel(
                new MessageCodec(),
                new PingHandler()
        );

        TFrame frame = new TFrame(0, MessageType.PingRequest.byteValue(), Integer.MAX_VALUE, Unpooled.EMPTY_BUFFER);

        channel.writeInbound(frame);
        TFrame newFrame = channel.readOutbound();

        assertNotNull(newFrame);
        assertEquals(frame.size, newFrame.size);
        assertEquals(MessageType.PingResponse.byteValue(), newFrame.type);
        assertEquals(frame.id, newFrame.id);

    }

}
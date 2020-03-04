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
package com.uber.tchannel.codecs;

import com.google.common.base.Charsets;
import com.uber.tchannel.frames.FrameType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TFrameCodecTest {

    @Test
    public void shouldEncodeAndDecodeFrame() {

        EmbeddedChannel channel = new EmbeddedChannel(
            new TChannelLengthFieldBasedFrameDecoder(),
            new TFrameCodec()
        );

        String payload = "Hello, World!";
        ByteBuf buffer = Unpooled.wrappedBuffer(payload.getBytes(StandardCharsets.UTF_8));

        TFrame frame = new TFrame(
            payload.getBytes(StandardCharsets.UTF_8).length,
            FrameType.InitRequest,
            Integer.MAX_VALUE,
            buffer
        );

        channel.writeOutbound(frame);
        channel.writeInbound(channel.readOutbound());

        TFrame newFrame = channel.readInbound();
        assertNotNull(newFrame);
        assertEquals(frame.size, newFrame.size);
        assertEquals(frame.type, newFrame.type);
        assertEquals(frame.id, newFrame.id);
        newFrame.release();

    }

    @Test
    public void encodeWithoutError() {
        ByteBuf allocatedByteBuf1 = Unpooled.buffer(TFrame.FRAME_SIZE_LENGTH);
        ByteBufAllocator allocator = Mockito.mock(ByteBufAllocator.class);
        when(allocator.buffer(TFrame.FRAME_HEADER_LENGTH, TFrame.FRAME_HEADER_LENGTH)).thenReturn(allocatedByteBuf1);

        String payload = "Hello, World!";
        ByteBuf buffer = Unpooled.wrappedBuffer(payload.getBytes(StandardCharsets.UTF_8));
        TFrame frame = new TFrame(
            10,
            FrameType.InitRequest,
            Integer.MAX_VALUE,
            buffer
        );
        TFrameCodec.encode(allocator, frame);

        verify(allocator, times(1)).buffer(TFrame.FRAME_HEADER_LENGTH, TFrame.FRAME_HEADER_LENGTH);
        assertEquals(1, allocatedByteBuf1.refCnt());
    }

    @Test
    public void encodeWithError() {
        ByteBuf allocatedByteBuf1 = Unpooled.buffer(TFrame.FRAME_SIZE_LENGTH);
        ByteBufAllocator allocator = Mockito.mock(ByteBufAllocator.class);
        when(allocator.buffer(TFrame.FRAME_HEADER_LENGTH, TFrame.FRAME_HEADER_LENGTH)).thenReturn(allocatedByteBuf1);

        CompositeByteBuf buffer = Mockito.mock(CompositeByteBuf.class);
        when(buffer.writerIndex(anyInt())).thenThrow(new RuntimeException("Can't write"));
        TFrame frame = new TFrame(
            10,
            FrameType.InitRequest,
            Integer.MAX_VALUE,
            buffer
        );
        try {
            TFrameCodec.encode(allocator, frame);
            fail();
        } catch (Exception e) {
            assertEquals("Can't write", e.getMessage());
        }

        verify(allocator, times(1)).buffer(TFrame.FRAME_HEADER_LENGTH, TFrame.FRAME_HEADER_LENGTH);
        assertEquals(0, allocatedByteBuf1.refCnt());
    }

    @Test
    public void decodeWithErrorDoesntRetainExtraCopy() throws Exception {
        ByteBuf input = Unpooled.buffer(TFrame.FRAME_SIZE_LENGTH);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        assertEquals(1, input.refCnt());
        try {
            new TFrameCodec().decode(ctx, input, new ArrayList<Object>());
            fail();
        } catch (IndexOutOfBoundsException e) {
            assertTrue(e
                .getMessage()
                .contains(
                    "readerIndex(0) + length(2) exceeds writerIndex(0): "));
        }

        assertEquals(1, input.refCnt());
    }

    @Test
    public void decodeWithoutErrorRetainsExtraCopy() throws Exception {
        ByteBuf input = Unpooled.buffer(2 * TFrame.FRAME_HEADER_LENGTH);
        byte[] payload = "hello".getBytes(Charsets.US_ASCII);
        input.writeShort((payload.length + TFrame.FRAME_HEADER_LENGTH)); // payload size
        input.writeByte(1); // type
        input.writeByte(1); // reserved
        input.writeInt(25); // id
        input.writeBytes(new byte[]{0,0,0,0,0,0,0,0}); //reserved
        input.writeBytes(payload); //payload

        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);

        assertEquals(1, input.refCnt());
        ArrayList<Object> out = new ArrayList<>();
        new TFrameCodec().decode(ctx, input, out);

        assertEquals(1, out.size());
        assertEquals("hello", ((TFrame)out.get(0)).payload.toString(Charsets.US_ASCII));
        assertEquals(2, input.refCnt());
    }

}

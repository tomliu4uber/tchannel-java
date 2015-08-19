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

import com.uber.tchannel.errors.FatalProtocolError;
import com.uber.tchannel.errors.ProtocolError;
import com.uber.tchannel.errors.ProtocolErrorProcessor;
import com.uber.tchannel.messages.InitMessage;
import com.uber.tchannel.messages.InitRequest;
import com.uber.tchannel.messages.InitResponse;
import com.uber.tchannel.messages.Message;
import com.uber.tchannel.tracing.Trace;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class InitRequestHandler extends ChannelHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object object) throws Exception {

        Message message = (Message) object;

        switch (message.getMessageType()) {

            case InitRequest:

                InitRequest initRequestMessage = (InitRequest) message;

                if (initRequestMessage.getVersion() == InitMessage.DEFAULT_VERSION) {
                    ChannelFuture f = ctx.writeAndFlush(new InitResponse(
                            initRequestMessage.getId(),
                            InitMessage.DEFAULT_VERSION,
                            initRequestMessage.getHeaders()
                    ));
                    f.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    ctx.pipeline().remove(this);
                } else {
                    throw new FatalProtocolError(
                            new Trace(0, 0, 0, (byte) 0x00),
                            String.format("Expected Protocol version: %d", InitMessage.DEFAULT_VERSION)
                    );
                }

                break;

            default:

                throw new FatalProtocolError(
                        new Trace(0, 0, 0, (byte) 0x00),
                        "Must not send any data until receiving Init Request"
                );

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

        if (cause instanceof ProtocolError) {
            ProtocolError protocolError = (ProtocolError) cause;
            ProtocolErrorProcessor.handleError(ctx, protocolError);
        } else {
            super.exceptionCaught(ctx, cause);
        }

    }
}

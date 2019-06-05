/**
 * Copyright (C) 2009-2010 Wilfred Springer
 *
 * This file is part of Preon.
 *
 * Preon is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * Preon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Preon; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.codehaus.preon.codec;

import static org.codehaus.preon.buffer.ByteOrder.BigEndian;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.codehaus.preon.Builder;
import org.codehaus.preon.Codec;
import org.codehaus.preon.CodecFactory;
import org.codehaus.preon.Codecs;
import org.codehaus.preon.DecodingException;
import org.codehaus.preon.Resolver;
import org.codehaus.preon.ResolverContext;
import org.codehaus.preon.annotation.BoundNumber;
import org.codehaus.preon.annotation.BoundObject;
import org.codehaus.preon.annotation.Choices;
import org.codehaus.preon.annotation.Choices.Choice;
import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.buffer.ByteOrder;
import org.codehaus.preon.buffer.DefaultBitBuffer;
import org.codehaus.preon.el.Expressions;

/**
 * A collection of tests for the {@link SelectFromCodec}.
 *
 * @author Wilfred Springer (wis)
 */
public class SelectFromCodecTest extends TestCase {

    private Choices choices;

    private ResolverContext context;

    private AnnotatedElement metadata;

    private CodecFactory codecFactory;

    private Codec floatCodec;

    private Codec integerCodec;

    private Codec shortCodec;

    private Resolver resolver;

    private Builder builder;

    public void setUp() {
        context = createMock(ResolverContext.class);
        codecFactory = createMock(CodecFactory.class);
        metadata = createMock(AnnotatedElement.class);
        floatCodec = createMock(Codec.class);
        integerCodec = createMock(Codec.class);
        shortCodec = createMock(Codec.class);
        resolver = createMock(Resolver.class);
        builder = createMock(Builder.class);
        choices = new Choices() {

            public Choice[] alternatives() {
                return new Choice[]{new Choice() {

                    public String condition() {
                        return "prefix==0";
                    }

                    public Class<?> type() {
                        return Integer.class;
                    }

                    public Class<? extends Annotation> annotationType() {
                        return Choice.class;
                    }

                }, new Choice() {

                    public String condition() {
                        return "prefix==1";
                    }

                    public Class<?> type() {
                        return Short.class;
                    }

                    public Class<? extends Annotation> annotationType() {
                        return Choice.class;
                    }

                }};
            }

            public ByteOrder byteOrder() {
                return ByteOrder.BigEndian;
            }

            public Class<?> defaultType() {
                return Float.class;
            }

            public int prefixSize() {
                return 8;
            }

            public Class<? extends Annotation> annotationType() {
                return Choices.class;
            }

        };
    }

    public void testSelectFrom() throws DecodingException {
        BitBuffer buffer = new DefaultBitBuffer(ByteBuffer.wrap(new byte[]{0, 1, (byte) 255,
                (byte) 255, (byte) 255, (byte) 255}));

        // We expect all Codecs to be constructed
        expect(codecFactory.create(null, Float.class, context)).andReturn(floatCodec);
        expect(
                codecFactory.create((AnnotatedElement) isNull(), eq(Integer.class),
                        isA(ResolverContext.class))).andReturn(integerCodec);
        expect(
                codecFactory.create((AnnotatedElement) isNull(), eq(Short.class),
                        isA(ResolverContext.class))).andReturn(shortCodec);
        expect(integerCodec.decode(buffer, resolver, builder)).andReturn(new Integer(3));
        expect(integerCodec.getSize()).andReturn(Expressions.createInteger(32, Resolver.class));
        expect(shortCodec.getSize()).andReturn(Expressions.createInteger(16, Resolver.class));
        expect(shortCodec.decode(buffer, resolver, builder)).andReturn(new Short((short) 5));

        // Replay
        replay(codecFactory, context, metadata, shortCodec, integerCodec, floatCodec, resolver,
                builder);
        SelectFromCodec codec = new SelectFromCodec(Number.class, choices, context, codecFactory,
                metadata);

        // If we can potentially decode a Float, as well as an Integer, or a Short, then we cannot really predict the size.
        assertNull(codec.getSize());

        // Decode first value
        Object value = codec.decode(buffer, resolver, builder);
        assertNotNull(value);
        assertEquals(3, ((Integer) value).intValue());

        // Decode second value
        value = codec.decode(buffer, resolver, builder);
        assertNotNull(value);
        assertEquals(5, ((Short) value).intValue());

        // Verify
        verify(codecFactory, context, metadata, shortCodec, integerCodec, floatCodec, resolver,
                builder);
    }
    
    public void testEncode() throws IOException, DecodingException {
        class Command {
        }
        class Run extends Command {
            @BoundNumber(size = "16", byteOrder = BigEndian) int speed;
            @BoundNumber(size = "8")                         int direction;
            Run(int s, int d) { speed = s; direction = d; }
        }
        class Hide extends Command {
            @BoundNumber(size = "32", byteOrder = BigEndian) int safehouse;
            Hide(int s) { safehouse = s; }
        }
        class Message {
            @BoundObject(selectFrom = @Choices(
                    prefixSize = 16, byteOrder = BigEndian, alternatives = {
                    @Choice(condition = "prefix == 0x08", type = Run.class),
                    @Choice(condition = "prefix == 0x18", type = Hide.class) }))
            Command cmd;
            Message(Command c) { cmd = c; }
        }
        Codec<Message> msgCodec = Codecs.create(Message.class);
        
        Message msg = new Message(new Run(30, 5));
        byte[] data = Codecs.encode(msg, msgCodec);
        assertArrayEquals(new byte[] { 0, 8, 0, 30, 5 }, data);
        
        msg = new Message(new Hide(101));
        data = Codecs.encode(msg, msgCodec);
        assertArrayEquals(new byte[] { 0, 0x18, 0, 0, 0, 101 }, data);
    }
}

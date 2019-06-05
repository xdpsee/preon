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

import org.codehaus.preon.el.Document;
import org.codehaus.preon.el.Expression;
import org.codehaus.preon.Builder;
import org.codehaus.preon.DecodingException;
import org.codehaus.preon.Resolver;
import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.buffer.ByteOrder;
import org.codehaus.preon.channel.BitChannel;
import org.codehaus.preon.codec.NumericCodec;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class NumericCodecTest {

    @Mock
    private Resolver resolver;

    @Mock
    private Expression<Integer, Resolver> size;

    @Mock
    private Expression<Integer, Resolver> matchExpression;

    @Mock
    private BitChannel channel;
    
    @Mock
    private BitBuffer buffer;
    
    @Mock
    private NumericCodec.NumericType type;

    @Mock
    private Builder builder;

    @Test
    public void shouldEncodeCorrectly() throws IOException {
        NumericCodec codec = new NumericCodec(size, ByteOrder.BigEndian, NumericCodec.NumericType.Long, matchExpression);
        when(size.eval(resolver)).thenReturn(3);
        codec.encode(new Long(12L), channel, resolver);
        Mockito.verify(channel).write(3, 12L, ByteOrder.BigEndian);
    }
    
    @Test
    public void shouldMatchCorrectly() throws IOException, DecodingException {
        int bits = 3;
        Integer value = 12;
        NumericCodec codec = new NumericCodec(size, ByteOrder.BigEndian, type, matchExpression);
        when(size.eval(resolver)).thenReturn(bits);
        when(type.decode(buffer, bits, ByteOrder.BigEndian)).thenReturn(value);
        when(matchExpression.eval(resolver)).thenReturn(value);
        assertThat((Integer) codec.decode(buffer, resolver, builder), is(value));
    }
}

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
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.codehaus.preon.Codec;
import org.codehaus.preon.Codecs;
import org.codehaus.preon.DecodingException;
import org.codehaus.preon.DefaultBuilder;
import org.codehaus.preon.annotation.BoundBitField;
import org.codehaus.preon.annotation.BoundEnumOption;
import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.buffer.DefaultBitBuffer;
import org.junit.Test;

/**
 * @author Jerzy Smyczek
 */
public class BitFieldCodecTest {
    
    public static enum Day {
        @BoundEnumOption(0x01) MON,
        @BoundEnumOption(0x02) TUE,
        @BoundEnumOption(0x04) WED,
        @BoundEnumOption(0x08) THU,
        @BoundEnumOption(0x10) FRI,
        @BoundEnumOption(0x20) SAT,
        @BoundEnumOption(0x40) SUN
    }
    
    public static class Bean {
        @BoundBitField(size = "16", byteOrder = BigEndian, type = Day.class)
        Set<Day> days = new HashSet<Day>();
    }

    @Test
    public void testEncode() throws IOException {
        Bean bean = new Bean();
        bean.days.addAll(Arrays.asList(Day.TUE, Day.THU, Day.SAT, Day.SUN));
        Codec<Bean> codec = Codecs.create(Bean.class);
        
        byte[] data = Codecs.encode(bean, codec);
        assertEquals(2, data.length);
        assertEquals(0, data[0]);
        assertEquals(0x02 | 0x08 | 0x20 | 0x40, data[1]);
    }
    
    @Test
    public void testDecode() throws DecodingException {
        byte[] data = { 0x00, 0x01 | 0x04 | 0x10 };
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        BitBuffer buffer = new DefaultBitBuffer(byteBuffer);

        Codec<Bean> codec = Codecs.create(Bean.class);
        Bean bean = codec.decode(buffer, null, new DefaultBuilder());
        Set<Day> expected = new HashSet<Day>(Arrays.asList(Day.MON, Day.WED, Day.FRI));
        assertEquals(expected, bean.days);
    }
}

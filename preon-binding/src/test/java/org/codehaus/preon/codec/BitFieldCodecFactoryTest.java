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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.when;

import java.lang.reflect.AnnotatedElement;
import java.util.Set;

import org.codehaus.preon.Codec;
import org.codehaus.preon.ResolverContext;
import org.codehaus.preon.annotation.BoundBitField;
import org.codehaus.preon.annotation.BoundEnumOption;
import org.codehaus.preon.buffer.ByteOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BitFieldCodecFactoryTest {
    
    enum Access {
        @BoundEnumOption(1) READ,
        @BoundEnumOption(2) WRITE,
    }
    
    BitFieldCodecFactory factory;
    
    @Mock
    BoundBitField annotation;
    
    @Mock
    AnnotatedElement metadata;
    
    @Mock
    ResolverContext context;
    
    @Before
    public void before() {
        factory = new BitFieldCodecFactory();
    }

    @Test
    public void test() {
        when(metadata.isAnnotationPresent(BoundBitField.class)).thenReturn(true);
        when(metadata.getAnnotation(BoundBitField.class)).thenReturn(annotation);
        when(annotation.type()).thenReturn((Class) Access.class);
        when(annotation.byteOrder()).thenReturn(ByteOrder.BigEndian);
        when(annotation.size()).thenReturn("16");
        
        Codec<Set> codec = factory.create(metadata, Set.class, context);
        assertThat(codec, is(not(nullValue())));
    }
}

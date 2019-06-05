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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import nl.flotsam.pecia.Documenter;
import nl.flotsam.pecia.Para;
import nl.flotsam.pecia.ParaContents;
import nl.flotsam.pecia.SimpleContents;

import org.codehaus.preon.Builder;
import org.codehaus.preon.Codec;
import org.codehaus.preon.CodecDescriptor;
import org.codehaus.preon.DecodingException;
import org.codehaus.preon.Resolver;
import org.codehaus.preon.annotation.BoundEnumOption;
import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.buffer.ByteOrder;
import org.codehaus.preon.channel.BitChannel;
import org.codehaus.preon.descriptor.Documenters;
import org.codehaus.preon.el.Expression;

/**
 * Decodes bit fields as sets of enums, taking bit values from {@link BoundEnumOption} annotations.
 * @author Jerzy Smyczek
 */
public class BitFieldCodec<E extends Enum<E>> implements Codec<Set<E>> {

    /** Maps enums to bits. */
    private Map<E, Long> enumBits = new HashMap<E, Long>();

    /** Size of resulting value in bits. */
    private Expression<Integer, Resolver> sizeExpr;

    /** Endianness of resulting value. */
    private ByteOrder byteOrder;

    /** Unqualified name of enum class. */
    private String enumName;

    /**
     * Initialized the codec, extracting values from enum class.
     * @param enumClass type of enum held by encoded set
     * @param sizeExpr number of bits in encoded numeric value
     * @param byteOrder endianness
     */
    public BitFieldCodec(Class<E> enumClass, Expression<Integer, Resolver> sizeExpr, ByteOrder byteOrder) {
        this.sizeExpr = sizeExpr;
        this.byteOrder = byteOrder;
        this.enumName = enumClass.getSimpleName();

        for (E enumValue : enumClass.getEnumConstants()) {
            enumBits.put(enumValue, getBit(enumValue));
        }
    }

    /**
     * Decodes bit-field, matching it against bit of each enum value.
     * @return set of enum values matching the bit-field
     */
    @Override
    public Set<E> decode(BitBuffer buffer, Resolver resolver, Builder builder) throws DecodingException {
        int size = sizeExpr.eval(resolver);
        long bitField = buffer.readAsLong(size, byteOrder);
        Set<E> enumSet = new HashSet<E>();

        for (Entry<E, Long> entry : enumBits.entrySet()) {
            if ((bitField & entry.getValue()) != 0) {
                enumSet.add(entry.getKey());
            }
        }
        return enumSet;
    }

    /**
     * Encodes set of enums, setting a bit of output numeric value for each enum in the set.
     */
    @Override
    public void encode(Set<E> enumSet, BitChannel channel, Resolver resolver) throws IOException {
        long bitField = 0;
        for (E enumValue : enumSet) {
            bitField |= enumBits.get(enumValue);
        }
        int size = sizeExpr.eval(resolver);
        channel.write(size, bitField, byteOrder);
    }

    @Override
    public Expression<Integer, Resolver> getSize() {
        return sizeExpr;
    }

    @Override
    public Class<?>[] getTypes() {
        return new Class[] { Set.class };
    }

    @Override
    public Class<?> getType() {
        return Set.class;
    }

    private long getBit(E enumValue) {
        try {
            Field field = enumValue.getDeclaringClass().getField(enumValue.name());
            if (field.isAnnotationPresent(BoundEnumOption.class)) {
                return field.getAnnotation(BoundEnumOption.class).value();
            } else {
                throw new IllegalArgumentException(enumValue + " is missing BoundEnumOption annotation");
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public CodecDescriptor getCodecDescriptor() {
        return new CodecDescriptor() {
            public <C extends SimpleContents<?>> Documenter<C> details(String bufferReference) {
                return new Documenter<C>() {
                    public void document(C target) {
                        Para<?> para = target.para();
                        
                        if (!sizeExpr.isParameterized()) {
                            para.text("The set of ").text(enumName).text(" symbols is represented as a ")
                                    .document(Documenters.forNumericValue(sizeExpr.eval(null), byteOrder)).text(".");
                        } else {
                            para.text("The set of ").text(enumName).text(" values is represented as a numeric value (")
                                    .document(Documenters.forByteOrder(byteOrder)).text(". The number of bits is ")
                                    .document(Documenters.forExpression(sizeExpr)).text(".");
                        }
                        para.text(" The numeric value is a combination of values corresponding to the following symbols:").end();
                        
                        SortedMap<Long, E> sortedBits = new TreeMap<Long, E>();
                        for (Entry<E, Long> e : enumBits.entrySet()) {
                            sortedBits.put(e.getValue(), e.getKey());
                        }
                        for (Map.Entry<Long, E> entry : sortedBits.entrySet()) {
                            String val = String.format("0x%02X", entry.getKey());
                            target.para().text(val).text(": ").text(entry.getValue().name()).end();
                        }
                    }
                };
            }

            public <C extends ParaContents<?>> Documenter<C> reference(final Adjective adjective,
                    boolean startWithCapital) {
                return new Documenter<C>() {
                    public void document(C target) {
                        target.text(adjective.asTextPreferAn(false)).text("index of an enumeration");
                    }
                };
            }

            public <C extends ParaContents<?>> Documenter<C> summary() {
                return new Documenter<C>() {
                    public void document(C target) {
                        target.text("A set of ").text(enumName).text(" symbols, represented by a numeric value.");
                    }
                };
            }

            @Override
            public boolean requiresDedicatedSection() {
                return false;
            }

            @Override
            public String getTitle() {
                return null;
            }
        };
    }
}

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

import org.codehaus.preon.Codec;
import org.codehaus.preon.CodecFactory;
import org.codehaus.preon.ResolverContext;
import org.codehaus.preon.annotation.BoundList;
import org.codehaus.preon.annotation.Choices;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Map;

public class MapCodecFactory implements CodecFactory {

    private final CodecFactory codecFactory;

    public MapCodecFactory(CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    public <T> Codec<T> create(AnnotatedElement metadata, Class<T> type, ResolverContext context) {
        if (Map.class.isAssignableFrom(type)) {
            BoundList boundList = metadata.getAnnotation(BoundList.class);
            if (boundList != null && typeIsGuaranteedToBeEntry(boundList)) {
                Codec<List> listCodec =
                        codecFactory.create(metadata, List.class, context);
                if (listCodec != null) {
                    return new ListBasedMapCodec(listCodec);
                }  else {
                    return null;
                }
            }
        }
        return null;
    }

    private boolean typeIsGuaranteedToBeEntry(BoundList boundList) {
        if (boundList.type() != null && Map.Entry.class.isAssignableFrom(boundList.type())) {
            return true;
        } else if (boundList.types() != null && boundList.types().length > 0) {
            boolean allGood = true;
            for (Class<?> type : boundList.types()) {
                allGood &= (type != null && Map.Entry.class.isAssignableFrom(type));
            }
            return allGood;
        } else if (boundList.selectFrom() != null && boundList.selectFrom().alternatives().length > 0) {
            boolean allGood = true;
            for (Choices.Choice choice : boundList.selectFrom().alternatives()) {
                allGood &= (choice != null && Map.Entry.class.isAssignableFrom(choice.type()));
            }
            return allGood;
        }
        return false;
    }

}

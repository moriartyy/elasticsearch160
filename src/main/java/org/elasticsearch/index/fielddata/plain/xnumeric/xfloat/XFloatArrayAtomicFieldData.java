/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.fielddata.plain.xnumeric.xfloat;

import org.apache.lucene.index.RandomAccessOrds;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.util.ObjectArray;
import org.elasticsearch.index.fielddata.ordinals.Ordinals;

public class XFloatArrayAtomicFieldData extends AtomicXFloatFieldData  {

    private Ordinals ordinals;
    private ObjectArray<Object> values;

    public XFloatArrayAtomicFieldData(ObjectArray<Object> values, Ordinals ordinals) {
        this.values = values;
        this.ordinals = ordinals;
    }

    @Override
    public MultiXFloatValues getValues() {
        
        final RandomAccessOrds ords = this.ordinals.ordinals();
        
        return new MultiXFloatValues() {

            @Override
            public void setDocument(int docID) {
                ords.setDocument(docID);
            }

            @Override
            public int count() {
                return ords.cardinality();
            }

            @Override
            public XFloat valueAt(int index) {
                return (XFloat) values.get(ords.ordAt(index));
            }
            
        };
    }

    @Override
    public long ramBytesUsed() {
        return values.ramBytesUsed() + ordinals.ramBytesUsed();
    }

    @Override
    public void close() throws ElasticsearchException {
        
    }

}

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

package org.elasticsearch.index.fielddata.plain.xnumeric;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.index.fielddata.SortedBinaryDocValues;
import org.elasticsearch.index.fielddata.SortingBinaryDocValues;

public abstract class AbstractAtomicXNumericFieldData<T extends MultiXNumericValues<?>> implements AtomicXNumericFieldData<T> {

    @Override
    public final SortedBinaryDocValues getBytesValues() {
        final T cfValues = getValues();
        return new SortingBinaryDocValues() {

            final List<CharSequence> list = new ArrayList<>();

            @Override
            public void setDocument(int docID) {
                list.clear();
                cfValues.setDocument(docID);
                for (int i = 0, count = cfValues.count(); i < count; ++i) {
                    list.add(cfValues.valueAt(i).toString());
                }
                count = list.size();
                grow();
                for (int i = 0; i < count; ++i) {
                    final CharSequence s = list.get(i);
                    values[i].copyChars(s);
                }
                sort();
            }

        };
    }

    @Override
    public final ScriptDocValues.GeoPoints getScriptValues() {
        throw new UnsupportedOperationException();
    }
}

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

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.apache.lucene.util.packed.PackedInts;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.BigArrays;
import org.elasticsearch.common.util.ObjectArray;
import org.elasticsearch.common.breaker.CircuitBreaker;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.IndexFieldDataCache;
import org.elasticsearch.index.fielddata.ordinals.Ordinals;
import org.elasticsearch.index.fielddata.ordinals.OrdinalsBuilder;
import org.elasticsearch.index.fielddata.plain.NonEstimatingEstimator;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MapperService;
import org.elasticsearch.index.mapper.FieldMapper.Names;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.indices.breaker.CircuitBreakerService;


public class XFloatArrayIndexFieldData extends XFloatIndexFieldData {
    
    private final CircuitBreakerService breakerService;
    
    public static class Builder implements IndexFieldData.Builder {

        @Override
        public IndexFieldData<?> build(Index index, @IndexSettings Settings indexSettings, FieldMapper<?> mapper,
                                                               IndexFieldDataCache cache, CircuitBreakerService breakerService, MapperService mapperService) {
            return new XFloatArrayIndexFieldData(index, indexSettings, mapper.names(), mapper.fieldDataType(), cache, breakerService);
        }
    }

    public XFloatArrayIndexFieldData(Index index, Settings indexSettings, Names fieldNames,
            FieldDataType fieldDataType, IndexFieldDataCache cache, CircuitBreakerService breakerService) {
        super(index, indexSettings, fieldNames, fieldDataType, cache);
        this.breakerService = breakerService;
    }

    @Override
    public AtomicXFloatFieldData loadDirect(AtomicReaderContext context) throws Exception {
        AtomicReader reader = context.reader();

        Terms terms = reader.terms(getFieldNames().indexName());
        AtomicXFloatFieldData data = null;
        // TODO: Use an actual estimator to estimate before loading.
        NonEstimatingEstimator estimator = new NonEstimatingEstimator(breakerService.getBreaker(CircuitBreaker.Name.FIELDDATA));
        if (terms == null) {
            data = AtomicXFloatFieldData.empty(reader.maxDoc());
            estimator.afterLoad(null, data.ramBytesUsed());
            return data;
        }
        
        final long numTerms = terms.size();
        final float acceptableTransientOverheadRatio = PackedInts.FAST;

        TermsEnum termsEnum = terms.iterator(null);
        boolean success = false;
        
        ObjectArray<Object> values = BigArrays.NON_RECYCLING_INSTANCE.newObjectArray(numTerms);

        try (OrdinalsBuilder builder = new OrdinalsBuilder(numTerms, reader.maxDoc(), acceptableTransientOverheadRatio)) {
            DocsEnum docsEnum = null;
            TermToFloatSet termToFloat = new TermToFloatSet();
            for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {
                long termOrd = builder.nextOrdinal();
                values.set(termOrd, termToFloat.parse(term));
                docsEnum = termsEnum.docs(null, docsEnum, DocsEnum.FLAG_NONE);
                for (int docId = docsEnum.nextDoc(); docId != DocsEnum.NO_MORE_DOCS; docId = docsEnum.nextDoc()) {
                    builder.addDoc(docId);
                }
            }
            
            final Ordinals ordinals = builder.build(fieldDataType.getSettings());

            data = new XFloatArrayAtomicFieldData(values, ordinals);
            success = true;
            
            return data;
        } finally {
            if (!success) {
                // If something went wrong, unwind any current estimations we've made
                estimator.afterLoad(termsEnum, 0);
            } else {
                // Call .afterLoad() to adjust the breaker now that we have an exact size
                estimator.afterLoad(termsEnum, data.ramBytesUsed());
            }

        }
        
    }

    static class TermToFloatSet {
        
        CharsRefBuilder spare = new CharsRefBuilder();
        
        public TermToFloatSet() {
        }
        
        public XFloat parse(BytesRef term) {
            XFloat floatSet = new XFloat();
            spare.copyUTF8Bytes(term);
            int separaterPos = 0;
            for (int i=0; i<spare.length(); i++) {
                if (spare.charAt(i) == '|') {
                    separaterPos = i;
                    break;
                }
            }
            if (separaterPos == 0) {
                floatSet.defaultVal(Float.parseFloat(new String(spare.chars(), 0, spare.length())));
            } else {
                floatSet.defaultVal(Float.parseFloat(new String(spare.chars(), 0, separaterPos)));
                
                int offset = separaterPos + 1;
                int id = 0;
                float v;
                for (int i=separaterPos + 1; i<spare.length(); i++) {
                    if (spare.charAt(i) == ':') {
                        id = Integer.parseInt(new String(spare.chars(), offset, i - offset));
                        offset = i + 1;
                    } else if (spare.charAt(i) == ',') {
                        v = Float.parseFloat(new String(spare.chars(), offset, i - offset));
                        floatSet.add(id, v);
                        offset = i + 1;
                    }
                }
                if (id > 0) { // means map value exist.
                    v = Float.parseFloat(new String(spare.chars(), offset, spare.length() - offset)); 
                    floatSet.add(id, v);
                }
            }

            return floatSet;
        }
     }
}

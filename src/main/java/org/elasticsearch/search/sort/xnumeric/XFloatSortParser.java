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

package org.elasticsearch.search.sort.xnumeric;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.FieldCache.Floats;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.plain.xnumeric.xfloat.XFloatIndexFieldData;
import org.elasticsearch.index.fielddata.plain.xnumeric.xfloat.MultiXFloatValues;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.sort.SortParser;

import java.io.IOException;
import java.util.BitSet;

public class XFloatSortParser implements SortParser {

    @Override
    public String[] names() {
        return new String[] { "_xfloat" };
    }

    @Override
    public SortField parse(XContentParser parser, SearchContext context) throws Exception {
        String fieldName = null;
        boolean reverse = false;
        final BitSet bitSet = new BitSet();
        
        XContentParser.Token token;
        String currentName = parser.currentName();
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentName = parser.currentName();
            } else if (token == XContentParser.Token.START_ARRAY) {
                while (!parser.nextToken().equals(XContentParser.Token.END_ARRAY)) {
                    if (parser.currentToken() == XContentParser.Token.VALUE_NUMBER) {
                        bitSet.set(parser.intValue());
                    }
                }
                fieldName = currentName;
            } else if (token.isValue()) {
                if ("reverse".equals(currentName)) {
                    reverse = parser.booleanValue();
                } else if ("order".equals(currentName)) {
                    reverse = "desc".equals(parser.text());
                } else {
                    fieldName = currentName;
                }
            }
        }

        FieldMapper<?> mapper = context.smartNameFieldMapper(fieldName);
        if (mapper == null) {
            throw new ElasticsearchIllegalArgumentException("failed to find mapper for [" + fieldName + "] for geo distance based sort");
        }
        
        final XFloatIndexFieldData fieldData = context.fieldData().getForField(mapper);

        IndexFieldData.XFieldComparatorSource comparatorSource = new IndexFieldData.XFieldComparatorSource() {

            @Override
            public SortField.Type reducedType() {
                return SortField.Type.FLOAT;
            }

            @Override
            public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
                return new FieldComparator.FloatComparator(numHits, null, null, null) {
                    
                    @Override
                    protected Floats getFloatValues(AtomicReaderContext context, String field) throws IOException {
                        final MultiXFloatValues floatSetValues = fieldData.load(context).getValues();
                        final BitSet flags = bitSet;
                        return new Floats() {
                            
                            @Override
                            public float get(int docID) {
                                floatSetValues.setDocument(docID);
                                return floatSetValues.valueAt(0).value(flags);
                            }
                        };
                    }
                };
            }

        };

        return new SortField(fieldName, comparatorSource, reverse);
    }
}

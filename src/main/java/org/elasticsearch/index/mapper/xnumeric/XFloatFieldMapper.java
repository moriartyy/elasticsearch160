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

package org.elasticsearch.index.mapper.xnumeric;

import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.codec.docvaluesformat.DocValuesFormatProvider;
import org.elasticsearch.index.codec.postingsformat.PostingsFormatProvider;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.mapper.*;
import org.elasticsearch.index.mapper.core.AbstractFieldMapper;
import org.elasticsearch.index.mapper.internal.AllFieldMapper;
import org.elasticsearch.index.similarity.SimilarityProvider;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.mapper.core.TypeParsers;

/**
 *
 */
public class XFloatFieldMapper extends AbstractFieldMapper<String> implements AllFieldMapper.IncludeInAll {

    public static final String CONTENT_TYPE = "xfloat";

    public static class Defaults extends AbstractFieldMapper.Defaults {
        public static final FieldType FIELD_TYPE = new FieldType(AbstractFieldMapper.Defaults.FIELD_TYPE);

        static {
            FIELD_TYPE.setIndexed(true);
            FIELD_TYPE.setTokenized(false);
            FIELD_TYPE.setOmitNorms(true);
            FIELD_TYPE.setIndexOptions(IndexOptions.DOCS_ONLY);
            FIELD_TYPE.freeze();
        }
    }

    public static class Builder extends AbstractFieldMapper.Builder<Builder, XFloatFieldMapper> {


        public Builder(String name) {
            super(name, new FieldType(Defaults.FIELD_TYPE));
            builder = this;
        }

        @Override
        public XFloatFieldMapper build(BuilderContext context) {
            
            XFloatFieldMapper fieldMapper = new XFloatFieldMapper(buildNames(context), fieldType, indexAnalyzer, 
                    searchAnalyzer, postingsProvider, docValuesProvider, similarity, normsLoading, fieldDataSettings, 
                    context.indexSettings(), multiFieldsBuilder.build(this, context), copyTo);
            fieldMapper.includeInAll(false);
            return fieldMapper;
        }
    }

    public static class TypeParser implements Mapper.TypeParser {
        
        @Override
        public Mapper.Builder<?, ?> parse(String name, Map<String, Object> node, ParserContext parserContext) throws MapperParsingException {
            Builder builder = new Builder(name);
            TypeParsers.parseField(builder, name, node, parserContext);
            return builder;
        }
    }

    
    protected XFloatFieldMapper(Names names, FieldType fieldType, NamedAnalyzer indexAnalyzer, NamedAnalyzer searchAnalyzer, 
            PostingsFormatProvider postingsFormat, DocValuesFormatProvider docValuesFormat,
            SimilarityProvider similarity, Loading normsLoading, @Nullable Settings fieldDataSettings, 
            Settings indexSettings, MultiFields multiFields, CopyTo copyTo) {
        super(names, 1.0f, fieldType, false, indexAnalyzer, searchAnalyzer, postingsFormat, docValuesFormat, 
                similarity, normsLoading, fieldDataSettings, indexSettings, multiFields, copyTo);
    }

    @Override
    public FieldType defaultFieldType() {
        return fieldType;
    }

    @Override
    public FieldDataType defaultFieldDataType() {
        return new FieldDataType(CONTENT_TYPE);
    }

    @Override
    public String value(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    @Override
    protected void parseCreateField(ParseContext context, List<Field> fields) throws IOException {
        XFloatForMapper xfloat = parseCreateFieldForFloatSet(context);
        Field field = new Field(names.indexName(), xfloat.string(), fieldType);
        fields.add(field);
    }
    
    static class XFloatForMapper {
        
        String map;
        float defaultVal;
        
        XFloatForMapper(String map) {
            this(map, 0f);
        }
        
          public String string() {
              if (map == null || map == "") {
                  return String.valueOf(defaultVal);
              } else {
                  return String.valueOf(defaultVal) + "|" + map;
              }
        }

        XFloatForMapper(String map, float defaultVal) {
            this.map = map;
            this.defaultVal = defaultVal;
        }
    }
    
    public static XFloatForMapper parseCreateFieldForFloatSet(ParseContext context) throws IOException {
        if (context.externalValueSet()) {
            return new XFloatForMapper((String) context.externalValue());
        }
        XContentParser parser = context.parser();
        if (parser.currentToken() == XContentParser.Token.VALUE_NULL) {
            return new XFloatForMapper(null);
        }
        if (parser.currentToken() == XContentParser.Token.START_OBJECT) {
            XContentParser.Token token;
            String currentFieldName = null;
            String map = null;
            float defaultVal = 0f;
            while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
                if (token == XContentParser.Token.FIELD_NAME) {
                    currentFieldName = parser.currentName();
                } else {
                    if ("map".equals(currentFieldName) || "_map".equals(currentFieldName)) {
                        map = parser.textOrNull();
                    } else if ("default".equals(currentFieldName) || "_default".equals(currentFieldName)) {
                        defaultVal = parser.floatValue();
                    } else {
                        throw new ElasticsearchIllegalArgumentException("unknown property [" + currentFieldName + "]");
                    }
                }
            }
            return new XFloatForMapper(map, defaultVal);
        }
        return new XFloatForMapper(parser.textOrNull(), 0f);
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    public void includeInAll(Boolean includeInAll) {
        
    }

    @Override
    public void includeInAllIfNotSet(Boolean includeInAll) {
        
    }

    @Override
    public void unsetIncludeInAll() {
        
    }
}

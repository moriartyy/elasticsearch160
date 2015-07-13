package org.elasticsearch.ext.multivaluenumeric;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.FieldCache.Doubles;
import org.apache.lucene.search.FieldCache.Floats;
import org.apache.lucene.util.FixedBitSet;
import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.common.geo.GeoDistance;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.geo.GeoUtils;
import org.elasticsearch.common.geo.GeoDistance.FixedSourceDistance;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.cache.fixedbitset.FixedBitSetFilter;
import org.elasticsearch.index.fielddata.IndexFieldData;
import org.elasticsearch.index.fielddata.IndexGeoPointFieldData;
import org.elasticsearch.index.fielddata.MultiGeoPointValues;
import org.elasticsearch.index.fielddata.NumericDoubleValues;
import org.elasticsearch.index.fielddata.SortedNumericDoubleValues;
import org.elasticsearch.index.fielddata.plain.ConditionalFloat.IndexConditionalFloatFieldData;
import org.elasticsearch.index.fielddata.plain.ConditionalFloat.MultiConditionalFloatValues;
import org.elasticsearch.index.fielddata.IndexFieldData.XFieldComparatorSource.Nested;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.object.ObjectMapper;
import org.elasticsearch.index.query.support.NestedInnerQueryParseSupport;
import org.elasticsearch.index.search.nested.NonNestedDocsFilter;
import org.elasticsearch.search.MultiValueMode;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.sort.SortParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class ConditionalFloatsSortParser implements SortParser {

    @Override
    public String[] names() {
        return new String[] { "_conditional_float",  "_conditionalFloat" };
    }

    @Override
    public SortField parse(XContentParser parser, SearchContext context) throws Exception {
        String fieldName = null;
        boolean reverse = false;
        final BitSet flagsSet = new BitSet();
        
        XContentParser.Token token;
        String currentName = parser.currentName();
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentName = parser.currentName();
            } else if (token == XContentParser.Token.START_ARRAY) {
                while (!parser.nextToken().equals(XContentParser.Token.END_ARRAY)) {
                    if (parser.currentToken() == XContentParser.Token.VALUE_NUMBER) {
                        flagsSet.set(parser.intValue());
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
        
        final IndexConditionalFloatFieldData multiValueNumericFieldData = context.fieldData().getForField(mapper);


        IndexFieldData.XFieldComparatorSource geoDistanceComparatorSource = new IndexFieldData.XFieldComparatorSource() {

            @Override
            public SortField.Type reducedType() {
                return SortField.Type.DOUBLE;
            }

            @Override
            public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) throws IOException {
                return new FieldComparator.FloatComparator(numHits, null, null, null) {
                	
                	@Override
                	protected Floats getFloatValues(AtomicReaderContext context, String field) throws IOException {
                		final MultiConditionalFloatValues multiConditionalFloatValues = multiValueNumericFieldData.load(context).getConditionalFloatValues();
                		final BitSet flags = flagsSet;
                		return new Floats() {
							
							@Override
							public float get(int docID) {
								multiConditionalFloatValues.setDocument(docID);
								return multiConditionalFloatValues.valueAt(0).value(flags);
							}
						};
                	}
                };
//            	return new FieldComparator.DoubleComparator(numHits, null, null, null) {
//                    @Override
//                    protected Doubles getDoubleValues(AtomicReaderContext context, String field) throws IOException {
//                        final MultiConditionalFloatValues multiConditionalFloatValues = multiValueNumericFieldData.load(context).getConditionalFloatValues();
//                        
//                        return new Doubles() {
//                            @Override
//                            public double get(int docID) {
//                                return multiValueNumericValues.get(docID);
//                            }
//                        };
//                    }
//                };
            }

        };

        return new SortField(fieldName, geoDistanceComparatorSource, reverse);
    }

    private void parseGeoPoints(XContentParser parser, List<GeoPoint> geoPoints) throws IOException {
        while (!parser.nextToken().equals(XContentParser.Token.END_ARRAY)) {
            if (parser.currentToken() == XContentParser.Token.VALUE_NUMBER) {
                // we might get here if the geo point is " number, number] " and
                // the parser already moved over the opening bracket
                // in this case we cannot use GeoUtils.parseGeoPoint(..) because
                // this expects an opening bracket
                double lon = parser.doubleValue();
                parser.nextToken();
                if (!parser.currentToken().equals(XContentParser.Token.VALUE_NUMBER)) {
                    throw new ElasticsearchParseException("geo point parsing: expected second number but got" + parser.currentToken());
                }
                double lat = parser.doubleValue();
                GeoPoint point = new GeoPoint();
                point.reset(lat, lon);
                geoPoints.add(point);
            } else {
                GeoPoint point = new GeoPoint();
                GeoUtils.parseGeoPoint(parser, point);
                geoPoints.add(point);
            }

        }
    }
}

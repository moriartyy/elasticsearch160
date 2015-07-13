package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

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
import org.elasticsearch.index.fielddata.IndexFieldDataCache;
import org.elasticsearch.index.fielddata.ordinals.Ordinals;
import org.elasticsearch.index.fielddata.ordinals.OrdinalsBuilder;
import org.elasticsearch.index.fielddata.plain.NonEstimatingEstimator;
import org.elasticsearch.index.mapper.FieldMapper.Names;
import org.elasticsearch.indices.breaker.CircuitBreakerService;


public class ConditionalFloatStringIndexFieldData extends AbstractConditionalFloatStringIndexFieldData {
	
	private final CircuitBreakerService breakerService;

	public ConditionalFloatStringIndexFieldData(Index index, Settings indexSettings, Names fieldNames,
			FieldDataType fieldDataType, IndexFieldDataCache cache, CircuitBreakerService breakerService) {
		super(index, indexSettings, fieldNames, fieldDataType, cache);
		this.breakerService = breakerService;
	}

	@Override
	public AtomicConditionalFloatFieldData loadDirect(AtomicReaderContext context) throws Exception {
        AtomicReader reader = context.reader();

        Terms terms = reader.terms(getFieldNames().indexName());
        AtomicConditionalFloatFieldData data = null;
        // TODO: Use an actual estimator to estimate before loading.
        NonEstimatingEstimator estimator = new NonEstimatingEstimator(breakerService.getBreaker(CircuitBreaker.Name.FIELDDATA));
        if (terms == null) {
            data = AbstractAtomicConditionalFloatFieldData.empty(reader.maxDoc());
            estimator.afterLoad(null, data.ramBytesUsed());
            return data;
        }
        
        final long numTerms = terms.size();
        final float acceptableTransientOverheadRatio = PackedInts.FAST;

        TermsEnum termsEnum = terms.iterator(null);
        boolean success = false;
        
        ObjectArray<Object> conditionalFloats = BigArrays.NON_RECYCLING_INSTANCE.newObjectArray(numTerms);

        try (OrdinalsBuilder builder = new OrdinalsBuilder(numTerms, reader.maxDoc(), acceptableTransientOverheadRatio)) {
            DocsEnum docsEnum = null;
            TermToConditionalFloat termToFloat = new TermToConditionalFloat();
            for (BytesRef term = termsEnum.next(); term != null; term = termsEnum.next()) {
                final long termOrd = builder.nextOrdinal();
                conditionalFloats.set(termOrd, termToFloat.parse(term));
                docsEnum = termsEnum.docs(null, docsEnum, DocsEnum.FLAG_NONE);
                for (int docId = docsEnum.nextDoc(); docId != DocsEnum.NO_MORE_DOCS; docId = docsEnum.nextDoc()) {
                    builder.addDoc(docId);
                }
            }
            
            final Ordinals ordinals = builder.build(fieldDataType.getSettings());

            data = new ConditionalFloatStringAtomicFieldData(conditionalFloats, ordinals);
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

	class TermToConditionalFloat {
		
		CharsRefBuilder spare = new CharsRefBuilder();
		
		public TermToConditionalFloat() {
		}
		
		public ConditionalFloat parse(BytesRef term) {
			ConditionalFloat conditionalFloat = new ConditionalFloat();
			spare.copyUTF8Bytes(term);
			int offset = 0;
			int id = 0;
			float v;
			for (int i=0; i<spare.length(); i++) {
				if (spare.charAt(i) == ':') {
					id = Integer.parseInt(new String(spare.chars(), offset, i - offset));
					offset = i + 1;
				} else if (spare.charAt(i) == ',') {
					v = Float.parseFloat(new String(spare.chars(), offset, i - offset));
					conditionalFloat.add(id, v);
					offset = i + 1;
				}
			}
			v = Float.parseFloat(new String(spare.chars(), offset, spare.length() - offset)); 
			conditionalFloat.add(id, v);
			return conditionalFloat;
		}
 	}
}

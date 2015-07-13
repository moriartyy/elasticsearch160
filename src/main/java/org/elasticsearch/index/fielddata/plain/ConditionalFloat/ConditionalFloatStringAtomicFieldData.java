package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import org.apache.lucene.index.RandomAccessOrds;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.util.ObjectArray;
import org.elasticsearch.index.fielddata.ordinals.Ordinals;

public class ConditionalFloatStringAtomicFieldData extends AbstractAtomicConditionalFloatFieldData  {

	private Ordinals ordinals;
	private ObjectArray<Object> conditionalFloats;

	public ConditionalFloatStringAtomicFieldData(ObjectArray<Object> conditionalFloats, Ordinals ordinals) {
		this.conditionalFloats = conditionalFloats;
		this.ordinals = ordinals;
	}

	@Override
	public MultiConditionalFloatValues getConditionalFloatValues() {
		
		final RandomAccessOrds randomAccessOrds = this.ordinals.ordinals();
		
		return new MultiConditionalFloatValues() {

			@Override
			public void setDocument(int docID) {
				randomAccessOrds.setDocument(docID);
			}

			@Override
			public int count() {
				return randomAccessOrds.cardinality();
			}

			@Override
			public ConditionalFloat valueAt(int index) {
				return (ConditionalFloat) conditionalFloats.get(randomAccessOrds.ordAt(index));
			}
			
		};
	}

	@Override
	public long ramBytesUsed() {
		// TODO how to calcualte this.
		return 0;
	}

	@Override
	public void close() throws ElasticsearchException {
		
	}

}

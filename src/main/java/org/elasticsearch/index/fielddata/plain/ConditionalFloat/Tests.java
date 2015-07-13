package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import java.io.IOException;

import org.apache.lucene.index.RandomAccessOrds;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.index.fielddata.ordinals.Ordinals;
import org.elasticsearch.index.fielddata.ordinals.OrdinalsBuilder;

public class Tests {
	
	public static void main(String[] args) throws IOException {
		
		double[] d = new double[10];
		
		int numTerms = 2;
		OrdinalsBuilder builder = new OrdinalsBuilder(numTerms, 6, 0.5f);
		for (int i=0; i<numTerms; i++) {
			long j = builder.nextOrdinal();
			System.out.println("ord: " + j);
			
//			if (j == 3) {
//				System.out.println(j);
//			}
			
			for (int k=0; k<3; k++) {
				int doc = k;
				System.out.println("add doc: " + (doc));
				builder.addDoc(doc);
			}
		}
		
		System.out.println();
		System.out.println("------------------------------");
		System.out.println();
		
		Ordinals ordinals = builder.build(ImmutableSettings.EMPTY);
		builder.close();
		RandomAccessOrds ords = ordinals.ordinals();
		ords.setDocument(2);
		int c = ords.cardinality();
		for (int i=0; i<c; i++) {
			System.out.println(ords.ordAt(i));
		}
	}

}

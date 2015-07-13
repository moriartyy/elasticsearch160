package org.elasticsearch.index.fielddata.plain.ConditionalFloat;

import org.elasticsearch.ElasticsearchIllegalArgumentException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.fielddata.FieldDataType;
import org.elasticsearch.index.fielddata.IndexFieldData.XFieldComparatorSource.Nested;
import org.elasticsearch.index.fielddata.IndexFieldDataCache;
import org.elasticsearch.index.fielddata.plain.AbstractIndexFieldData;
import org.elasticsearch.index.mapper.FieldMapper.Names;
import org.elasticsearch.search.MultiValueMode;

public abstract class AbstractConditionalFloatStringIndexFieldData extends AbstractIndexFieldData<AtomicConditionalFloatFieldData> implements IndexConditionalFloatFieldData {


    public AbstractConditionalFloatStringIndexFieldData(Index index, Settings indexSettings, Names fieldNames, FieldDataType fieldDataType, IndexFieldDataCache cache) {
        super(index, indexSettings, fieldNames, fieldDataType, cache);
    }

    @Override
    public final XFieldComparatorSource comparatorSource(@Nullable Object missingValue, MultiValueMode sortMode, Nested nested) {
        throw new ElasticsearchIllegalArgumentException("can't sort on conditional_float field without using specific sorting feature, like conditional_float");
    }


}

/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
 *   
 *  See the NOTICE file distributed with this work for additional
 *  information regarding copyright ownership.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Apache License,
 *  Version 2.0 which accompanies this distribution and is available at
 *  http://www.apache.org/licenses/LICENSE-2.0.txt
 ******************************************************************************/
package org.locationtech.geowave.core.store.adapter;

import java.util.Collections;

import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.index.InsertionIds;
import org.locationtech.geowave.core.store.data.PersistentDataset;
import org.locationtech.geowave.core.store.index.CommonIndexValue;
import org.locationtech.geowave.core.store.index.PrimaryIndex;

public class FitToIndexPersistenceEncoding extends
		AdapterPersistenceEncoding
{
	private final InsertionIds insertionIds;

	public FitToIndexPersistenceEncoding(
			final ByteArrayId dataId,
			final PersistentDataset<CommonIndexValue> commonData,
			final PersistentDataset<Object> adapterExtendedData,
			final ByteArrayId partitionKey,
			final ByteArrayId sortKey ) {
		super(
				dataId,
				commonData,
				adapterExtendedData);
		insertionIds = new InsertionIds(
				partitionKey,
				sortKey == null ? null : Collections.singletonList(sortKey));
	}

	@Override
	public InsertionIds getInsertionIds(
			final PrimaryIndex index ) {
		return insertionIds;
	}

	@Override
	public boolean isDeduplicationEnabled() {
		return false;
	}

}

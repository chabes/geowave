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
package org.locationtech.geowave.core.store.base;

import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.index.InsertionIds;
import org.locationtech.geowave.core.store.data.PersistentValue;
import org.locationtech.geowave.core.store.entities.GeoWaveKey;
import org.locationtech.geowave.core.store.entities.GeoWaveKeyImpl;
import org.locationtech.geowave.core.store.entities.GeoWaveRow;
import org.locationtech.geowave.core.store.entities.GeoWaveRowImpl;
import org.locationtech.geowave.core.store.entities.GeoWaveValue;
import org.locationtech.geowave.core.store.entities.GeoWaveValueImpl;

/**
 * There is a single intermediate row per original entry passed into a write
 * operation. This offers a higher level abstraction from the raw key-value
 * pairs in geowave (can be multiple per original entry). A datastore is
 * responsible for translating from this intermediary representation of rows to
 * key-value rows.
 *
 */
class IntermediaryWriteEntryInfo
{
	public static class FieldInfo<T>
	{
		private ByteArrayId fieldId;
		private T dataValue;
		private final byte[] visibility;
		private final byte[] writtenValue;

		public FieldInfo(
				final ByteArrayId fieldId,
				final T dataValue,
				final byte[] writtenValue,
				final byte[] visibility ) {
			this.fieldId = fieldId;
			this.dataValue = dataValue;
			this.writtenValue = writtenValue;
			this.visibility = visibility;
		}

		public ByteArrayId getFieldId() {
			return fieldId;
		}

		public T getDataValue() {
			return dataValue;
		}

		public byte[] getWrittenValue() {
			return writtenValue;
		}

		public byte[] getVisibility() {
			return visibility;
		}

		public GeoWaveValue getValue() {
			return new GeoWaveValueImpl(
					fieldId.getBytes(),
					visibility,
					writtenValue);
		}
	}

	private final byte[] dataId;
	private final short internalAdapterId;
	private final InsertionIds insertionIds;
	private final List<FieldInfo<?>> fieldInfo;

	public IntermediaryWriteEntryInfo(
			final byte[] dataId,
			final short internalAdapterId,
			final InsertionIds insertionIds,
			final List<FieldInfo<?>> fieldInfo ) {
		this.dataId = dataId;
		this.internalAdapterId = internalAdapterId;
		this.insertionIds = insertionIds;
		this.fieldInfo = fieldInfo;
	}

	@Override
	public String toString() {
		return new ByteArrayId(
				dataId).getString();
	}

	public short getInternalAdapterId() {
		return internalAdapterId;
	}

	public InsertionIds getInsertionIds() {
		return insertionIds;
	}

	public byte[] getDataId() {
		return dataId;
	}

	public List<FieldInfo<?>> getFieldInfo() {
		return fieldInfo;
	}

	public GeoWaveRow[] getRows() {
		final GeoWaveValue[] fieldValues = new GeoWaveValue[fieldInfo.size()];
		for (int i = 0; i < fieldValues.length; i++) {
			fieldValues[i] = fieldInfo.get(
					i).getValue();
		}
		final GeoWaveKey[] keys = GeoWaveKeyImpl.createKeys(
				insertionIds,
				dataId,
				internalAdapterId);
		return Arrays
				.stream(
						keys)
				.map(
						k -> new GeoWaveRowImpl(
								k,
								fieldValues))
				.toArray(
						new ArrayGenerator());
	}

	private static class ArrayGenerator implements
			IntFunction<GeoWaveRow[]>
	{
		@Override
		public GeoWaveRow[] apply(
				final int value ) {
			return new GeoWaveRow[value];
		}
	}

}

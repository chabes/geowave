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
package org.locationtech.geowave.adapter.raster.stats;

import java.nio.ByteBuffer;

import org.locationtech.geowave.adapter.raster.FitToIndexGridCoverage;
import org.locationtech.geowave.adapter.raster.RasterUtils;
import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.index.Mergeable;
import org.locationtech.geowave.core.store.adapter.statistics.AbstractDataStatistics;
import org.locationtech.geowave.core.store.entities.GeoWaveRow;
import org.opengis.coverage.grid.GridCoverage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

public class RasterFootprintStatistics extends
		AbstractDataStatistics<GridCoverage>
{
	private static final Logger LOGGER = LoggerFactory.getLogger(RasterFootprintStatistics.class);
	public static final ByteArrayId STATS_TYPE = new ByteArrayId(
			"FOOTPRINT");
	private Geometry footprint;

	public RasterFootprintStatistics() {
		this(
				null);
	}

	public RasterFootprintStatistics(
			final Short internalAdapterId ) {
		super(
				internalAdapterId,
				STATS_TYPE);
	}

	@Override
	public byte[] toBinary() {
		byte[] bytes = null;
		if (footprint == null) {
			bytes = new byte[] {};
		}
		else {
			bytes = new WKBWriter().write(footprint);
		}
		final ByteBuffer buf = super.binaryBuffer(bytes.length);
		buf.put(bytes);
		return buf.array();
	}

	@Override
	public void fromBinary(
			final byte[] bytes ) {
		final ByteBuffer buf = super.binaryBuffer(bytes);
		final byte[] payload = buf.array();
		if (payload.length > 0) {
			try {
				footprint = new WKBReader().read(payload);
			}
			catch (final ParseException e) {
				LOGGER.warn(
						"Unable to parse WKB",
						e);
			}
		}
		else {
			footprint = null;
		}
	}

	@Override
	public void entryIngested(
			final GridCoverage entry,
			final GeoWaveRow... geoWaveRows ) {
		if (entry instanceof FitToIndexGridCoverage) {
			footprint = RasterUtils.combineIntoOneGeometry(
					footprint,
					((FitToIndexGridCoverage) entry).getFootprintWorldGeometry());
		}
	}

	@Override
	public void merge(
			final Mergeable statistics ) {
		if (statistics instanceof RasterFootprintStatistics) {
			footprint = RasterUtils.combineIntoOneGeometry(
					footprint,
					((RasterFootprintStatistics) statistics).footprint);
		}
	}
}

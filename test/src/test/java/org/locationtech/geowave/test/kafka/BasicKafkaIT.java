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
package org.locationtech.geowave.test.kafka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.locationtech.geowave.adapter.raster.util.ZipUtils;
import org.locationtech.geowave.adapter.vector.FeatureDataAdapter;
import org.locationtech.geowave.adapter.vector.stats.FeatureBoundingBoxStatistics;
import org.locationtech.geowave.core.geotime.store.query.SpatialQuery;
import org.locationtech.geowave.core.geotime.store.statistics.BoundingBoxDataStatistics;
import org.locationtech.geowave.core.index.ByteArrayId;
import org.locationtech.geowave.core.store.CloseableIterator;
import org.locationtech.geowave.core.store.adapter.AdapterStore;
import org.locationtech.geowave.core.store.adapter.DataAdapter;
import org.locationtech.geowave.core.store.adapter.InternalDataAdapter;
import org.locationtech.geowave.core.store.adapter.PersistentAdapterStore;
import org.locationtech.geowave.core.store.adapter.statistics.CountDataStatistics;
import org.locationtech.geowave.core.store.adapter.statistics.DataStatisticsStore;
import org.locationtech.geowave.core.store.cli.remote.options.DataStorePluginOptions;
import org.locationtech.geowave.core.store.query.Query;
import org.locationtech.geowave.core.store.query.QueryOptions;
import org.locationtech.geowave.test.GeoWaveITRunner;
import org.locationtech.geowave.test.TestUtils;
import org.locationtech.geowave.test.annotation.Environments;
import org.locationtech.geowave.test.annotation.GeoWaveTestStore;
import org.locationtech.geowave.test.annotation.Environments.Environment;
import org.locationtech.geowave.test.annotation.GeoWaveTestStore.GeoWaveStoreType;
import org.locationtech.geowave.test.basic.AbstractGeoWaveIT;
import org.locationtech.geowave.test.kafka.KafkaTestUtils;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

@RunWith(GeoWaveITRunner.class)
@Environments({
	Environment.KAFKA
})
public class BasicKafkaIT extends
		AbstractGeoWaveIT
{
	private final static Logger LOGGER = LoggerFactory.getLogger(BasicKafkaIT.class);
	private static final Map<ByteArrayId, Integer> EXPECTED_COUNT_PER_ADAPTER_ID = new HashMap<ByteArrayId, Integer>();

	static {
		EXPECTED_COUNT_PER_ADAPTER_ID.put(
				new ByteArrayId(
						"gpxpoint"),
				11911);
		EXPECTED_COUNT_PER_ADAPTER_ID.put(
				new ByteArrayId(
						"gpxtrack"),
				4);
	}

	protected static final String TEST_DATA_ZIP_RESOURCE_PATH = TestUtils.TEST_RESOURCE_PACKAGE
			+ "mapreduce-testdata.zip";
	protected static final String OSM_GPX_INPUT_DIR = TestUtils.TEST_CASE_BASE + "osm_gpx_test_case/";

	@GeoWaveTestStore(value = {
		GeoWaveStoreType.ACCUMULO,
		GeoWaveStoreType.BIGTABLE,
		GeoWaveStoreType.HBASE,
		GeoWaveStoreType.DYNAMODB,
		GeoWaveStoreType.CASSANDRA
	})
	protected DataStorePluginOptions dataStorePluginOptions;

	protected DataStorePluginOptions getDataStorePluginOptions() {
		return dataStorePluginOptions;
	}

	private static long startMillis;

	@BeforeClass
	public static void extractTestFiles()
			throws URISyntaxException {
		ZipUtils.unZipFile(
				new File(
						BasicKafkaIT.class.getClassLoader().getResource(
								TEST_DATA_ZIP_RESOURCE_PATH).toURI()),
				TestUtils.TEST_CASE_BASE);

		startMillis = System.currentTimeMillis();
		LOGGER.warn("-----------------------------------------");
		LOGGER.warn("*                                       *");
		LOGGER.warn("*         RUNNING BasicKafkaIT          *");
		LOGGER.warn("*                                       *");
		LOGGER.warn("-----------------------------------------");
	}

	@AfterClass
	public static void reportTest() {
		LOGGER.warn("-----------------------------------------");
		LOGGER.warn("*                                       *");
		LOGGER.warn("*      FINISHED BasicKafkaIT            *");
		LOGGER
				.warn("*         " + ((System.currentTimeMillis() - startMillis) / 1000)
						+ "s elapsed.                 *");
		LOGGER.warn("*                                       *");
		LOGGER.warn("-----------------------------------------");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBasicIngestGpx()
			throws Exception {
		KafkaTestUtils.testKafkaStage(OSM_GPX_INPUT_DIR);
		KafkaTestUtils.testKafkaIngest(
				dataStorePluginOptions,
				false,
				OSM_GPX_INPUT_DIR);

		final DataStatisticsStore statsStore = dataStorePluginOptions.createDataStatisticsStore();
		final PersistentAdapterStore adapterStore = dataStorePluginOptions.createAdapterStore();
		int adapterCount = 0;

		try (CloseableIterator<InternalDataAdapter<?>> adapterIterator = adapterStore.getAdapters()) {
			while (adapterIterator.hasNext()) {
				final InternalDataAdapter<?> internalDataAdapter = adapterIterator.next();
				final FeatureDataAdapter adapter = (FeatureDataAdapter) internalDataAdapter.getAdapter();

				// query by the full bounding box, make sure there is more than
				// 0 count and make sure the count matches the number of results
				final BoundingBoxDataStatistics<?> bboxStat = (BoundingBoxDataStatistics<SimpleFeature>) statsStore
						.getDataStatistics(
								internalDataAdapter.getInternalAdapterId(),
								FeatureBoundingBoxStatistics.composeId(adapter
										.getFeatureType()
										.getGeometryDescriptor()
										.getLocalName()));
				final CountDataStatistics<?> countStat = (CountDataStatistics<SimpleFeature>) statsStore
						.getDataStatistics(
								internalDataAdapter.getInternalAdapterId(),
								CountDataStatistics.STATS_TYPE);
				// then query it
				final GeometryFactory factory = new GeometryFactory();
				final Envelope env = new Envelope(
						bboxStat.getMinX(),
						bboxStat.getMaxX(),
						bboxStat.getMinY(),
						bboxStat.getMaxY());
				final Geometry spatialFilter = factory.toGeometry(env);
				final Query query = new SpatialQuery(
						spatialFilter);
				final int resultCount = testQuery(
						adapter,
						query);
				assertTrue(
						"'" + adapter.getAdapterId().getString()
								+ "' adapter must have at least one element in its statistic",
						countStat.getCount() > 0);
				assertEquals(
						"'" + adapter.getAdapterId().getString()
								+ "' adapter should have the same results from a spatial query of '" + env
								+ "' as its total count statistic",
						countStat.getCount(),
						resultCount);
				assertEquals(
						"'" + adapter.getAdapterId().getString()
								+ "' adapter entries ingested does not match expected count",
						EXPECTED_COUNT_PER_ADAPTER_ID.get(adapter.getAdapterId()),
						new Integer(
								resultCount));
				adapterCount++;
			}
		}
		assertTrue(
				"There should be exactly two adapters",
				(adapterCount == 2));
	}

	private int testQuery(
			final DataAdapter<?> adapter,
			final Query query )
			throws Exception {
		final org.locationtech.geowave.core.store.DataStore geowaveStore = dataStorePluginOptions.createDataStore();

		final CloseableIterator<?> accumuloResults = geowaveStore.query(
				new QueryOptions(
						adapter,
						TestUtils.DEFAULT_SPATIAL_INDEX),
				query);

		int resultCount = 0;
		while (accumuloResults.hasNext()) {
			accumuloResults.next();

			resultCount++;
		}
		accumuloResults.close();

		return resultCount;

	}
}

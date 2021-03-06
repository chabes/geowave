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
package org.locationtech.geowave.core.cli.api;

import java.util.Map;

/**
 * This arguments are used to allow sections and commands to modify how
 * arguments are parsed during prepare / execution stage.
 */
public interface OperationParams
{

	/**
	 * Operations that were parsed & instantiated for execution
	 * 
	 * @return
	 */
	Map<String, Operation> getOperationMap();

	/**
	 * Key value pairs for contextual information during command parsing
	 */
	Map<String, Object> getContext();
}

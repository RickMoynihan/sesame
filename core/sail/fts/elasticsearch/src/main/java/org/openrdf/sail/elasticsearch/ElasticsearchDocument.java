/* 
 * Licensed to Aduna under one or more contributor license agreements.  
 * See the NOTICE.txt file distributed with this work for additional 
 * information regarding copyright ownership. 
 *
 * Aduna licenses this file to you under the terms of the Aduna BSD 
 * License (the "License"); you may not use this file except in compliance 
 * with the License. See the LICENSE.txt file distributed with this work 
 * for the full License.
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package org.openrdf.sail.elasticsearch;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.geo.GeoHashUtils;
import org.elasticsearch.search.SearchHit;
import org.openrdf.sail.lucene.SearchDocument;
import org.openrdf.sail.lucene.SearchFields;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

public class ElasticsearchDocument implements SearchDocument {

	private final String id;

	private final String type;

	private final long version;

	private final String index;

	private final Map<String, Object> fields;

	private final SpatialContext geoContext;

	public ElasticsearchDocument(SearchHit hit, SpatialContext geoContext) {
		this(hit.getId(), hit.getType(), hit.getIndex(), hit.getVersion(), hit.getSource(), geoContext);
	}

	public ElasticsearchDocument(String id, String type, String index, String resourceId, String context,
			SpatialContext geoContext)
	{
		this(id, type, index, 0L, new HashMap<String, Object>(), geoContext);
		fields.put(SearchFields.URI_FIELD_NAME, resourceId);
		if (context != null) {
			fields.put(SearchFields.CONTEXT_FIELD_NAME, context);
		}
	}

	public ElasticsearchDocument(String id, String type, String index, long version,
			Map<String, Object> fields, SpatialContext geoContext)
	{
		this.id = id;
		this.type = type;
		this.version = version;
		this.index = index;
		this.fields = fields;
		this.geoContext = geoContext;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public long getVersion() {
		return version;
	}

	public String getIndex() {
		return index;
	}

	public Map<String, Object> getSource() {
		return fields;
	}

	@Override
	public String getResource() {
		return (String)fields.get(SearchFields.URI_FIELD_NAME);
	}

	@Override
	public String getContext() {
		return (String)fields.get(SearchFields.CONTEXT_FIELD_NAME);
	}

	@Override
	public Set<String> getPropertyNames() {
		return ElasticsearchIndex.getPropertyFields(fields.keySet());
	}

	@Override
	public void addProperty(String name) {
		// in elastic search, fields must have an explicit value
		if (fields.containsKey(name)) {
			throw new IllegalStateException("Property already added: " + name);
		}
		fields.put(name, null);
		if (!fields.containsKey(SearchFields.TEXT_FIELD_NAME)) {
			fields.put(SearchFields.TEXT_FIELD_NAME, null);
		}
	}

	@Override
	public void addProperty(String name, String text) {
		addField(name, text, fields);
		addField(SearchFields.TEXT_FIELD_NAME, text, fields);
	}

	@Override
	public void addGeoProperty(String name, String text) {
		addField(name, text, fields);
		try {
			Shape shape = geoContext.readShapeFromWkt(text);
			if (shape instanceof Point) {
				Point p = (Point)shape;
				fields.put(ElasticsearchIndex.GEOHASH_FIELD_PREFIX + name,
						GeoHashUtils.encode(p.getY(), p.getX()));
			}
		}
		catch (ParseException e) {
			// ignore
		}
	}

	@Override
	public boolean hasProperty(String name, String value) {
		List<String> fieldValues = asStringList(fields.get(name));
		if (fieldValues != null) {
			for (String fieldValue : fieldValues) {
				if (value.equals(fieldValue)) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public List<String> getProperty(String name) {
		return asStringList(fields.get(name));
	}

	private static void addField(String name, String value, Map<String, Object> document) {
		Object oldValue = document.get(name);
		Object newValue;
		if (oldValue != null) {
			List<String> newList = makeModifiable(asStringList(oldValue));
			newList.add(value);
			newValue = newList;
		}
		else {
			newValue = value;
		}
		document.put(name, newValue);
	}

	private static List<String> makeModifiable(List<String> l) {
		List<String> modList;
		if (!(l instanceof ArrayList<?>)) {
			modList = new ArrayList<String>(l.size() + 1);
			modList.addAll(l);
		}
		else {
			modList = l;
		}
		return modList;
	}

	@SuppressWarnings("unchecked")
	private static List<String> asStringList(Object value) {
		List<String> l;
		if (value == null) {
			l = null;
		}
		else if (value instanceof List<?>) {
			l = (List<String>)value;
		}
		else {
			l = Collections.singletonList((String)value);
		}
		return l;
	}
}

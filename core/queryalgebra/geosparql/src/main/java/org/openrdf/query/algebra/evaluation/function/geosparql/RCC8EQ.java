package org.openrdf.query.algebra.evaluation.function.geosparql;

import org.openrdf.model.vocabulary.GEOF;
import org.openrdf.query.algebra.evaluation.function.Function;

import com.spatial4j.core.shape.Shape;

/**
 * The GeoSPARQL {@link Function} geof:rcc8eq, as defined in <a
 * href="http://www.opengeospatial.org/standards/geosparql">OGC GeoSPARQL - A
 * Geographic Query Language for RDF Data</a>.
 */
public class RCC8EQ extends GeometricRelationFunction {

	@Override
	public String getURI() {
		return GEOF.RCC8_EQ.stringValue();
	}

	@Override
	protected boolean relation(Shape s1, Shape s2) {
		return SpatialSupport.getSpatialAlgebra().equals(s1, s2);
	}
}

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
package info.aduna.iteration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 *
 * @author akam
 */
@RunWith(Suite.class)
@SuiteClasses({
	LimitIterationTest.class,
	EmptyIterationTest.class,
	OffsetIterationTest.class,
	ConvertingIterationTest.class,
	CloseableIteratorIterationTest.class,
	DelayedIterationTest.class,
	DistinctIterationTest.class,
	ExceptionConvertingIterationTest.class,
	FilterIterationTest.class,
	IntersectionIterationTest.class,
	DistinctIntersectionIterationTest.class,
	IteratorIterationTest.class,
	LookAheadIterationTest.class,
	MinusIterationTest.class,
	DistinctMinusIterationTest.class,
	SingletonIterationTest.class,
	UnionIterationTest.class,
})
public class AllTests {
}

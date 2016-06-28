/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.util.DefaultTraversalStrategies;
import org.apache.tinkerpop.gremlin.util.TimeUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@RunWith(Parameterized.class)
public class IdentityRemovalStrategyTest {

    private static boolean executedshouldBeFaster = false;

    @Parameterized.Parameter(value = 0)
    public Traversal original;

    @Parameterized.Parameter(value = 1)
    public Traversal optimized;


    private void applyIdentityRemovalStrategy(final Traversal traversal) {
        final TraversalStrategies strategies = new DefaultTraversalStrategies();
        strategies.addStrategies(IdentityRemovalStrategy.instance());
        traversal.asAdmin().setStrategies(strategies);
        traversal.asAdmin().applyStrategies();

    }

    @Test
    public void doTest() {
        applyIdentityRemovalStrategy(original);
        assertEquals(optimized, original);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable<Object[]> generateTestParameters() {

        return Arrays.asList(new Traversal[][]{
                {__.identity(), __.identity()},
                {__.identity().out(), __.out()},
                {__.identity().out().identity(), __.out()},
                {__.identity().as("a").out().identity(), __.identity().as("a").out()},
                {__.identity().as("a").out().identity().as("b"), __.identity().as("a").out().as("b")},
                {__.identity().as("a").out().in().identity().identity().as("b").identity().out(), __.identity().as("a").out().in().as("b").out()},
                {__.out().identity().as("a").out().in().identity().identity().as("b").identity().out(), __.out().as("a").out().in().as("b").out()},
        });
    }

    @Test
    public void shouldBeFaster() {
        if (executedshouldBeFaster)
            return;
        else
            executedshouldBeFaster = true;
        final int startSize = 1000;
        final int clockRuns = 1000;
        final Integer[] starts = new Integer[startSize];
        for (int i = 0; i < startSize; i++) {
            starts[i] = i;
        }

        final Runnable original = () -> __.inject(starts).identity().identity().identity().identity().iterate();

        final Runnable optimized = () -> {
            final Traversal<Integer, Integer> traversal = __.inject(starts).identity().identity().identity().identity();
            traversal.asAdmin().setStrategies(traversal.asAdmin().getStrategies().clone().addStrategies(IdentityRemovalStrategy.instance()));
            traversal.iterate();
        };

        //System.out.println(TimeUtil.clock(clockRuns, original) + "---" + TimeUtil.clock(clockRuns,optimized));
        assertTrue(TimeUtil.clock(clockRuns, original) > TimeUtil.clock(clockRuns, optimized));
        assertTrue(TimeUtil.clock(clockRuns, optimized) < TimeUtil.clock(clockRuns, original));
    }
}

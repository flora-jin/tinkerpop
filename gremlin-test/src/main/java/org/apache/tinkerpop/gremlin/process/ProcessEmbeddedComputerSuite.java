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
package org.apache.tinkerpop.gremlin.process;

import org.apache.tinkerpop.gremlin.AbstractGremlinSuite;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputerTest;
import org.apache.tinkerpop.gremlin.process.computer.clone.CloneVertexProgramTest;
import org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgramTest;
import org.apache.tinkerpop.gremlin.process.computer.search.path.ShortestPathVertexProgramTest;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalEngine;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalInterruptionComputerTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.LambdaStepTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.MatchTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.ProfileTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.ProgramTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.map.WriteTest;
import org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.ExplainTest;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.EarlyLimitStrategyProcessTest;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.optimization.IncidentToAdjacentStrategyProcessTest;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * The {@code ProcessLimitedComputerSuite} is a set of tests that providers may implement in addition to the standard
 *  * Gherkin feature tests. These tests are only relevant for embedded use cases covering features like lambdas or other
 *  * capabilities that can only work when running Gremlin in the same JVM as the {@link Graph} instance.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class ProcessEmbeddedComputerSuite extends AbstractGremlinSuite {

    /**
     * This list of tests in the suite that will be executed as part of this suite.
     */
    private static final Class<?>[] allTests = new Class<?>[]{
            GraphComputerTest.class,
            MatchTest.CountMatchTraversals.class,
            MatchTest.GreedyMatchTraversals.class,
            ProfileTest.Traversals.class,
            ProgramTest.Traversals.class,
            WriteTest.Traversals.class,
            ExplainTest.Traversals.class,
            LambdaStepTest.Traversals.class,

            // compliance
            TraversalInterruptionComputerTest.class,

            // algorithms
            PageRankVertexProgramTest.class,
            ShortestPathVertexProgramTest.class,
            CloneVertexProgramTest.class,

            // optimizations
            IncidentToAdjacentStrategyProcessTest.class,
            EarlyLimitStrategyProcessTest.class
    };

    /**
     * A list of the minimum set of base tests that Gremlin flavors should implement to be compliant with Gremlin.
     */
    private static final Class<?>[] testsToEnforce = new Class<?>[]{
            MatchTest.class,
            ProfileTest.class,
            ProgramTest.class,
    };

    /**
     * This constructor is used by JUnit and will run this suite with its concrete implementations of the
     * {@code testsToEnforce}.
     */
    public ProcessEmbeddedComputerSuite(final Class<?> klass, final RunnerBuilder builder) throws InitializationError {
        super(klass, builder, allTests, allTests, false, TraversalEngine.Type.COMPUTER);
    }

    /**
     * This constructor is used by Gremlin flavor implementers who supply their own implementations of the
     * {@code testsToEnforce}.
     */
    public ProcessEmbeddedComputerSuite(final Class<?> klass, final RunnerBuilder builder, final Class<?>[] testsToExecute) throws InitializationError {
        super(klass, builder, testsToExecute, testsToEnforce, true, TraversalEngine.Type.COMPUTER);
    }
}

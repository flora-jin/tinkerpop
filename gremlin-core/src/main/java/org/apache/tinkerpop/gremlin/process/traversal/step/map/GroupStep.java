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

package org.apache.tinkerpop.gremlin.process.traversal.step.map;

import org.apache.tinkerpop.gremlin.process.traversal.Operator;
import org.apache.tinkerpop.gremlin.process.traversal.Step;
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.FilteringBarrier;
import org.apache.tinkerpop.gremlin.process.traversal.step.Grouping;
import org.apache.tinkerpop.gremlin.process.traversal.step.ByModulating;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalParent;
import org.apache.tinkerpop.gremlin.process.traversal.step.Barrier;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.ReducingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.SupplyingBarrierStep;
import org.apache.tinkerpop.gremlin.process.traversal.traverser.TraverserRequirement;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalHelper;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalUtil;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.function.HashMapSupplier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class GroupStep<S, K, V> extends ReducingBarrierStep<S, Map<K, V>>
        implements ByModulating, TraversalParent, Grouping<S, K, V> {

    private char state = 'k';
    private Traversal.Admin<S, K> keyTraversal;
    private Traversal.Admin<S, V> valueTraversal;
    private Barrier barrierStep;

    public GroupStep(final Traversal.Admin traversal) {
        super(traversal);
        this.valueTraversal = this.integrateChild(__.fold().asAdmin());
        this.barrierStep = determineBarrierStep(this.valueTraversal);
        this.setReducingBiOperator(new GroupBiOperator<>(null == this.barrierStep ? Operator.assign : this.barrierStep.getMemoryComputeKey().getReducer()));
        this.setSeedSupplier(HashMapSupplier.instance());
    }

    @Override
    public Traversal.Admin<S, K> getKeyTraversal() {
        return this.keyTraversal;
    }

    @Override
    public Traversal.Admin<S, V> getValueTraversal() {
        return this.valueTraversal;
    }

    private void setValueTraversal(final Traversal.Admin kvTraversal) {
        this.valueTraversal = this.integrateChild(convertValueTraversal(kvTraversal));
        this.barrierStep = determineBarrierStep(this.valueTraversal);
        this.setReducingBiOperator(new GroupBiOperator<>(null == this.barrierStep ? Operator.assign : this.barrierStep.getMemoryComputeKey().getReducer()));
    }

    @Override
    public void resetBarrierFromValueTraversal() {
        this.barrierStep = determineBarrierStep(this.valueTraversal);
        this.setReducingBiOperator(new GroupBiOperator<>(null == this.barrierStep ? Operator.assign : this.barrierStep.getMemoryComputeKey().getReducer()));
    }

    @Override
    public void modulateBy(final Traversal.Admin<?, ?> kvTraversal) {
        if ('k' == this.state) {
            this.keyTraversal = this.integrateChild(kvTraversal);
            this.state = 'v';
        } else if ('v' == this.state) {
            this.setValueTraversal(kvTraversal);
            this.state = 'x';
        } else {
            throw new IllegalStateException("The key and value traversals for group()-step have already been set: " + this);
        }
    }

    @Override
    public void replaceLocalChild(final Traversal.Admin<?, ?> oldTraversal, final Traversal.Admin<?, ?> newTraversal) {
        if (null != this.keyTraversal && this.keyTraversal.equals(oldTraversal))
            this.keyTraversal = this.integrateChild(newTraversal);
        else if (null != this.valueTraversal && this.valueTraversal.equals(oldTraversal))
            this.setValueTraversal(newTraversal);
    }

    @Override
    public Map<K, V> projectTraverser(final Traverser.Admin<S> traverser) {
        final Map<K, V> map = new HashMap<>(1);
        this.valueTraversal.reset();
        this.valueTraversal.addStart(traverser);

        TraversalUtil.produce(traverser, this.keyTraversal).ifProductive(p -> {
            // if there's no barrier then add the result of the valueTraversal as the value. if there is a barrier,
            // then pop the next barrier value into the map. there are cases where hasNextBarrier is false in which
            // case CollectingBarrierStep instances get special handling but only when there are
            // Supplying/ReducingBarrierStep somewhere in the traversal. these step instances like fold() or sum()
            // can take an empty TraverserSet and actually produce an output. if that isn't accounted for the key will
            // just get filtered out. TINKERPOP-2971
            if (null == this.barrierStep) {
                if (this.valueTraversal.hasNext()) {
                    map.put((K) p, (V) this.valueTraversal.next());
                }
            } else if (this.barrierStep.hasNextBarrier()) {
                map.put((K) p, (V) this.barrierStep.nextBarrier());
            } else if (this.barrierStep instanceof FilteringBarrier) {
                final int barrierStepIndex = TraversalHelper.stepIndex((Step) barrierStep, valueTraversal) + 1;
                if (valueTraversal.getSteps().stream().skip(barrierStepIndex).anyMatch(
                            step -> step instanceof SupplyingBarrierStep || step instanceof ReducingBarrierStep)) {
                    map.put((K) p, (V) this.barrierStep.getEmptyBarrier());
                }
            }
        });

        return map;
    }

    @Override
    public String toString() {
        return StringFactory.stepString(this, this.keyTraversal, this.valueTraversal);
    }

    @Override
    public List<Traversal.Admin<?, ?>> getLocalChildren() {
        final List<Traversal.Admin<?, ?>> children = new ArrayList<>(2);
        if (null != this.keyTraversal)
            children.add(this.keyTraversal);
        children.add(this.valueTraversal);
        return children;
    }

    @Override
    public Set<TraverserRequirement> getRequirements() {
        return this.getSelfAndChildRequirements(TraverserRequirement.OBJECT, TraverserRequirement.BULK);
    }

    @Override
    public GroupStep<S, K, V> clone() {
        final GroupStep<S, K, V> clone = (GroupStep<S, K, V>) super.clone();
        if (null != this.keyTraversal)
            clone.keyTraversal = this.keyTraversal.clone();
        clone.valueTraversal = this.valueTraversal.clone();
        clone.barrierStep = determineBarrierStep(clone.valueTraversal);
        return clone;
    }

    @Override
    public void setTraversal(final Traversal.Admin<?, ?> parentTraversal) {
        super.setTraversal(parentTraversal);
        integrateChild(this.keyTraversal);
        integrateChild(this.valueTraversal);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        if (this.keyTraversal != null) result ^= this.keyTraversal.hashCode();
        result ^= this.valueTraversal.hashCode();
        return result;
    }

    @Override
    public Map<K, V> generateFinalResult(final Map<K, V> object) {
        return doFinalReduction((Map<K, Object>) object, this.valueTraversal);
    }

    ///////////////////////

    public static final class GroupBiOperator<K, V> implements BinaryOperator<Map<K, V>>, Serializable {

        private BinaryOperator<V> barrierAggregator;

        public GroupBiOperator() {
            // no-arg constructor for serialization
        }

        public GroupBiOperator(final BinaryOperator<V> barrierAggregator) {
            this.barrierAggregator = barrierAggregator;
        }

        @Override
        public Map<K, V> apply(final Map<K, V> mapA, final Map<K, V> mapB) {
            for (final K key : mapB.keySet()) {
                V objectA = mapA.get(key);
                final V objectB = mapB.get(key);
                if (null == objectA)
                    objectA = objectB;
                else if (null != objectB)
                    objectA = this.barrierAggregator.apply(objectA, objectB);
                mapA.put(key, objectA);
            }
            return mapA;
        }
    }
}
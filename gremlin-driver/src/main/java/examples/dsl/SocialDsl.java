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
package examples.dsl;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerFactory;

import static org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource.traversal;

public class SocialDsl {
    public static void main(String[] args) {
        Graph graph = TinkerFactory.createModern();
        SocialTraversalSource social = traversal(SocialTraversalSource.class).with(graph);

        // Validating that Marko knows Josh
        boolean hasMarkoKnowsJosh = social.V().has("name","marko").knows("josh").hasNext();
        System.out.println("Marko knows Josh: " + hasMarkoKnowsJosh);
        boolean personsMarkoKnowsJosh = social.persons("marko").knows("josh").hasNext();
        System.out.println("Marko knows Josh: " + personsMarkoKnowsJosh);

        // Get age of the youngest friend of Marko
        int hasYoungestFriendAge = social.V().has("name","marko").youngestFriendsAge().next().intValue();
        System.out.println("Youngest friend of Marko is " + hasYoungestFriendAge + " years old.");
        int personsYoungestFriendAge = social.persons("marko").youngestFriendsAge().next().intValue();
        System.out.println("Youngest friend of Marko is " + personsYoungestFriendAge + " years old.");

        // Get number of all persons
        int numberOfAll= social.persons().count().next().intValue();
        System.out.println("Number of all persons: " + numberOfAll);

        // Find all persons with two or more projects
        int numberOfPersons = social.persons().filter(__.createdAtLeast(2)).count().next().intValue();
        System.out.println("Number of persons with two or more projects: " + numberOfPersons);
    }
}

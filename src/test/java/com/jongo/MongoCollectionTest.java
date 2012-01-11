/*
 * Copyright (C) 2011 Benoit GUEROUT <bguerout at gmail dot com> and Yves AMSELLEM <amsellem dot yves at gmail dot com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jongo;

import static org.fest.assertions.Assertions.assertThat;

import java.net.UnknownHostException;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import com.jongo.jackson.EntityProcessor;
import com.jongo.model.Coordinate;
import com.jongo.model.Poi;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class MongoCollectionTest {

    private MongoCollection mongoCollection;
    private String address = "22 rue des murlins", id = "1";
    private int lat = 48, lng = 2;

    @Before
    public void setUp() throws UnknownHostException, MongoException {
        DBCollection collection = new Mongo().getDB("jongo").getCollection("poi");
        mongoCollection = new MongoCollection(collection, new EntityProcessor());
        mongoCollection.drop();
    }

    String addressExists = "{address:{$exists:true}}";

    @Test
    public void canFindOne() throws Exception {
        /* given */
        mongoCollection.save(new Poi("999", address));// TODO return id

        /* when */
        String id = mongoCollection.findOne(addressExists, new IdDBObjectMapper());
        Poi poi = mongoCollection.findOne(addressExists, Poi.class);

        /* then */
        assertThat(id).isEqualTo("999");
        assertThat(poi.id).isEqualTo("999");
    }

    @Test
    public void canFindOneWithParameters() throws Exception {
        /* given */
        mongoCollection.save(new Poi("999", address));// TODO return id

        /* when */
        String id = mongoCollection.findOne("{_id:#}", new Object[] { "999" }, new IdDBObjectMapper());
        Poi poi = mongoCollection.findOne("{_id:#}", new Object[] { "999" }, Poi.class);

        /* then */
        assertThat(id).isEqualTo("999");
        assertThat(poi.id).isEqualTo("999");
    }

    @Test
    public void shouldEmptyResultBeCorrect() throws Exception {
        assertThat(mongoCollection.findOne("{_id:'invalid-id'}", Poi.class)).isNull();
        assertThat(mongoCollection.findOne("{_id:'invalid-id'}", new IdDBObjectMapper())).isNull();

        assertThat(mongoCollection.find("{_id:'invalid-id'}", Poi.class)).hasSize(0);
    }

    @Test
    public void canFindEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(id, address));

        /* when */
        Iterator<String> strings = mongoCollection.find(addressExists, new IdDBObjectMapper());
        Iterator<Poi> pois = mongoCollection.find(addressExists, Poi.class);

        /* then */
        assertThat(strings.next()).isEqualTo(id);
        assertThat(pois.next().id).isEqualTo(id);

        assertThat(strings.hasNext()).isFalse();
        assertThat(pois.hasNext()).isFalse();
    }

    @Test
    public void canFindEntitiesWithMapperAndParameters() throws Exception {
        /* given */
        mongoCollection.save(new Poi(id, address));

        /* when */
        Iterator<String> strings = mongoCollection.find("{_id:#}", new Object[] { "1" }, new IdDBObjectMapper());

        /* then */
        assertThat(strings.hasNext()).isTrue();
        assertThat(strings.next()).isEqualTo(id);
    }

    @Test
    public void canFindEntitiesUsingSubProperty() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));

        /* when */
        Iterator<Poi> results = mongoCollection.find("{'coordinate.lat':48}", Poi.class);

        /* then */
        assertThat(results.next().coordinate.lat).isEqualTo(lat);
        assertThat(results.hasNext()).isFalse();
    }

    @Test
    public void canSortEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi("23 rue des murlins"));
        mongoCollection.save(new Poi("21 rue des murlins"));
        mongoCollection.save(new Poi("22 rue des murlins"));

        /* when */
        Iterator<Poi> results = mongoCollection.find("{'$query':{}, '$orderby':{'address':1}}", Poi.class);

        /* then */
        assertThat(results.next().address).isEqualTo("21 rue des murlins");
        assertThat(results.next().address).isEqualTo("22 rue des murlins");
        assertThat(results.next().address).isEqualTo("23 rue des murlins");
        assertThat(results.hasNext()).isFalse();
    }

    @Test
    public void canLimitEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi(address));

        /* when */
        Iterator<Poi> results = mongoCollection.find("{'$query':{}, '$maxScan':2}", Poi.class);

        /* then */
        assertThat(results).hasSize(2);
    }

    @Test
    public void canUseConditionnalOperator() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, 1, 1));
        mongoCollection.save(new Poi(address, 2, 1));
        mongoCollection.save(new Poi(address, 3, 1));

        /* then */
        assertThat(mongoCollection.find("{coordinate.lat: {$gt: 2}}", Poi.class)).hasSize(1);
        assertThat(mongoCollection.find("{coordinate.lat: {$lt: 2}}", Poi.class)).hasSize(1);
        assertThat(mongoCollection.find("{coordinate.lat: {$gte: 2}}", Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{coordinate.lat: {$lte: 2}}", Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{coordinate.lat: {$gt: 1, $lt: 3}}", Poi.class)).hasSize(1);

        assertThat(mongoCollection.find("{coordinate.lat: {$ne: 2}}", Poi.class)).hasSize(2);
        assertThat(mongoCollection.find("{coordinate.lat: {$in: [1,2,3]}}", Poi.class)).hasSize(3);
    }

    @Test
    public void canFilterDistinctStringEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi(address));
        mongoCollection.save(new Poi("23 rue des murlins"));

        /* when */
        Iterator<String> addresses = mongoCollection.distinct("address", "", String.class);

        /* then */
        assertThat(addresses.next()).isEqualTo(address);
        assertThat(addresses.next()).isEqualTo("23 rue des murlins");
        assertThat(addresses.hasNext()).isFalse();
    }

    @Test
    public void canFilterDistinctIntegerEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, 4, 1));

        /* when */
        Iterator<Integer> addresses = mongoCollection.distinct("coordinate.lat", "", Integer.class);

        /* then */
        assertThat(addresses.next()).isEqualTo(lat);
        assertThat(addresses.next()).isEqualTo(4);
        assertThat(addresses.hasNext()).isFalse();
    }

    @Test
    public void canFilterDistinctEntitiesOnTypedProperty() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, 4, 1));

        /* when */
        Iterator<Coordinate> coordinates = mongoCollection.distinct("coordinate", "", Coordinate.class);

        /* then */
        Coordinate first = coordinates.next();
        assertThat(first.lat).isEqualTo(lat);
        assertThat(first.lng).isEqualTo(lng);
        Coordinate second = coordinates.next();
        assertThat(second.lat).isEqualTo(4);
        assertThat(second.lng).isEqualTo(1);
        assertThat(coordinates.hasNext()).isFalse();
    }

    @Test
    public void canFilterDistinctEntitiesWithQuery() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(null, 4, 1));

        /* when */
        Iterator<Coordinate> coordinates = mongoCollection.distinct("coordinate", addressExists, Coordinate.class);

        /* then */
        Coordinate first = coordinates.next();
        assertThat(first.lat).isEqualTo(lat);
        assertThat(first.lng).isEqualTo(lng);
        assertThat(coordinates.hasNext()).isFalse();
    }

    @Test
    public void canCountEntities() throws Exception {
        /* given */
        mongoCollection.save(new Poi(address, lat, lng));
        mongoCollection.save(new Poi(null, 4, 1));

        /* then */
        assertThat(mongoCollection.count(addressExists)).isEqualTo(1);
        assertThat(mongoCollection.count("{'coordinate.lat': {$exists:true}}")).isEqualTo(2);
    }

    @Test
    public void canUpdateEntity() throws Exception {
        /* given */
        mongoCollection.save(new Poi(id, address));
        Iterator<Poi> pois = mongoCollection.find("{_id: '1'}", Poi.class);
        Poi poi = pois.next();
        poi.address = null;
        mongoCollection.save(poi);

        /* when */
        pois = mongoCollection.find("{_id: '1'}", Poi.class);

        /* then */
        poi = pois.next();
        assertThat(poi.id).isEqualTo(id);
        assertThat(poi.address).isNull();
    }

    @Test
    public void canGetCollectionName() throws Exception {
        assertThat(mongoCollection.getName()).isEqualTo("poi");
    }

    private static class IdDBObjectMapper implements DBObjectMapper<String> {
        @Override
        public String map(DBObject result) {
            return result.get(MongoCollection.MONGO_ID).toString();
        }
    }
}

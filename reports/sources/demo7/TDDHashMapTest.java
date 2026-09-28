package org.jugsaxony.tdd.demo7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * TDD test suite for {@link TDDHashMap}.
 *
 * Contract under test:
 * - open hashing (open addressing), no separate chaining, no entry wrappers
 * - not thread-safe, unbounded capacity
 * - null keys rejected (NullPointerException), null values permitted
 * - keys()/values() return snapshot lists, order unspecified
 */
class TDDHashMapTest
{
    private TDDHashMap<String, String> map;

    @BeforeEach
    void setUp()
    {
        map = new TDDHashMap<>();
    }

    // ------------------------------------------------------------------
    // Key type used to force hash collisions: every instance reports the
    // same hashCode, equality is based on id.
    // ------------------------------------------------------------------
    private static final class CollidingKey
    {
        private final int id;

        CollidingKey( final int id )
        {
            this.id = id;
        }

        @Override
        public int hashCode()
        {
            return 42;
        }

        @Override
        public boolean equals( final Object obj )
        {
            return obj instanceof CollidingKey other && other.id == this.id;
        }

        @Override
        public String toString()
        {
            return "CollidingKey(" + id + ")";
        }
    }

    // ------------------------------------------------------------------
    // Key type with a configurable, possibly pathological hashCode.
    // ------------------------------------------------------------------
    private static final class HashKey
    {
        private final int id;
        private final int hash;

        HashKey( final int id, final int hash )
        {
            this.id = id;
            this.hash = hash;
        }

        @Override
        public int hashCode()
        {
            return hash;
        }

        @Override
        public boolean equals( final Object obj )
        {
            return obj instanceof HashKey other && other.id == this.id;
        }

        @Override
        public String toString()
        {
            return "HashKey(" + id + ",hash=" + hash + ")";
        }
    }

    @Nested
    @DisplayName( "construction and empty state" )
    class Construction
    {
        @Test
        @DisplayName( "a fresh map is empty" )
        void freshMapIsEmpty()
        {
            assertEquals( 0, map.size() );
            assertTrue( map.keys().isEmpty() );
            assertTrue( map.values().isEmpty() );
        }

        @Test
        @DisplayName( "get on an empty map returns null" )
        void getOnEmptyMapReturnsNull()
        {
            assertNull( map.get( "anything" ) );
        }

        @Test
        @DisplayName( "remove on an empty map returns null" )
        void removeOnEmptyMapReturnsNull()
        {
            assertNull( map.remove( "anything" ) );
        }

        @Test
        @DisplayName( "clear on an empty map is a no-op" )
        void clearOnEmptyMapIsNoOp()
        {
            map.clear();
            assertEquals( 0, map.size() );
        }

        @Test
        @DisplayName( "keys() and values() on an empty map return non-null lists" )
        void emptyMapReturnsNonNullLists()
        {
            assertTrue( map.keys() != null );
            assertTrue( map.values() != null );
        }
    }

    @Nested
    @DisplayName( "put and get" )
    class PutAndGet
    {
        @Test
        @DisplayName( "put then get returns the value" )
        void putThenGet()
        {
            map.put( "one", "1" );

            assertEquals( "1", map.get( "one" ) );
            assertEquals( 1, map.size() );
        }

        @Test
        @DisplayName( "put returns null when the key was absent" )
        void putReturnsNullForNewKey()
        {
            assertNull( map.put( "one", "1" ) );
        }

        @Test
        @DisplayName( "put returns the previous value when overwriting" )
        void putReturnsPreviousValue()
        {
            map.put( "one", "1" );

            assertEquals( "1", map.put( "one", "uno" ) );
            assertEquals( "uno", map.get( "one" ) );
            assertEquals( 1, map.size(), "overwrite must not grow the map" );
        }

        @Test
        @DisplayName( "get for an absent key returns null" )
        void getAbsentKeyReturnsNull()
        {
            map.put( "one", "1" );

            assertNull( map.get( "two" ) );
        }

        @Test
        @DisplayName( "multiple distinct keys coexist" )
        void multipleKeys()
        {
            map.put( "a", "1" );
            map.put( "b", "2" );
            map.put( "c", "3" );

            assertEquals( 3, map.size() );
            assertEquals( "1", map.get( "a" ) );
            assertEquals( "2", map.get( "b" ) );
            assertEquals( "3", map.get( "c" ) );
        }

        @Test
        @DisplayName( "keys that are equal but not identical resolve to the same entry" )
        void equalButNotSameKeyInstance()
        {
            map.put( new String( "key" ), "value" );

            assertEquals( "value", map.get( new String( "key" ) ) );
            assertEquals( "value", map.put( new String( "key" ), "other" ) );
            assertEquals( 1, map.size() );
        }
    }

    @Nested
    @DisplayName( "null handling" )
    class NullHandling
    {
        @Test
        @DisplayName( "put with a null key throws NullPointerException" )
        void putNullKeyThrows()
        {
            assertThrows( NullPointerException.class, () -> map.put( null, "v" ) );
        }

        @Test
        @DisplayName( "get with a null key throws NullPointerException" )
        void getNullKeyThrows()
        {
            assertThrows( NullPointerException.class, () -> map.get( null ) );
        }

        @Test
        @DisplayName( "remove with a null key throws NullPointerException" )
        void removeNullKeyThrows()
        {
            assertThrows( NullPointerException.class, () -> map.remove( null ) );
        }

        @Test
        @DisplayName( "null values are permitted and stored" )
        void nullValueIsPermitted()
        {
            assertNull( map.put( "k", null ) );

            assertEquals( 1, map.size(), "a key mapped to null still counts" );
            assertNull( map.get( "k" ) );
            assertTrue( map.keys().contains( "k" ) );
        }

        @Test
        @DisplayName( "overwriting a null value returns null and stores the new value" )
        void overwriteNullValue()
        {
            map.put( "k", null );

            assertNull( map.put( "k", "v" ) );
            assertEquals( "v", map.get( "k" ) );
            assertEquals( 1, map.size() );
        }

        @Test
        @DisplayName( "overwriting a value with null works" )
        void overwriteWithNullValue()
        {
            map.put( "k", "v" );

            assertEquals( "v", map.put( "k", null ) );
            assertNull( map.get( "k" ) );
            assertEquals( 1, map.size() );
        }

        @Test
        @DisplayName( "remove of a key holding a null value returns null but removes the key" )
        void removeNullValuedKey()
        {
            map.put( "k", null );

            assertNull( map.remove( "k" ) );
            assertEquals( 0, map.size() );
            assertFalse( map.keys().contains( "k" ) );
        }

        @Test
        @DisplayName( "values() contains null values" )
        void valuesContainsNull()
        {
            map.put( "a", null );
            map.put( "b", "2" );

            final List<String> values = map.values();
            assertEquals( 2, values.size() );
            assertTrue( values.contains( null ) );
            assertTrue( values.contains( "2" ) );
        }
    }

    @Nested
    @DisplayName( "remove" )
    class Remove
    {
        @Test
        @DisplayName( "remove returns the stored value and shrinks the map" )
        void removeReturnsValue()
        {
            map.put( "a", "1" );
            map.put( "b", "2" );

            assertEquals( "1", map.remove( "a" ) );
            assertEquals( 1, map.size() );
            assertNull( map.get( "a" ) );
            assertEquals( "2", map.get( "b" ) );
        }

        @Test
        @DisplayName( "remove of an absent key returns null and changes nothing" )
        void removeAbsentKey()
        {
            map.put( "a", "1" );

            assertNull( map.remove( "zzz" ) );
            assertEquals( 1, map.size() );
            assertEquals( "a", map.keys().get( 0 ) );
        }

        @Test
        @DisplayName( "removing twice returns null the second time" )
        void removeTwice()
        {
            map.put( "a", "1" );

            assertEquals( "1", map.remove( "a" ) );
            assertNull( map.remove( "a" ) );
            assertEquals( 0, map.size() );
        }

        @Test
        @DisplayName( "a removed key can be re-inserted" )
        void reinsertAfterRemove()
        {
            map.put( "a", "1" );
            map.remove( "a" );

            assertNull( map.put( "a", "2" ) );
            assertEquals( "2", map.get( "a" ) );
            assertEquals( 1, map.size() );
        }

        @Test
        @DisplayName( "removing every key one by one empties the map" )
        void removeAllOneByOne()
        {
            for ( int i = 0; i < 100; i++ )
            {
                map.put( "key" + i, "value" + i );
            }
            for ( int i = 0; i < 100; i++ )
            {
                assertEquals( "value" + i, map.remove( "key" + i ) );
            }

            assertEquals( 0, map.size() );
            assertTrue( map.keys().isEmpty() );
            assertTrue( map.values().isEmpty() );
        }
    }

    @Nested
    @DisplayName( "size" )
    class Size
    {
        @Test
        @DisplayName( "size tracks puts, overwrites, removes and clear" )
        void sizeTracking()
        {
            assertEquals( 0, map.size() );

            map.put( "a", "1" );
            assertEquals( 1, map.size() );

            map.put( "b", "2" );
            assertEquals( 2, map.size() );

            map.put( "a", "9" );
            assertEquals( 2, map.size(), "overwrite must not change size" );

            map.remove( "a" );
            assertEquals( 1, map.size() );

            map.remove( "absent" );
            assertEquals( 1, map.size(), "failed remove must not change size" );

            map.clear();
            assertEquals( 0, map.size() );
        }
    }

    @Nested
    @DisplayName( "keys and values" )
    class KeysAndValues
    {
        @Test
        @DisplayName( "keys() returns every key exactly once" )
        void keysContainsAllKeysOnce()
        {
            map.put( "a", "1" );
            map.put( "b", "2" );
            map.put( "c", "3" );
            map.put( "a", "9" );

            final List<String> keys = map.keys();
            assertEquals( 3, keys.size() );
            assertEquals( Set.of( "a", "b", "c" ), new HashSet<>( keys ) );
        }

        @Test
        @DisplayName( "values() returns one value per entry, including duplicates" )
        void valuesContainsAllValues()
        {
            map.put( "a", "same" );
            map.put( "b", "same" );
            map.put( "c", "other" );

            final List<String> values = map.values();
            assertEquals( 3, values.size() );
            assertEquals( 2, values.stream().filter( "same"::equals ).count() );
            assertEquals( 1, values.stream().filter( "other"::equals ).count() );
        }

        @Test
        @DisplayName( "keys() and values() have the same size as the map" )
        void keysAndValuesSizes()
        {
            for ( int i = 0; i < 50; i++ )
            {
                map.put( "k" + i, "v" + i );
            }

            assertEquals( map.size(), map.keys().size() );
            assertEquals( map.size(), map.values().size() );
        }

        @Test
        @DisplayName( "keys() reflects removals" )
        void keysReflectRemovals()
        {
            map.put( "a", "1" );
            map.put( "b", "2" );
            map.remove( "a" );

            assertEquals( List.of( "b" ), map.keys() );
        }

        @Test
        @DisplayName( "returned lists are snapshots; mutating them does not affect the map" )
        void returnedListsAreSnapshots()
        {
            map.put( "a", "1" );

            final List<String> keys = map.keys();
            keys.add( "injected" );
            final List<String> values = map.values();
            values.clear();

            assertEquals( 1, map.size() );
            assertEquals( "1", map.get( "a" ) );
            assertEquals( List.of( "a" ), map.keys() );
        }
    }

    @Nested
    @DisplayName( "clear" )
    class Clear
    {
        @Test
        @DisplayName( "clear removes all entries" )
        void clearEmptiesMap()
        {
            map.put( "a", "1" );
            map.put( "b", "2" );

            map.clear();

            assertEquals( 0, map.size() );
            assertNull( map.get( "a" ) );
            assertNull( map.get( "b" ) );
            assertTrue( map.keys().isEmpty() );
            assertTrue( map.values().isEmpty() );
        }

        @Test
        @DisplayName( "a cleared map is fully reusable" )
        void mapIsReusableAfterClear()
        {
            map.put( "a", "1" );
            map.clear();

            assertNull( map.put( "a", "2" ), "cleared keys must count as absent" );
            map.put( "b", "3" );

            assertEquals( 2, map.size() );
            assertEquals( "2", map.get( "a" ) );
            assertEquals( "3", map.get( "b" ) );
        }

        @Test
        @DisplayName( "clear after removes and re-inserts leaves no residue" )
        void clearAfterChurn()
        {
            for ( int i = 0; i < 50; i++ )
            {
                map.put( "k" + i, "v" + i );
            }
            for ( int i = 0; i < 25; i++ )
            {
                map.remove( "k" + i );
            }
            map.put( "k0", "again" );

            map.clear();

            assertEquals( 0, map.size() );
            assertNull( map.get( "k0" ) );
        }
    }

    @Nested
    @DisplayName( "collision handling (open addressing)" )
    class Collisions
    {
        @Test
        @DisplayName( "keys with identical hashCodes are all stored and found" )
        void identicalHashCodes()
        {
            final TDDHashMap<CollidingKey, Integer> collisionMap = new TDDHashMap<>();

            for ( int i = 0; i < 100; i++ )
            {
                collisionMap.put( new CollidingKey( i ), i );
            }

            assertEquals( 100, collisionMap.size() );
            for ( int i = 0; i < 100; i++ )
            {
                assertEquals( i, collisionMap.get( new CollidingKey( i ) ) );
            }
        }

        @Test
        @DisplayName( "overwrite works for colliding keys" )
        void overwriteCollidingKey()
        {
            final TDDHashMap<CollidingKey, String> collisionMap = new TDDHashMap<>();
            collisionMap.put( new CollidingKey( 1 ), "first" );
            collisionMap.put( new CollidingKey( 2 ), "second" );

            assertEquals( "first", collisionMap.put( new CollidingKey( 1 ), "replaced" ) );
            assertEquals( "replaced", collisionMap.get( new CollidingKey( 1 ) ) );
            assertEquals( "second", collisionMap.get( new CollidingKey( 2 ) ) );
            assertEquals( 2, collisionMap.size() );
        }

        @Test
        @DisplayName( "removing a key in the middle of a probe chain keeps later keys reachable" )
        void removeFromMiddleOfProbeChain()
        {
            final TDDHashMap<CollidingKey, Integer> collisionMap = new TDDHashMap<>();
            for ( int i = 0; i < 20; i++ )
            {
                collisionMap.put( new CollidingKey( i ), i );
            }

            // remove keys from the middle of the chain
            for ( int i = 5; i < 15; i++ )
            {
                assertEquals( i, collisionMap.remove( new CollidingKey( i ) ) );
            }

            assertEquals( 10, collisionMap.size() );
            // keys probed past the removed slots must still be found
            for ( int i = 15; i < 20; i++ )
            {
                assertEquals( i, collisionMap.get( new CollidingKey( i ) ) );
            }
            // removed keys must be gone
            for ( int i = 5; i < 15; i++ )
            {
                assertNull( collisionMap.get( new CollidingKey( i ) ) );
            }
            // remaining early keys untouched
            for ( int i = 0; i < 5; i++ )
            {
                assertEquals( i, collisionMap.get( new CollidingKey( i ) ) );
            }
        }

        @Test
        @DisplayName( "re-inserting into slots freed by removals works" )
        void reinsertIntoFreedSlots()
        {
            final TDDHashMap<CollidingKey, Integer> collisionMap = new TDDHashMap<>();
            for ( int i = 0; i < 10; i++ )
            {
                collisionMap.put( new CollidingKey( i ), i );
            }
            for ( int i = 0; i < 10; i++ )
            {
                collisionMap.remove( new CollidingKey( i ) );
            }
            assertEquals( 0, collisionMap.size() );

            for ( int i = 100; i < 110; i++ )
            {
                collisionMap.put( new CollidingKey( i ), i );
            }
            assertEquals( 10, collisionMap.size() );
            for ( int i = 100; i < 110; i++ )
            {
                assertEquals( i, collisionMap.get( new CollidingKey( i ) ) );
            }
            for ( int i = 0; i < 10; i++ )
            {
                assertNull( collisionMap.get( new CollidingKey( i ) ) );
            }
        }

        @Test
        @DisplayName( "pathological hashCodes (0, negative, Integer.MIN_VALUE) work" )
        void pathologicalHashCodes()
        {
            final TDDHashMap<HashKey, String> hashKeyMap = new TDDHashMap<>();
            final int[] hashes = { 0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, -( Integer.MIN_VALUE + 1 ) };

            for ( int i = 0; i < hashes.length; i++ )
            {
                hashKeyMap.put( new HashKey( i, hashes[i] ), "v" + i );
            }

            assertEquals( hashes.length, hashKeyMap.size() );
            for ( int i = 0; i < hashes.length; i++ )
            {
                assertEquals( "v" + i, hashKeyMap.get( new HashKey( i, hashes[i] ) ) );
                assertEquals( "v" + i, hashKeyMap.remove( new HashKey( i, hashes[i] ) ) );
            }
            assertEquals( 0, hashKeyMap.size() );
        }
    }

    @Nested
    @DisplayName( "growth and capacity" )
    class Growth
    {
        @Test
        @DisplayName( "the map grows beyond any initial capacity and keeps all entries" )
        void growsUnbounded()
        {
            final TDDHashMap<Integer, Integer> intMap = new TDDHashMap<>();
            final int count = 100_000;

            for ( int i = 0; i < count; i++ )
            {
                intMap.put( i, i * 2 );
            }

            assertEquals( count, intMap.size() );
            for ( int i = 0; i < count; i++ )
            {
                assertEquals( i * 2, intMap.get( i ) );
            }
            assertEquals( count, intMap.keys().size() );
            assertEquals( count, intMap.values().size() );
        }

        @Test
        @DisplayName( "a map full of colliding keys still grows and stays correct" )
        void growthUnderFullCollision()
        {
            final TDDHashMap<CollidingKey, Integer> collisionMap = new TDDHashMap<>();
            final int count = 1_000;

            for ( int i = 0; i < count; i++ )
            {
                collisionMap.put( new CollidingKey( i ), i );
            }

            assertEquals( count, collisionMap.size() );
            for ( int i = 0; i < count; i++ )
            {
                assertEquals( i, collisionMap.get( new CollidingKey( i ) ) );
            }
        }

        @Test
        @DisplayName( "heavy put/remove churn keeps the map consistent with a reference HashMap" )
        void churnAgainstReferenceMap()
        {
            final TDDHashMap<Integer, String> actual = new TDDHashMap<>();
            final Map<Integer, String> expected = new HashMap<>();
            final Random random = new Random( 20260921L );

            for ( int round = 0; round < 20_000; round++ )
            {
                final int key = random.nextInt( 500 );
                if ( random.nextInt( 3 ) == 0 )
                {
                    // remove
                    assertEquals( expected.remove( key ), actual.remove( key ),
                            "remove mismatch at round " + round + " key " + key );
                }
                else
                {
                    // put (sometimes null values)
                    final String value = random.nextInt( 5 ) == 0 ? null : "v" + random.nextInt( 1000 );
                    assertEquals( expected.put( key, value ), actual.put( key, value ),
                            "put mismatch at round " + round + " key " + key );
                }

                assertEquals( expected.size(), actual.size(), "size mismatch at round " + round );
            }

            // full verification of the final state
            assertEquals( expected.keySet(), new HashSet<>( actual.keys() ) );
            assertEquals( expected.size(), actual.values().size() );
            for ( final Map.Entry<Integer, String> entry : expected.entrySet() )
            {
                assertTrue( actual.keys().contains( entry.getKey() ) );
                assertEquals( entry.getValue(), actual.get( entry.getKey() ),
                        "value mismatch for key " + entry.getKey() );
            }
        }

        @Test
        @DisplayName( "repeated fill and clear cycles stay correct" )
        void fillAndClearCycles()
        {
            final TDDHashMap<Integer, Integer> intMap = new TDDHashMap<>();

            for ( int cycle = 0; cycle < 10; cycle++ )
            {
                for ( int i = 0; i < 1_000; i++ )
                {
                    intMap.put( i, i + cycle );
                }
                assertEquals( 1_000, intMap.size() );
                for ( int i = 0; i < 1_000; i++ )
                {
                    assertEquals( i + cycle, intMap.get( i ) );
                }
                intMap.clear();
                assertEquals( 0, intMap.size() );
            }
        }
    }

    @Nested
    @DisplayName( "generic key and value types" )
    class GenericTypes
    {
        @Test
        @DisplayName( "works with Integer keys and Integer values" )
        void integerKeyValue()
        {
            final TDDHashMap<Integer, Integer> intMap = new TDDHashMap<>();
            intMap.put( 1, 10 );
            intMap.put( 2, 20 );

            assertEquals( 10, intMap.get( 1 ) );
            assertEquals( 20, intMap.get( 2 ) );
            assertEquals( Integer.valueOf( 10 ), intMap.remove( 1 ) );
            assertNull( intMap.get( 1 ) );
        }

        @Test
        @DisplayName( "works with custom object values" )
        void customValueTypes()
        {
            record Person( String name, int age ) {}
            final TDDHashMap<String, Person> personMap = new TDDHashMap<>();
            final Person alice = new Person( "Alice", 30 );

            personMap.put( "alice", alice );

            assertEquals( alice, personMap.get( "alice" ) );
            assertEquals( List.of( alice ), personMap.values() );
        }

        @Test
        @DisplayName( "keys are looked up by equals, not identity" )
        void lookupByEqualsNotIdentity()
        {
            final List<String> keys = new ArrayList<>();
            final TDDHashMap<String, Integer> stringMap = new TDDHashMap<>();

            for ( int i = 0; i < 10; i++ )
            {
                final String key = new String( "key" + i ); // fresh instance, no interning
                keys.add( key );
                stringMap.put( key, i );
            }
            for ( int i = 0; i < 10; i++ )
            {
                assertEquals( i, stringMap.get( new String( "key" + i ) ) );
            }
        }
    }
}

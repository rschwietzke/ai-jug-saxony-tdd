I want to create a new implementation of an open hashing map and all required tests. The following method signatures have to be supported. K and V are the generics. Open hashing means no separate chaining in case of collisions. no wrappers for storeage. Allocation free storage except for the backing structure.

* public TDDHashMap()
* public V get( final K key )
* public V put( final K key, final V value )
* public V remove( final K key )
* public int size()
* public List<K> keys()
* public List<V> values()
* public void clear()

* not thread-safe
* free to choose collision strategy
* unbound capacity
* no null keys, but null values are permitted
* maven 
* junit 5, java 21, org.jugsaxony.tdd
* name: TDDHashMap.java

**Important:** Implement the test cases first, run a TDD style process. Stop after the creation of the test cases and let a human review. Wait for a 'go' before implementing the concrete code.

--

log this conversation into doc/implementation.md
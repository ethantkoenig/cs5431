package network;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The FixedSizeSet is a set that holds MAX_ENTRIES and deletes the oldest element if one tries to
 * add another elements. Similar to a circular queue, but with O(1) lookup.
 *
 * @version 1.0, March 5 2017
 */
public class FixedSizeSet<T> {
    private final static int MAX_ENTRIES = 100;

    private Set<T> set = Collections.newSetFromMap(new LinkedHashMap<T, Boolean>(){
        protected boolean removeEldestEntry(Map.Entry<T, Boolean> eldest) {
            return size() > MAX_ENTRIES;
        }
    });

    public FixedSizeSet() {
    }

    public void add(T element){
        set.add(element);
    }

    public boolean contains(T element){
        return set.contains(element);
    }
}

package sourcecodeanalysis;

import java.io.Serializable;
import java.util.*;

/**
 * HashMap核心源码分析(基于JDK 1.8)
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class HashMapAnalysis<K,V> extends AbstractMap<K,V>
        implements Map<K,V>, Cloneable, Serializable {

    /**
     * HashMap特性简介:
     * 1.HashMap的key和value都可以为null，并且不保证遍历的顺序就是放入时的元素顺序
     * 	 key重复时value会被替换，不同的key可以拥有相同的value
     *
     * 2.在hash均匀分布元素的情况下，每次get/put的时间消耗都是相等的
     * 	  遍历hashMap所需的时间和其容量成正比(容量指桶的数量以及每个桶中Entry的数量)
     *   所以若是对遍历的速度非常在意的情况，千万不能将初始桶的数量设置太多或者将负载因子设置太低，否则遍历会非常的慢
     *
     * 3.影响HashMap性能的因素有两个:初始容量 capacity 以及 负载因子  loader
     *   capacity:桶的数量，可以在创建HashMap时自己设置初始capacity,若不设置，默认初始capacity为16.
     *   loader:负载因子，影响hashMap最多可以装多少个元素的条件之一.
     *   threshold: = capacity * loader,这个值为hashMap当前能够装下的最多元素数量，下次放入元素前则需要先做rehash操作
     *
     * 4.一般来讲，负载因子初始为0.75是一个时间和空间成本的折衷结果。
     *   如果将负载因子设置的过高，虽然hashmap可以存放更多的元素，但是遍历时每个桶中因为有过多的Entry所以需要消耗更多时间
     *   所以在设置初始容量时需要考虑到当前场景下这个hashmap大小，尽可能设置合适的初始capacity和loader以减少rehash操作带来的开销
     *
     * 5.hash碰撞:如果两个entry的key的hashcode是相同的，那么它们应该放在同一个桶中并以链表的形式存储，这就是哈希碰撞。
     *
     * 6.rehash:当此次放入元素超过当前 capcity * loader时，需要先执行该操作。
     *   rehash会把hashmap当前桶的数量变为2倍(以达到hashmap的扩容效果)，然后将原有的每个key-value对重新计算索引放入相应的桶中，是一个非常耗时的操作.
     *   注：rehash操作在1.8之后改变了方式，具体看下面的分析。
     *
     * 7.特别强调：HashMap是线程不安全的!!!
     *   若多个线程并发访问同一个HashMap实例对象，且至少有一个线程对它的操作为“将map结构变化的操作”，
     *   那么必须在外部就对其进行加锁，且通常是对封装了map的对象进行加锁操作。
     *   *（将map结构变化的操作：指的是添加或删除键值对的操作,仅仅改变值不被认为是该种操作）
     *
     * 8.若不存在第4点中所说的“封装了map的对象”，那么就必须在创建HashMap时就对其进行包装以得到一个线程安全的HashMap，采用下述方法：
     *   Map m = Collections.synchronizedMap(new HashMap(...));
     *
     * 9.HashMap迭代器的返回是快速-失败(fail-fast)的。
     *   迭代器创建后，一旦在迭代操作期间碰到“将map结构变化的操作”（迭代器自己提供的删除方法除外），
     *   迭代器会立刻抛出  ConcurrentModificationException 这个异常。
     *   因此，迭代期间一旦有并发修改的操作，相较于允许该操作而导致需要面对将来不知何时可能发生的不确定的后果所承担的风险，迭代器选择干净利落的直接返回失败。
     *
     * 10.一般来讲，迭代器无法保证迭代时的并发修改操作能够完全符合预期的情况。
     *   因此如果看到 ConcurrentModificationException 这个异常,应该知道是迭代器迭代期间有其他线程进行了修改map结构操作导致发生异常，
     *   而不应该错误的认为程序执行是正确的！
     *
     * 11.JDK 1.8 之后，HashMap在桶中的entry数量达到一定值后，会将这些entry转化为红黑树的形式存储，以便提高查询和遍历的效率
     *
     * 12.HashMap的优点:遍历、插入删除的速度都很快
     *
     * 13.HashMap的缺点:
     *
     */


    /**  HashMap用到的核心属性 */

    //初始容量（桶的数量）,默认为16
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    //最大支持容量，2^30
    static final int MAXIMUM_CAPACITY = 1 << 30;

    //负载因子，默认0.75
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    //当一个桶中的entry数量大于8时，就满足了链表转化为树结构存储的其中一个条件
    static final int TREEIFY_THRESHOLD = 8;

    //当一个桶中的entry数量小于6时，将这个桶中的键值对转化为桶+链表的结构存储
    static final int UNTREEIFY_THRESHOLD = 6;

    //桶的数量大于64个，链表转化为树结构存储的另一个条件
    static final int MIN_TREEIFY_CAPACITY = 64;

    //标准单向链表的节点组成
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;  //结点哈希值
        final K key;
        V value;
        HashMap.Node<K,V> next;  //指向下一个结点

        Node(int hash, K key, V value, HashMap.Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }
        public final String toString() { return key + "=" + value; }

        public final int hashCode() {
            //直接调用object类的hashcode()方法
            //key和value按位异或，不同为真
            //注意：一个数a两次对同一个值b进行异或操作得到的还是它本身
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (o == this)  //内存地址相同，直接返回true
                return true;
            if (o instanceof Map.Entry) {  //key和value都相同才返回true
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()))
                    return true;
            }
            return false;
        }
    }

    //每个Node计算hash的方法，返回的是最终放的桶的index
    static final int hash(Object key) {  //注意key为null时，默认是放在0桶位的
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    //返回大于所传入的自定义容量的最小2的整数幂(比如传入13，则返回16)
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    //存放桶的数组
    transient HashMap.Node<K,V>[] table;

    //map中所有的键值对
    transient Set<Map.Entry<K,V>> entrySet;

    //map中键值对的数量(实际容量)
    transient int size;

    //结构改变次数，fast-fail机制
    transient int modCount;

    //自定义的rehash操作临界值
    int threshold;

    //自定义的负载因子
    final float loadFactor;


    /**   HashMap用到的核心方法  */


    /**
     *可以自定义初始容量和负载因子的构造方法
     *
     * @param  initialCapacity 初始容量
     * @param  loadFactor      负载因子
     * @throws IllegalArgumentException  传入值边界检查异常
     */
    public HashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                    initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                    loadFactor);
        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * 根据key对象获取结点
     *
     * @param hash key对象的哈希值
     * @param key key对象
     * @return 如果存在该key对应节点，则返回节点；否则返回null
     */
    final HashMap.Node<K,V> getNode(int hash, Object key) {
        HashMap.Node<K,V>[] tab;
        HashMap.Node<K,V> first, e;
        int n;
        K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null) {  //如果桶数组已经初始化且初始大小>0且根据传入key的hash寻找其应该存放的桶里面有结点存在
            if (first.hash == hash && // 总是先检查第一个结点
                    ((k = first.key) == key || (key != null && key.equals(k))))
                return first;  //如果hash值相同并且key也相同，则直接返回（要找的结点就是第一个结点）
            if ((e = first.next) != null) {   //如果该桶中第一个结点还有后续结点,则继续寻找，否则返回null
                if (first instanceof HashMap.TreeNode)  //如果该桶已经是红黑树存储，则直接调用树结点的get方法
                    return ((HashMap.TreeNode<K,V>)first).getTreeNode(hash, key);
                do {  //如果还是链表方式存储，则遍历链表，若有找到hash相同并且key也相同的结点，那么就是需要找的结点，返回该结点;没找到则返回null
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }


    /**
     * Implements Map.put and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to put
     * @param onlyIfAbsent if true, don't change existing value
     * @param evict if false, the table is in creation mode.
     * @return previous value, or null if none
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        HashMap.Node<K,V>[] tab; HashMap.Node<K,V> p; int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, null);
        else {
            HashMap.Node<K,V> e; K k;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;
            else if (p instanceof HashMap.TreeNode)
                e = ((HashMap.TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                for (int binCount = 0; ; ++binCount) {
                    if ((e = p.next) == null) {
                        p.next = newNode(hash, key, value, null);
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                            treeifyBin(tab, hash);
                        break;
                    }
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
            }
        }
        ++modCount;
        if (++size > threshold)
            resize();
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @param value the value to match if matchValue, else ignored
     * @param matchValue if true only remove if value is equal
     * @param movable if false do not move other nodes while removing
     * @return the node, or null if none
     */
    final HashMap.Node<K,V> removeNode(int hash, Object key, Object value,
                                       boolean matchValue, boolean movable) {
        HashMap.Node<K,V>[] tab; HashMap.Node<K,V> p; int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {
            HashMap.Node<K,V> node = null, e; K k; V v;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) {
                if (p instanceof HashMap.TreeNode)
                    node = ((HashMap.TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    do {
                        if (e.hash == hash &&
                                ((k = e.key) == key ||
                                        (key != null && key.equals(k)))) {
                            node = e;
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {
                if (node instanceof HashMap.TreeNode)
                    ((HashMap.TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                else if (node == p)
                    tab[index] = node.next;
                else
                    p.next = node.next;
                ++modCount;
                --size;
                afterNodeRemoval(node);
                return node;
            }
        }
        return null;
    }


    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    final HashMap.Node<K,V>[] resize() {
        HashMap.Node<K,V>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        }
        else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                    (int)ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        HashMap.Node<K,V>[] newTab = (HashMap.Node<K,V>[])new HashMap.Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                HashMap.Node<K,V> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else if (e instanceof HashMap.TreeNode)
                        ((HashMap.TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        HashMap.Node<K,V> loHead = null, loTail = null;
                        HashMap.Node<K,V> hiHead = null, hiTail = null;
                        HashMap.Node<K,V> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            }
                            else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }


    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    final void treeifyBin(HashMap.Node<K,V>[] tab, int hash) {
        int n, index; HashMap.Node<K,V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            HashMap.TreeNode<K,V> hd = null, tl = null;
            do {
                HashMap.TreeNode<K,V> p = replacementTreeNode(e, null);
                if (tl == null)
                    hd = p;
                else {
                    p.prev = tl;
                    tl.next = p;
                }
                tl = p;
            } while ((e = e.next) != null);
            if ((tab[index] = hd) != null)
                hd.treeify(tab);
        }
    }

    









    @Override
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        // TODO Auto-generated method stub
        return null;
    }

}

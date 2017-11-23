package sourcecodeanalysis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap.Node;
import java.util.LinkedHashMap.Entry;

/**
 * LinkedHashMap核心源码分析(基于JDK 1.8)
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class LinkedHashMapAnalysis {

	/**
	 * LinkedHashMap特性简介:
	 * 1.LinkedHashMap继承了HashMap的很大一部分特性，所以源码风格上和HashMap如出一辙
	 *   但是相比HashMap而言，LinkedHashMap还额外维护了一个双向链表，这个链表和HashMap桶中的那个单向链表完全无关
	 *   它只是用来记录每个键值对的前后插入顺序，所以LinkedHashMap其实是"维护了键值对插入顺序的HashMap".
	 *   
	 * 2.LinkedHashMap的key和value都允许为null
	 *   如果key重复了，则key对应的value会被覆盖。多个不同的key可以有一样的value。
	 *   
	 * 3.LinkedHashMap维护的链表存储顺序是从  最近最少使用(链表头) 到  最近最多使用(链表尾)
	 *   每次新插入的键值对会被维护到链表的尾部。
	 *   这个特性使得 LinkedHashMap 非常适合做LRU缓存。
	 * 
	 * 4.注意每次get/put元素，这个结点在双向链表上的位置都会被移到链表的尾部（因为它最近被访问了）
	 * 
	 * 5.特别强调：LinkedHashMap是线程不安全的!!!
     *   若多个线程并发访问同一个LinkedHashMap实例对象，且至少有一个线程对它的操作为“将map结构变化的操作”，
     *   那么必须在外部就对其进行加锁，且通常是对封装了map的对象进行加锁操作。
     *   *（将map结构变化的操作：指的是添加或删除键值对的操作,仅仅改变值不被认为是该种操作）
	 * 
	 * 6.若不存在第5点中所说的“封装了map的对象”，那么就必须在创建LinkedHashMap时就对其进行包装以得到一个线程安全的LinkedHashMap，采用下述方法：
	 *   Map m = Collections.synchronizedMap(new LinkedHashMap(...));</pre>
	 * 
	 * 7.LinkedHashMap迭代器的返回是快速-失败(fail-fast)的。
     *   迭代器创建后，一旦在迭代操作期间碰到“将map结构变化的操作”（迭代器自己提供的删除方法除外），
     *   迭代器会立刻抛出  ConcurrentModificationException 这个异常。
     *   因此，迭代期间一旦有并发修改的操作，相较于允许该操作而导致需要面对将来不知何时可能发生的不确定的后果所承担的风险，迭代器选择干净利落的直接返回失败。
	 * 
	 * 8.一般来讲，迭代器无法保证迭代时的并发修改操作能够完全符合预期的情况。
     *   因此如果看到 ConcurrentModificationException 这个异常,应该知道是迭代器迭代期间有其他线程进行了修改map结构操作导致发生异常，
     *   而不应该错误的认为程序执行是正确的！
	 * 
	 * 9.LinkedHashMap的优点：既有HashMap的查询、插入快的特性，又可以维护键值对的插入顺序
	 * 
	 * 10.LinkedHashMap的缺点:因为需要额外维护一个双向链表，所以在插入键值对的时候比HashMap略微慢一些
	 * 
	 */
	
	
	
	/**  LinkedHashMap用到的核心属性 */
	
    /**
     * LinkedHashMap的Entry对象，除了继承于HashMap的4个属性，还额外添加了两个属性before,after用于维护在双向链表的前后结点关系
     */
    static class Entry<K,V> extends HashMap.Node<K,V> {
        Entry<K,V> before, after;
        Entry(int hash, K key, V value, Node<K,V> next) {
            super(hash, key, value, next);
        }
    }
    
    /**
     * 双向链表的头结点（最近最少使用的结点）
     */
    transient LinkedHashMap.Entry<K,V> head;

    /**
     * 双向链表的尾结点(最近最多使用的结点)
     */
    transient LinkedHashMap.Entry<K,V> tail;

    /**
     * 该属性定义了双向链表中存储结点的顺序:
     * 如果为true，则双向链表按照结点的访问顺序维护前后结点关系(访问、操作结点都会影响该结点在双向链表的位置)
     * 如果为false，则双向链表按照结点的插入顺序维护前后结点关系(只有操作结点的动作才会影响该结点在双向链表的位置)
     * 该值默认为false.
     */
    final boolean accessOrder;

	
    
	/**  LinkedHashMap用到的核心方法  */
	
    
    /**
     * 带capacity和loadFactor的构造函数
     *
     * @param  initialCapacity 自定义初始容量
     * @param  loadFactor      自定义负载因子
     * @throws IllegalArgumentException  初始容量或负载因子为负数，抛出异常
     *         
     */
    public LinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);   //调用HashMap的构造函数
        accessOrder = false;
    }

    /**
     * 只带capacity参数的构造函数，此时loadFactor为默认的0.75
     *
     * @param  initialCapacity 自定义初始容量
     * @throws IllegalArgumentException 初始容量为负数，抛出异常
     */
    public LinkedHashMap(int initialCapacity) {
        super(initialCapacity); //调用HashMap的构造函数
        accessOrder = false;
    }

    /**
     * 无参数构造函数。初始容量为默认的16，负载因子为默认的0.75
     */
    public LinkedHashMap() {
        super();  //调用HashMap的构造函数
        accessOrder = false;
    }


    /**
     * 三个参数的构造函数，可以控制双向链表的存储方式
     *
     * @param  initialCapacity 初始容量
     * @param  loadFactor      负载因子
     * @param  accessOrder     双向链表维护的存储顺序
     * @throws IllegalArgumentException 初始容量或负载因子为负数，抛出异常
     */
    public LinkedHashMap(int initialCapacity,
                         float loadFactor,
                         boolean accessOrder) {
        super(initialCapacity, loadFactor);  //调用HashMap的构造方法
        this.accessOrder = accessOrder;
    }
    
    /**
     * 将结点在双向链表中的位置移动到尾部
     * @param p
     */
    private void linkNodeLast(LinkedHashMap.Entry<K,V> p) {
        LinkedHashMap.Entry<K,V> last = tail;  //将当前双向链表的尾结点保存下来
        tail = p;  //尾结点设置为传入的结点
        if (last == null)  //如果之前链表中没有结点，即这次新增的结点是链表的头结点
            head = p;
        else {  //如果这次新增的结点不是链表的头结点，则将其移动到链表的尾部
            p.before = last;  //当前结点的前驱指向之前链表的尾结点
            last.after = p;  //之前链表尾结点后驱指向当前结点
        }
    }
    
    /**
     * LinkedHashMap重写了HashMap的newNode()方法
     * 该方法在HashMap的putVal()方法中判断需要新增结点时会被调用
     * @param hash
     * @param key
     * @param value
     * @param e
     * @return
     */
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> e) {
        LinkedHashMap.Entry<K,V> p =
            new LinkedHashMap.Entry<K,V>(hash, key, value, e);  //新增一个key-value的结点
        linkNodeLast(p);  //LinkedHashMap除了新增一个结点外，还将该结点在双向链表中的位置移动到尾部，这个操作默认按元素插入顺序维护了链表前后结点关系
        return p;
    }
    
    
    
    /**
     * 某个结点被删除后的回调方法
     * @param e
     */
    void afterNodeRemoval(Node<K,V> e) { // unlink
        LinkedHashMap.Entry<K,V> p =
            (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null)
            head = a;
        else
            b.after = a;
        if (a == null)
            tail = b;
        else
            a.before = b;
    }

    /**
     * 某个结点被插入后的回调方法
     * @param evict
     */
    void afterNodeInsertion(boolean evict) { // possibly remove eldest
        LinkedHashMap.Entry<K,V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    /**
     * 某个结点被访问后的回调方法
     * @param e
     */
    void afterNodeAccess(Node<K,V> e) { // move node to last
        LinkedHashMap.Entry<K,V> last;
        if (accessOrder && (last = tail) != e) {
            LinkedHashMap.Entry<K,V> p =
                (LinkedHashMap.Entry<K,V>)e, b = p.before, a = p.after;
            p.after = null;
            if (b == null)
                head = a;
            else
                b.after = a;
            if (a != null)
                a.before = b;
            else
                last = b;
            if (last == null)
                head = p;
            else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    
}

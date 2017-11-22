package sourcecodeanalysis;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.util.*;
import java.util.HashMap.Node;

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
     * 13.HashMap的缺点:在多线程并发情况下可能会导致死循环
     * 
     * 14.为什么HashMap的table容量都是2的整数幂？
     *    因为二进制计算比十进制计算快,resize只需向左移动一位即可
     * 
     * 15.为什么查找结点应该放在哪个桶时  (n - 1) & hash 使用的是这种写法?
     * 	     为什么不直接使用  n & hash 的写法?
     * 	     因为table的容量永远是2的整数幂，换算成二进制最后一位永远为0
     *    而hash转化为二进制最后一位有可能是1，也有可能是0，但是&操作是两个同时为真才是真
     *    所以如果直接用 n & hash 的写法，那么与出来的结果二进制最后一位永远为0
     *    但是n-1一定为奇数，而奇数的二进制最后一位一定为1
     *    所以用 (n - 1) & hash 这种写法 在hash最后一位也为1是，&的二进制结果最后一位就是1
     *    这个结果就是table的奇数索引处也能够存放结点了，否则最后一位永远为0，那么table的奇数索引处永远也放不了结点，造成空间浪费，哈希碰撞非常严重
     * 
     * 16.为什么 resize() 方法中有使用    e.hash & oldCap 把当前桶中的链表上所有结点分为高位和低位两部分？
     *    因为oldCap永远是2的整数幂，而hash的二进制数每一位都有可能是1，也有可能是0
     *    所以理论上来说这么写能够将链表中的结点较为平均的分为高位和低位两部分
     *    较之1.7及之前的rehash操作每次resize，每个元素都需要重新计算hash值来讲效率提升了很多
     *    这也是为什么用  & 而不是用 % 计算余数的原因.
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
            //key和value的hashcode按位异或，不同为真
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

    //rehash操作临界值
    int threshold;

    //自定义的负载因子
    final float loadFactor;


    /**   HashMap用到的核心方法  */

    /**
     * 无参构造方法，只设置了默认的负载因子
     */
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }
    
    /**
     * 带初始容量参数的构造方法
     * @param initialCapacity  自定义table初始容量
     */
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

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
                            ((k = e.key) == key || (key != null && key.equals(k))))  //key有可能是基本数据类型，也有可能是对象
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }


    /**
     * 根据传入的key和value添加/设置值
     * 若原先table中已存在该key对象对应的键值对，则将其值更改为新的value
     * 若原先table中不存在该key对象对应键值对，则计算其所需放入的bucket位置，将其放入
     *
     * @param hash key的hash值
     * @param key key对象
     * @param value 新的value
     * @param onlyIfAbsent 如果为true，则不改变已存在键值对的值
     * @param evict 如果为false，则table处于creation模式
     * @return  之前有键值对存在，则返回之前该key对应的value；否则返回null 
     */
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        HashMap.Node<K,V>[] tab; 
        HashMap.Node<K,V> p; //某个桶的首结点
        int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)  //如果buckets数组还没有初始化，先调用resize()方法初始化table
            n = (tab = resize()).length;   //记录下此时table的大小(桶的数量)
        if ((p = tab[i = (n - 1) & hash]) == null)  //如果根据hash值计算出该键值对应该放的桶位置，若此时该位置还没有结点存在
            tab[i] = newNode(hash, key, value, null);  //直接将该键值对设置为该桶的第一个结点
        else {  //执行到这里，说明发生碰撞，即tab[i]不为空，需要组成单链表或红黑树
            HashMap.Node<K,V> e;
            K k;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;  //发现该位置处第一个结点的key就是新传入键值对的key，则将该结点记录下来
            else if (p instanceof HashMap.TreeNode)  //如果桶中首结点已经是以红黑树结构存储
                e = ((HashMap.TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);  //调用树的put()方法
            else {  //桶中结点依旧还是链表存储
                for (int binCount = 0; ; ++binCount) {  //遍历链表，binCount记录已遍历结点个数
                    if ((e = p.next) == null) {  //如果当前遍历到的结点为空,说明之前遍历过的结点没有和当前传入键值对key相同的结点，需要新增该结点
                        p.next = newNode(hash, key, value, null);  //新增结点
                        if (binCount >= TREEIFY_THRESHOLD - 1) //如果新增该结点后，当前桶中的结点个数大于树化的界定值
                            treeifyBin(tab, hash);   //转为红黑树结构存储
                        break;
                    }
                    if (e.hash == hash &&   //如果遍历过程中在桶中找到了和当前传入键值对key相同的结点，则将该结点记录下来
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        break;
                    p = e;
                }
            }
            if (e != null) { //如果之前在桶中找到了和当前传入键值对key相同的结点，则将值进行替换
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;  //返回原先该结点的value
            }
        }
        ++modCount;  //该操作属于更改map结构的操作
        if (++size > threshold)  //如果插入该结点后map总结点个数大于阙值，则需要扩容和rehash
            resize();
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * 根据传入的key值(必要时可增加value值判断)删除结点
     *
     * @param hash key的hash值
     * @param key key对象
     * @param value 需要判断value时才传入
     * @param matchValue 如果为true，则必须key和value同时和传入结点相同才会被删除
     * @param movable 如果为false，删除时不移动其他结点
     * @return 被删除结点，或没找到要删除结点则返回null
     */
    final HashMap.Node<K,V> removeNode(int hash, Object key, Object value,
                                       boolean matchValue, boolean movable) {
        HashMap.Node<K,V>[] tab; 
        HashMap.Node<K,V> p; 
        int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {  //如果table已经初始化，大小不为0，且根据key的hash计算桶的位置处已有结点存在
            HashMap.Node<K,V> node = null, e; 
            K k; 
            V v;
            if (p.hash == hash &&  //如果要删除的结点就是该桶的第一个结点
                    ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;  //将该结点保存下来
            else if ((e = p.next) != null) { //如果第一个结点不是要删除的结点，则需要先遍历查找到该结点，再删除
                if (p instanceof HashMap.TreeNode)  //如果该桶中首结点是树结点，说明已是树存储结构
                    node = ((HashMap.TreeNode<K,V>)p).getTreeNode(hash, key);  //需调用树的get()方法查找和当前需删除结点key相同的结点
                else {  //否则依旧还是链表存储，遍历链表
                    do {
                        if (e.hash == hash &&
                                ((k = e.key) == key ||
                                        (key != null && key.equals(k)))) {  //如果找到和要删除结点的key相同的结点
                            node = e;  //保存下来
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {  //如果之前在桶中找到了和当前传入键值对key相同的结点
                if (node instanceof HashMap.TreeNode)  //之前找到的结点是树结点
                    ((HashMap.TreeNode<K,V>)node).removeTreeNode(this, tab, movable);  //调用树的remove()方法
                else if (node == p)  //之前找到的结点是桶中的首结点
                    tab[index] = node.next;  //将桶的首结点删除，并将下一个结点设置为首结点
                else  //之前找到的结点是链表中其中一个结点
                    p.next = node.next;  //删除该结点，并将前一个结点后驱指向被删除结点的下一个结点
                ++modCount; //该操作属于更改map结构的操作
                --size;  //map总结点个数减1
                afterNodeRemoval(node);
                return node;  //返回被删除的结点
            }
        }
        return null;
    }


    /**
     * 初始化数组或者扩容为2倍.
     * 初值为空时，则根据初始容量开辟空间来创建数组.
     * 否则， 因为我们使用2的幂定义数组大小，数据要么待在原来的下标， 或者移动到新数组的高位下标
     * 比如初始容量为16，原来有两个数据在index为1的桶中；resize后容量为32，原来那两个数据既可以在index为1的桶，也可以在index为17的桶中
     *
     * @return 扩容后的新数组
     */
    final HashMap.Node<K,V>[] resize() {
        HashMap.Node<K,V>[] oldTab = table;  //记录原数组
        int oldCap = (oldTab == null) ? 0 : oldTab.length;  //记录原数组Capacity
        int oldThr = threshold;  //记录原数组的rehash阙值
        int newCap, newThr = 0;  //新数组capacity,rehash阙值
        if (oldCap > 0) { //如果原数组capacity>0
            if (oldCap >= MAXIMUM_CAPACITY) { //原数组capacity已经达到容量最大值
                threshold = Integer.MAX_VALUE;  //阙值设置为最大值
                return oldTab;   //直接返回原数组
            }
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)  //新数组capacity为原数组两倍，如果此时数组大小还在16至最大值之间
                newThr = oldThr << 1; //新阙值也变为原先的两倍
        }
        else if (oldThr > 0) // 如果原阙值>0,说明调用HashMap构造方法(带阙值参数的那个)时只设置了阙值大小但没有设置capacity
            newCap = oldThr;  //将阙值大小直接赋值给新数组capacity
        else { //如果直接调用HashMap无参构造方法，则初始capacity和阙值都没有被设置，此处给它设置上
            newCap = DEFAULT_INITIAL_CAPACITY;   //初始capacity默认为16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);  //阙值默认为16*0.75=12
        }
        if (newThr == 0) { //如果新阙值为0，重新设置阙值，防止意外情况
            float ft = (float)newCap * loadFactor;  //用新capacity * 当前负载因子得到计算结果
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                    (int)ft : Integer.MAX_VALUE);//若新容量和该计算结果都未达到最大值，则新阙值就是该计算结果；否则新阙值为int最大值
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes","unchecked"})
        HashMap.Node<K,V>[] newTab = (HashMap.Node<K,V>[])new HashMap.Node[newCap];
        table = newTab;  //用新容量初始化新数组
        if (oldTab != null) { //若旧数组已经被初始化 
            for (int j = 0; j < oldCap; ++j) {  //遍历旧数组每个桶，对桶中每一个结点重新计算索引值，放入新数组对应的桶中
                HashMap.Node<K,V> e;
                if ((e = oldTab[j]) != null) {  //如果当前遍历到的桶中第一个结点不为空，才继续往下走，否则直接进行下一个桶的遍历
                    oldTab[j] = null; //将该桶的首结点置空
                    if (e.next == null)  //如果该结点没有后续结点，即该桶中只有这一个结点
                        newTab[e.hash & (newCap - 1)] = e;  //将该结点重新计算index后，放入新数组的桶中
                    else if (e instanceof HashMap.TreeNode)  //如果该结点有后续结点，且该结点已经是树存储
                        ((HashMap.TreeNode<K,V>)e).split(this, newTab, j, oldCap);  //则直接调用树的split方法
                    else { //如果该结点有后续结点，且该桶中结点存储方式还是链表存储
                        HashMap.Node<K,V> loHead = null, loTail = null;
                        HashMap.Node<K,V> hiHead = null, hiTail = null;
                        HashMap.Node<K,V> next;
                        do {
                            next = e.next;
                            //e.hash & oldCap 将该链表的结点均匀分散为新数组低位和高位两个位置
                            if ((e.hash & oldCap) == 0) { // 如果被分到低位，则在新数组中的桶位置和原先的桶是一样的
                                if (loTail == null)  //如果新数组中低位桶尾结点为空，说明该桶当前还没有结点
                                    loHead = e;  //则直接将新数组中该低位桶首结点设置为当前链表中遍历到的结点
                                else //如果新数组中低位桶尾结点不为空
                                    loTail.next = e;  //则将当前链表中遍历到的结点添加到新数组中该低位桶的链表尾部
                                loTail = e; //新数组中该低位桶的链表尾结点设置为该结点
                            }
                            else {  //如果被分到高位，则在新数组中的桶位置为原先所在桶位置+原先桶的capacity
                                if (hiTail == null) //如果新数组中高位桶尾结点为空，说明该桶当前还没有结点
                                    hiHead = e; //则直接将新数组中该高位桶首结点设置为当前链表中遍历到的结点
                                else //如果新数组中高位桶尾结点不为空
                                    hiTail.next = e; //则将当前链表中遍历到的结点添加到新数组中该高位桶的链表尾部
                                hiTail = e; //新数组中该高位桶的链表尾结点设置为该结点
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {  //如果新数组低位桶的尾结点非空
                            loTail.next = null;  //则将其下一个结点设置为null，方便后续结点插入
                            newTab[j] = loHead;  //并将新数组中低位桶首结点设置为loHead中保存的结点
                        }
                        if (hiTail != null) {  //如果新数组高位桶的尾结点非空
                            hiTail.next = null; //则将其下一个结点设置为null，方便后续结点插入
                            newTab[j + oldCap] = hiHead; //并将新数组中高位桶首结点设置为hiHead中保存的结点
                        }
                    }
                }
            }
        }
        return newTab;
    }


    /**
     * 将桶中结点的存储方式更改为红黑树的存储方式
     * 注意：如果当前桶的数量小于64，则不会做树化操作，而是直接调用resize扩容
     */
    final void treeifyBin(HashMap.Node<K,V>[] tab, int hash) {
        int n, index;
        HashMap.Node<K,V> e;
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY) //如果当前数组未初始化或桶的数量小于64则不做树化操作，而是用resize()方法替代
            resize();  
        else if ((e = tab[index = (n - 1) & hash]) != null) { //如果该桶的首结点不为null
            HashMap.TreeNode<K,V> hd = null, tl = null;
            do {  //将每个结点的存储方式更改为红黑树存储
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

    
    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                                             mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                       DEFAULT_INITIAL_CAPACITY :
                       (fc >= MAXIMUM_CAPACITY) ?
                       MAXIMUM_CAPACITY :
                       tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                         (int)ft : Integer.MAX_VALUE);
            @SuppressWarnings({"rawtypes","unchecked"})
                Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                    K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                    V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

}

package sourcecodeanalysis;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap.Entry;

/**
 * TreeMap核心源码分析(基于JDK 1.8)
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class TreeMapAnalysis<K,V>
extends AbstractMap<K,V>
implements NavigableMap<K,V>, Cloneable, java.io.Serializable {

	/**
	 * TreeMap特性简介:
	 * 1.TreeMap底层是基于红黑树实现的
	 * 	 
	 * 2.key不允许为空，value可以为空
	 *   如果key重复，则会将原先的value覆盖
	 *   多个不同的key可以拥有相同的value
	 * 
	 * 3.TreeMap是有序的，它有两种排序模式
	 *   一种是按照key的自然排序维护键值对的顺序
	 *   另一种是在构造函数中传入自定义的排序规则，以这个规则维护键值对的顺序
	 * 
	 * 4.特别强调：TreeMap是线程不安全的!!!
     *   若多个线程并发访问同一个TreeMap实例对象，且至少有一个线程对它的操作为“将map结构变化的操作”，
     *   那么必须在外部就对其进行加锁，且通常是对封装了map的对象进行加锁操作。
     *   *（将map结构变化的操作：指的是添加或删除键值对的操作,仅仅改变值不被认为是该种操作）
     *
     * 5.若不存在第4点中所说的“封装了map的对象”，那么就必须在创建TreeMap时就对其进行包装以得到一个线程安全的TreeMap，采用下述方法：
     *   SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));
     *   
     * 6.TreeMap迭代器的返回是快速-失败(fail-fast)的。
     *   迭代器创建后，一旦在迭代操作期间碰到“将map结构变化的操作”（迭代器自己提供的删除方法除外），
     *   迭代器会立刻抛出  ConcurrentModificationException 这个异常。
     *   因此，迭代期间一旦有并发修改的操作，相较于允许该操作而导致需要面对将来不知何时可能发生的不确定的后果所承担的风险，迭代器选择干净利落的直接返回失败。
     *
     * 7.一般来讲，迭代器无法保证迭代时的并发修改操作能够完全符合预期的情况。
     *   因此如果看到 ConcurrentModificationException 这个异常,应该知道是迭代器迭代期间有其他线程进行了修改map结构操作导致发生异常，
     *   而不应该错误的认为程序执行是正确的！
	 * 
	 * 8.注意TreeMap的Entry是不支持setValue()的.
	 *   如果想要改变某个key对应的value，或者新插入一个Entry，则应该使用put()方法.
	 * 
	 * 9.TreeMap的优点:对于一些统计操作性能较好，时间复杂度为O(log n).
	 * 
	 * 10.TreeMap的缺点:对于增删改查的操作性能较差，时间复杂度同样为O(log n).
	 * 
	 * 11.
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	
	
	
	/**  TreeMap用到的核心属性 */
	
	private static final boolean RED   = false;   //红色节点用false表示
    private static final boolean BLACK = true;  //黑色节点用true表示

    /**
     * 红黑树每个节点对象
     * @author EX_WLJR_CHENZEHUA
     *
     * @param <K>  key
     * @param <V>  value
     */
    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;   //键
        V value; //值
        Entry<K,V> left;  //左子节点引用
        Entry<K,V> right;  //右子节点引用
        Entry<K,V> parent;  //父节点引用
        boolean color = BLACK;  //当前结点颜色

        /**
         * 构造一个叶子节点，默认无左右子节点，颜色为黑色
         */
        Entry(K key, V value, Entry<K,V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * 返回key
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * 返回key对应的value
         *
         * @return the value associated with the key
         */
        public V getValue() {
            return value;
        }

        /**
         * 替换对应的值
         *
         * @return 原先被替换的值
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        /**
         * 重写节点的equals()方法
         */
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            //必须key和value都相同才算相等
            return valEquals(key,e.getKey()) && valEquals(value,e.getValue());
        }

        /**
         * 重写节点的hashCode()方法
         */
        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;  //key的hashCode 异或  value的hashCode
        }

        /**
         * 重写toString()方法
         */
        public String toString() {
            return key + "=" + value;
        }
    }
	
	/** 用户自定义的比较器 */
    private final Comparator<? super K> comparator;

    /** 红黑树根节点  */
    private transient Entry<K,V> root;

    /** 红黑树节点个数  */
    private transient int size = 0;

    /** map结构性操作计数器 */
    private transient int modCount = 0;

	
	
	/**   TreeMap用到的核心方法  */
    
    /**
     * 无参数构造方法，使用key的自然比较顺序来维护树的顺序
     * 注意：map的key如果是自定义对象，则必须实现了 Comparable 接口 (String、Integer等常用类已经实现该接口,所以可以直接用来作为key)
     * 如果Map有限定泛型，则该Map所有的key值必须遵守泛型类型的约束,不得违反，否则Map会报  ClassCastException 异常
     * 比如 Map<String,String>  在该map中调用   map.put(1,"error") 则会报异常!
     */
    public TreeMap() {
        comparator = null;
    }

    /**
     * 带参数构造方法，使用用户传入的比较器来维护树的顺序
     * 注意：map的key如果是自定义对象，则必须实现了 Comparable 接口 (String、Integer等常用类已经实现该接口,所以可以直接用来作为key)
     * 如果Map有限定泛型，则该Map所有的key值必须遵守泛型类型的约束,不得违反，否则Map会报  ClassCastException 异常
     * 比如 Map<String,String>  在该map中调用   map.put(1,"error") 则会报异常!
     * 
     * @param 用户自定义比较器，如果为空，则该Map会使用key的自然排序维护树的顺序
     */
    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }
    
    
    /**
     * 插入节点的操作
     * 如果原先map中已经有该key对应的键值对，则替换原先该key对应的value为新的value
     * 如果原先map中没有该key对应的键值对，则在map中新插入一个该key和value对应的键值对
     *
     * @param key 键
     * @param value 新值
     *
     * @return 返回原先的值 或者 null
     * 
     * @throws ClassCastException 如果用户传入了一个不可和其他key比较的key（违反泛型约定），则抛出该异常
     * @throws NullPointerException key传入了null，则报此异常
     */
    public V put(K key, V value) {
        Entry<K,V> t = root;   //获取根节点
        if (t == null) {  //如果根节点为空，则当前的树还没有初始化
            compare(key, key); // 检查key的类型以及是否为null

            root = new Entry<>(key, value, null); //根据key和value创建一个黑色新节点作为树的根节点
            size = 1;  //结点总数+1
            modCount++;  //结构性操作计数器 +1
            return null;  //直接返回
        }
        //根节点不为空，则执行下面逻辑
        int cmp;  //结点的比较结果  >0  <0  =0
        Entry<K,V> parent;
        // 区分是使用key的自然排序进行结点比较 还是 使用用户传入的比较器进行结点的比较
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {  //如果用户传入了自定义比较器
            do {
                parent = t;  //每次进行比较的节点，一开始t变量保存的是树的根节点
                cmp = cpr.compare(key, t.key); //使用自定义比较器的compare方法，对传入的结点和当前遍历到结点的key进行比较
                if (cmp < 0)  //如果传入节点的key比当前遍历到节点的key小
                    t = t.left;  //把下次进行比较的节点设置为当前遍历到的节点的左子节点
                else if (cmp > 0)  //如果传入节点的key比当前遍历到节点的key大
                    t = t.right; //把下次进行比较的节点设置为当前遍历到的节点的右子节点
                else  //如果传入节点的key和当前遍历到节点的key一样大
                    return t.setValue(value);  //说明这是一个替换原有key对应的value的操作,替换完成后直接返回（不需要再进行下面的插入操作）
            } while (t != null);  //直到遍历到某一个叶子结点才结束,t变量保存的是当前遍历到的叶子节点
        }
        else {  //没有自定义比较器，使用key的自然排序进行比较
            if (key == null)
                throw new NullPointerException();  //如果传入key为null，则报异常
            @SuppressWarnings("unchecked")
                Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;  //每次进行比较的节点，一开始t变量保存的是树的根节点
                cmp = k.compareTo(t.key); //使用Comparable接口的compareTo方法，对传入的结点和当前遍历到结点的key进行比较
                if (cmp < 0)  //以下步骤和上面if的步骤完全相同，不再赘述
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        //执行到这里说明遍历了整个树后没有发现存在与传入key相同的键值对，则需要将传入的键值对插入到树中
        Entry<K,V> e = new Entry<>(key, value, parent);  //根据当前传入的key和value新建一个黑色节点
        if (cmp < 0)  //如果之前最后一次比较的结果是传入的key比当时叶子结点的key小
            parent.left = e;  //那么就将当时叶子结点的左子结点设置为当前传入的结点，当前传入的结点变为新的叶子节点
        else  //如果之前最后一次比较的结果是传入的key比当时叶子结点的key大
            parent.right = e;  //那么就将当时叶子结点的右子结点设置为当前传入的结点，当前传入的结点变为新的叶子节点
        
        fixAfterInsertion(e);  //红黑树的核心方法:在插入后通过左旋、右旋、变色将当前树变成符合红黑树规定的树
        
        size++;  //节点总数+1
        modCount++;  //结构性操作计数器 +1
        return null;  //返回null
    }
    
    /**  平衡树的相关操作  */
    
    /**  返回当前节点的颜色，如果节点为空则默认返回黑色 */
    private static <K,V> boolean colorOf(Entry<K,V> p) {
        return (p == null ? BLACK : p.color);
    }

    /**  返回当前节点的父节点，没有父节点则返回空 */
    private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
        return (p == null ? null: p.parent);
    }

    /**  给当前结点设置颜色  */
    private static <K,V> void setColor(Entry<K,V> p, boolean c) {
        if (p != null)
            p.color = c;
    }

    /**  返回当前节点的左子节点，没有左子节点则返回空 */
    private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
        return (p == null) ? null: p.left;
    }

    /**  返回当前节点的右子节点，没有右子节点则返回空 */
    private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
        return (p == null) ? null: p.right;
    }
    
    
    /** 树的左旋操作 */
    private void rotateLeft(Entry<K,V> p) {
        if (p != null) {
            Entry<K,V> r = p.right;
            p.right = r.left;
            if (r.left != null)
                r.left.parent = p;
            r.parent = p.parent;
            if (p.parent == null)
                root = r;
            else if (p.parent.left == p)
                p.parent.left = r;
            else
                p.parent.right = r;
            r.left = p;
            p.parent = r;
        }
    }

    /** 树的右旋操作(可参考右旋方法流程图、一次右旋节点变化图) */
    private void rotateRight(Entry<K,V> p) {
        if (p != null) {   //如果待右旋节点p不为空
            Entry<K,V> l = p.left;  //获取待右旋节点p的左子节点l
            p.left = l.right;  //将p的左子节点指向l的右子节点
            //l的右子节点不为空
            if (l.right != null) l.right.parent = p; //l的右子节点的父节点指向p（p与l的右子节点建立父子节点关系）
            l.parent = p.parent;  //l的父节点指向p的父节点(相当于l取代p的位置)
            if (p.parent == null) //p父节点是否为空?
                root = l;  //p就是根节点，则将当前根节点变更为l
            else if (p.parent.right == p) //p是否为其父节点的右子节点?
                p.parent.right = l;  //p的父节点的右子节点指向l
            else p.parent.left = l;   //p的父节点的左子节点指向l
            l.right = p;  //l的右子节点指向p
            p.parent = l;  //p的父节点指向l
        }
    }
    
    
    /**
     * 树插入一个新结点后，将其根据红黑树的规则进行修正(可参考FixAfterInsertion流程图)
     * @param x   当前插入树的节点
     */
    private void fixAfterInsertion(Entry<K,V> x) {
        //默认将当前插入树的节点颜色设置为红色，为什么???
    	//因为红黑树有一个特性: "从根节点到所有叶子节点上的黑色节点数量是相同的"
    	//如果当前插入的节点是黑色的，那么必然会违反这个特性，所以必须将插入节点的颜色先设置为红色
    	x.color = RED;
    	//第一次遍历时，x变量保存的为当前新插入的节点
    	//为什么要用while循环?
    	//因为在旋转的过程中可能还会出现父子节点均为红色的情况，所以要不断往上遍历直至整个树都符合红黑树的规则
        while (x != null && x != root && x.parent.color == RED) { //如果当前节点不为空且不是根节点，并且当前节点的父节点颜色为红色
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) { //如果当前节点的父节点等于当前节点父节点的父节点的左子节点（即当前节点为左子节点插入）
            	
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));  //获取当前节点的叔父节点(和当前插入节点的父节点同辈的另外那个节点)
                if (colorOf(y) == RED) {  //如果叔父节点的颜色为红色
                	//以下4步用来保证不会连续出现两个红色节点
                    setColor(parentOf(x), BLACK);  //将当前节点的父节点设置为黑色
                    setColor(y, BLACK);  //将当前节点的叔父节点设置为黑色
                    setColor(parentOf(parentOf(x)), RED);  //将当前节点的祖父节点设置为红色
                    x = parentOf(parentOf(x));  //当前遍历节点变更为当前节点的祖父节点
                } else {  //如果叔父节点的颜色为黑色,或没有叔父节点
                    if (x == rightOf(parentOf(x))) {  //如果当前节点为左子树内侧插入
                        x = parentOf(x);  //将x变更为当前节点的父节点
                        rotateLeft(x);  //对当前节点的父节点进行一次左旋操作（旋转完毕后x对应的就是最左边的叶子节点）
                    }
                    //如果当前节点为左子树外侧插入
                    setColor(parentOf(x), BLACK); //将当前节点的父节点设置为黑色
                    setColor(parentOf(parentOf(x)), RED); //将当前节点的祖父节点设置为红色
                    rotateRight(parentOf(parentOf(x)));  //对当前节点的祖父节点进行一次右旋
                }
            } else {  //当前节点为右子节点插入
                Entry<K,V> y = leftOf(parentOf(parentOf(x)));  //以下步骤与上面基本相似，只是旋转方向相反，不再赘述
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);
                    setColor(y, BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    x = parentOf(parentOf(x));
                } else {
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    setColor(parentOf(x), BLACK);
                    setColor(parentOf(parentOf(x)), RED);
                    rotateLeft(parentOf(parentOf(x)));
                }
            }
        }
        root.color = BLACK;//注意在旋转的过程中可能将根节点变更为红色的，但红黑树的特性要求根节点必须为黑色，所以无论如何最后总要执行这行代码,将根节点设置为黑色
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
	
}

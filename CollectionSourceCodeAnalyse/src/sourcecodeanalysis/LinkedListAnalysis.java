package sourcecodeanalysis;

import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.LinkedList.Node;
import java.util.function.Consumer;

/**
 * LinkedList核心源码分析(基于JDK 1.8)
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class LinkedListAnalysis<E> extends AbstractSequentialList<E>
implements List<E>, Deque<E>, Cloneable, java.io.Serializable{

	/**
	 * LinkedList特性简介:
	 * 1.LinkedList底层实现是一个双向链表，允许任意类型元素放入（包括null），
	 *   LinkedList是有序的，且可以存放重复数据。
	 * 
	 * 2.链表：双向链表是由一个个Node组成的线性结构，每一个Node都是由下面三个元素按从左到右的顺序组成
	 *   prev(存放前一个Node的地址,和前一个Node的next地址相同) item(这个Node真正的数据) next(存放该Node的地址，和下一个Node的prev地址相同)
	 *   注:因为LinkedList是双向链表，所以最后一个Node的next地址应该和第一个Node的prev地址相同
	 * 
	 * 3.特别强调：LinkedList是线程不安全的!!!
	 *   若多个线程并发访问同一个LinkedList实例对象，且至少有一个线程对它的操作为“将list结构变化的操作”，
	 *   那么必须在外部就对其进行加锁，且通常是对封装了list的对象进行加锁操作。
	 *   *（将list结构变化的操作：指的是添加或删除元素或明确调整数组大小的操作,仅仅改变list中元素的值不被认为是该种操作）
	 *   
	 * 4.若不存在第4点中所说的“封装了list的对象”，那么就必须在创建LinkedList时就对其进行包装以得到一个线程安全的LinkedList，采用下述方法：
	 *   List list = Collections.synchronizedList(new LinkedList(...));
	 *   
	 * 5.LinkedList迭代器的返回是快速-失败(fail-fast)的。
	 *   迭代器创建后，一旦在迭代操作期间碰到“将list结构变化的操作”（迭代器自己提供的增加和删除元素的方法除外），
	 *   迭代器会立刻抛出  ConcurrentModificationException 这个异常。
	 *   因此，迭代期间一旦有并发修改的操作，相较于允许该操作而导致需要面对将来不知何时可能发生的不确定的后果所承担的风险，迭代器选择干净利落的直接返回失败。
	 *   
	 * 6.一般来讲，迭代器无法保证迭代时的并发修改操作能够完全符合预期的情况。
	 *   因此如果看到 ConcurrentModificationException 这个异常,应该知道是迭代器迭代期间有其他线程进行了修改list结构操作导致发生异常，
	 *   而不应该错误的认为程序执行是正确的！
	 *   
	 * 7.LinkedList的优点：因为其底层实现是基于链表的，而针对链表做增加和删除功能是非常方便的（只需调整前后两个Node的Next和Prev的指向即可）
	 *   所以LinkedList做 add()、remove()效率是很高的
	 *   
	 * 8.LinkedList的缺点: 同样因为底层是基于链表实现，所以若要取某个位置的值就比较麻烦，需要先判断这个位置距离链表头较近还是链表尾较近
	 *   然后从较近的一段开始遍历链表直到要找的元素所在的位置，所以LinkedList做查找的操作效率很低
	 *   
	 * 9.特别注意：千万不要用普通的for循环去遍历LinkedList！！速度慢到令人发指！
	 *   为什么？从第8点可知LinkedList每找一次元素都要从较近的那端开始一个一个遍历，在数据量大的情况下效率极其低下!
	 *   针对这种情况需要使用迭代器进行遍历 (或者foreach也可以，其实foreach底层就是迭代器，效率是一样的)
	 *   详细比对情况见 example包下的例子.
	 */
	
	
	/** 每一个节点，其实是一个内部类    */
    private static class Node<E> {
        E item;   //实际元素的值
        Node<E> next;   //指向下一个节点的地址
        Node<E> prev;   //指向上一个节点的地址

        Node(Node<E> prev, E element, Node<E> next) {  //节点构造方法
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
	
    
	/**  LinkedList用到的核心属性  */
	transient int size = 0;  //LinkedList的底层链表的大小
	
	transient Node<E> first;   //头节点，即第一个节点  transient表示序列化时该对象不进行序列化
	
	transient Node<E> last;  //尾节点，即最后一个节点  transient表示序列化时该对象不进行序列化
	
	
	
	/**  LinkedList用到的核心方法  */
	
	
	 /**
	  * 直接返回LinkedList的大小
	  */
	 public int size() {
	      return size;
	 }
	
	
	/**
     * add方法，默认把元素添加到链表的尾部
     *
     * @param e 要添加的元素
     * @return true 添加成功
     */
    public boolean add(E e) {
        linkLast(e);   //实际调用的是linkLast(e)这个方法
        return true;
    }
	
    /**
     * 将元素e设置为链表尾部节点
     */
    void linkLast(E e) {
        final Node<E> l = last;  //将原先的尾节点赋值给 l变量(可能为空)
        final Node<E> newNode = new Node<>(l, e, null); //新建一个保存元素e的节点，前驱指向原来的尾节点 l，后驱指向为null
        last = newNode;  //将当前的尾节点更新为新建的这个节点
        if (l == null)  //若原先尾节点为null，说明这个链表原先就是空的，此次放入的e元素是其第一个节点
            first = newNode;  //将当前链表的头节点设置为新增的e元素节点（此时链表中有且只有一个节点,头节点和尾节点都是这个e元素节点）
        else  //若原先尾节点不是null，则说明原先链表里是有值的
            l.next = newNode;  //那么将原先的尾节点(已经在上面赋值给了 l 变量)的后驱指向新建的这个节点即可(这个新建节点已经在上面设置为了尾节点)
        size++;   //链表大小加1
        modCount++;  //这个添加节点操作是 "更改list结构"的操作，计数器需要加1
    }

    
    /**
     * 删除链表中第一个和指定元素相同的节点，删除成功返回true
     * 链表中没有要删除的元素节点，则返回false
     *
     * @param o 需要删除的元素
     * @return true/false
     */
    public boolean remove(Object o) {
        if (o == null) {   //如果要删除的元素为 null
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {   //找到第一个为null的元素
                    unlink(x);  //调用unlink()方法删除节点
                    return true;
                }
            }
        } else {   //如果要删除元素非null
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {   //找到第一个和该元素相等的节点
                    unlink(x);  //调用unlink()方法删除节点
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 删除节点
     */
    E unlink(Node<E> x) {
        // assert x != null;        
        final E element = x.item;   //分别获取要删除节点的  前驱、元素、后驱对象
        final Node<E> next = x.next;
        final Node<E> prev = x.prev;

        if (prev == null) {  //如果当前要删除节点指向的前驱节点为空，则说明要删除的节点就是头节点
            first = next;  //将头节点变更为当前删除节点的下一个节点
        } else {   //如果当前要删除的节点不是头节点
            prev.next = next;  //将前一个节点的后驱指向当前被删除节点的下一个节点
            x.prev = null;  //被删除节点的前驱置为null
        }

        if (next == null) {   //如果当前要删除节点指向的后驱节点为空，则说明要删除的节点就是尾节点
            last = prev;  //将尾节点变更为当前删除节点的前一个节点
        } else {   //如果当前要删除的节点不是尾节点
            next.prev = prev;  //将后一个节点的前驱指向当前被删除节点的上一个节点
            x.next = null;  //被删除节点的后驱置为null
        }

        x.item = null;  //被删除节点元素置为null
        size--;  //链表大小加1
        modCount++;  //这个添加节点操作是 "更改list结构"的操作，计数器需要加1
        return element;  //返回被删除节点的元素
    }
    
    /**
     * 删除链表中所有元素，列表大小为空
     */
    public void clear() {
        // 删除链表中所有的节点
        // 节点前驱、元素、后驱全置为空是为了保证同时让根节点搜索和引用计数两种垃圾回收检测算法能够回收到这些节点的空间
        for (Node<E> x = first; x != null; ) {  //循环链表，把每一个节点都置为null
            Node<E> next = x.next;
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }
        first = last = null;  //头、尾节点置空
        size = 0;   //链表大小变为0
        modCount++;  //这个清空节点操作是 "更改list结构"的操作，计数器需要加1
    }
    
    
    
    /**
     * 返回指定位置处节点的元素值
     *
     * @param 指定位置
     * @return 节点元素
     * @throws 越界异常
     */
    public E get(int index) {
        checkElementIndex(index);  //检查用户指定索引位置是否越界
        return node(index).item;  //返回指定位置处节点的元素值
    }
    
    /**
     * 返回指定位置处的节点
     */
    Node<E> node(int index) {
        // assert isElementIndex(index);
    	// LinkedList是双向链表，所以可以进行双向查找，效率提高
        if (index < (size >> 1)) {  //如果指定位置在list大小的一半之前，则从前向后查找
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {   //如果指定位置在list大小的一半之后，则从后向前查找
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }
    
    
    /**
     * 将指定位置处节点的元素值替换为自定义的值
     *
     * @param 指定位置
     * @param 要替换成自定义的值
     * @return 原先被替换了的值
     * @throws 越界异常
     */
    public E set(int index, E element) {
        checkElementIndex(index);  //检查索引是否越界
        Node<E> x = node(index);  //获取原先指定位置处的节点对象
        E oldVal = x.item;  //将原先该节点的值保存下来
        x.item = element;  //将该节点的元素值替换为新值
        return oldVal;  //返回该节点原先的元素值
    }
    
    
    /**
     * 维持元素顺序的迭代器
     */
    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned;  //上一次遍历过的节点
        private Node<E> next;  //下一个节点
        private int nextIndex;  //下一个节点的索引位置
        private int expectedModCount = modCount;  //fail-fast 计数器

        ListItr(int index) {  //指定索引创建迭代器的构造函数
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);  //索引位置超出了list最后一个节点，则直接为null
            nextIndex = index;
        }

        public boolean hasNext() { //是否有下一个节点
            return nextIndex < size;
        }

        public E next() {  //返回下一个节点的元素值，并将下一个节点的索引光标往后移动一个节点
            checkForComodification(); //先检查是否在迭代器遍历时有其他线程做了"改变list结构的操作"
            if (!hasNext())  //先判断是否有下一个节点，若没有则抛异常
                throw new NoSuchElementException();

            lastReturned = next;  //将当前下一个节点索引光标指向的节点 赋值给 "上一次遍历的节点"
            next = next.next;  //将下一个节点设置为   当前下一个节点索引光标指向节点的下一个节点
            nextIndex++;  //下一个节点索引光标往后移动一个节点
            return lastReturned.item;  //返回此时  "上一次遍历的节点" 中的节点元素值
        }

        public boolean hasPrevious() {   //是否有前一个节点
            return nextIndex > 0;
        }

        public E previous() { //返回上一个节点的元素值，并将下一个节点的索引光标往前移动一个节点
            checkForComodification(); //先检查是否在迭代器遍历时有其他线程做了"改变list结构的操作"
            if (!hasPrevious()) //先判断是否有上一个节点，若没有则抛异常
                throw new NoSuchElementException();

            //先判断下一个节点是否为null(当前节点为尾节点)
            //是则直接将尾节点赋值给  "上一次返回的节点"
            //不是则将当前  "下一个阶段索引光标指向节点的前一个节点" 赋值给  "上一次遍历的节点"
            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;  //下一个节点索引光标往前移动一个节点
            return lastReturned.item;   //返回此时  "上一次遍历的节点" 中的节点元素值
        }

        public int nextIndex() {  //返回下一个节点索引光标
            return nextIndex;
        }

        public int previousIndex() {   //返回上一个节点索引光标
            return nextIndex - 1;
        }

        public void remove() {   //删除上一个遍历过的节点
            checkForComodification(); //先检查是否在迭代器遍历时有其他线程做了"改变list结构的操作"
            if (lastReturned == null)  //如果还没遍历过就调用remove()方法，异常
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;  //得到上一次返回节点的下一个节点
            unlink(lastReturned);  //删除上一次遍历的节点
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;  //下一个节点索引光标往前移动一个节点
            lastReturned = null; //将上一次遍历的节点置null
            expectedModCount++;  //计数器加1
        }

        public void set(E e) {   //设置上一次遍历过的节点的元素值为自定义的值
            if (lastReturned == null)  //还没有遍历过数据就调用set()方法，异常
                throw new IllegalStateException();  
            checkForComodification();  //先检查是否在迭代器遍历时有其他线程做了"改变list结构的操作"
            lastReturned.item = e;  //将上次遍历过的节点的元素值设置为自定义的值
        }

        public void add(E e) {   //添加节点
            checkForComodification(); //先检查是否在迭代器遍历时有其他线程做了"改变list结构的操作"
            lastReturned = null;  //上次遍历过的值置null
            if (next == null)  //如果当前节点为尾节点，直接调用linkLast()方法将新节点添加到链表尾部
                linkLast(e);
            else   //如果不是尾节点，则调用linkBefore()方法，在当前节点插入一个新的节点
                linkBefore(e, next);
            nextIndex++;  //下一个节点索引光标往后移动一个节点
            expectedModCount++; //计数器加1
        }

        //若当前计数器数量和初始化迭代器时计数器数量不一致，则抛出异常
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
    
    
    /**
	 *	将LinkedList底层链表节点中的元素按顺序序列化到流中
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // static 和  transient修饰的变量不参与序列化
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * 将流中的数据按顺序反序列化到LinkedList底层的链表中
     */
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // static 和  transient修饰的变量不参与反序列化
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E)s.readObject());
    }
    		
}

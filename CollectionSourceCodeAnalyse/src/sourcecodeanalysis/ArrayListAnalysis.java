package sourcecodeanalysis;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.ArrayList.Itr;
import java.util.ArrayList.ListItr;
import java.util.function.Consumer;

/**
 * ArrayList核心源码分析(基于JDK 1.8)
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class ArrayListAnalysis<E> extends AbstractList<E> 
		implements List<E>, RandomAccess, Cloneable, java.io.Serializable{

	/**
	 * ArrayList特性简介:
	 * 1.ArrayList底层实现是可调整大小的数组，允许任意类型元素放入（包括null），
	 *   ArrayList是有序的，且可以存放重复数据。
	 *   
	 * 2.ArrayList内部有个capacity变量，它就是ArrayList的总容量，随着元素添加进list这个capacity会自动增长。
	 * 
	 * 3.如果确定某一次添加的元素数量远大于该list当前的剩余空闲空间，可以调用ensureCapacity()方法，
	 *   该方法可以一次性将list的容量进行扩充，这样可以避免原本需要多次扩容而造成的性能消耗。
	 *   
	 * 4.特别强调：ArrayList是线程不安全的!!!
	 *   若多个线程并发访问同一个ArrayList实例对象，且至少有一个线程对它的操作为“将list结构变化的操作”，
	 *   那么必须在外部就对其进行加锁，且通常是对封装了list的对象进行加锁操作。
	 *   *（将list结构变化的操作：指的是添加或删除元素或明确调整数组大小的操作,仅仅改变list中元素的值不被认为是该种操作）
	 *   
	 * 5.若不存在第4点中所说的“封装了list的对象”，那么就必须在创建ArrayList时就对其进行包装以得到一个线程安全的ArrayList，采用下述方法：
	 *   List list = Collections.synchronizedList(new ArrayList(...));
	 *   
	 * 6.ArrayList迭代器的返回是快速-失败(fail-fast)的。
	 *   迭代器创建后，一旦在迭代操作期间碰到“将list结构变化的操作”（迭代器自己提供的增加和删除元素的方法除外），
	 *   迭代器会立刻抛出  ConcurrentModificationException 这个异常。
	 *   因此，迭代期间一旦有并发修改的操作，相较于允许该操作而导致需要面对将来不知何时可能发生的不确定的后果所承担的风险，迭代器选择干净利落的直接返回失败。
	 *   
	 * 7.一般来讲，迭代器无法保证迭代时的并发修改操作能够完全符合预期的情况。
	 *   因此如果看到 ConcurrentModificationException 这个异常,应该知道是迭代器迭代期间有其他线程进行了修改list结构操作导致发生异常，
	 *   而不应该错误的认为程序执行是正确的！
	 *   
	 * 8.ArrayList的优点：因为其底层是数组的实现，所以获取元素时只需直接取数组索引处的值即可，非常快
	 *   并且由于它是顺序的，所以往list末尾添加元素也是非常快的  
	 *   
	 * 9.ArrayList的缺点：同样因为其是数组的实现，所以在指定index处插入元素时需要把原先index处及其后所有的元素往右移动一位
	 *   而删除指定index处元素时需要将原先index之后的所有元素往左移动一位，效率很低
	 *   
	 * 10.因为ArrayList是基于数组的，所以遍历时只需根据索引取值即可，故使用foreach和普通for循环遍历效率差别不大
	 * 
	 */
	
	
	/**  ArrayList用到的核心属性  */
	
	/**
     * ArrayList默认初始容量为10
     */
    private static final int DEFAULT_CAPACITY = 10;
	
    
    /**
     * 底层存放ArrayList元素的数组,当前ArrayList的总容量就是这个数组的长度
     * 为什么要定义为transient?transient表示数组不参与序列化，那么如何将ArrayList中的元素进行序列化呢？
     * 其实ArrayList已经重写了序列化/反序列化方法，因为数组的容量不等于list元素的总个数，如果直接把整个数组都进行序列化会造成浪费
     * 所以重写的方法会先算出有多少个元素，然后遍历数组，只取有元素的数组索引处的值进行序列化
     * 这样可以加快序列化的速度，同时减少了序列化后文件的大小，非常好的想法!
     */
    transient Object[] elementData;
	
    /**
     * 当前ArrayList中元素的个数
     * @serial
     */
    private int size;
	
	
    
    /**  ArrayList用到的核心方法  */
	
    /**
     * 带参构造函数，使用自定义大小来初始化一个空的ArrayList
     *
     * @param  initialCapacity  ArrayList初始容量
     * @throws IllegalArgumentException 若传入的自定义大小为负数，则报此异常
     *         
     */
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
        	//初始化指定大小的数组
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
        	//初始化一个空的数组
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
        	//若用户传入自定义容量大小 < 0,则抛出此异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }
    
    /**
     * 无参构造函数，初始化一个大小为10,元素为空的数组
     */
    public ArrayList() {
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }
	
    
    
    /**
     * 对外提供的ArrayList扩容方法
     * 保证扩容后的list容量 >= 传入的minCapacity大小
     *
     * @param   minCapacity   期望最小扩容后的大小
     */
    public void ensureCapacity(int minCapacity) {
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            // 当前ArrayList容量是否比初始容量10小？是则从当前大小开始扩展，否则直接从10开始扩展
            ? 0
            : DEFAULT_CAPACITY;

        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }
    
    
    private void ensureExplicitCapacity(int minCapacity) {
        //注意这个方法是"改变list结构的操作"，modCount是一个计数器,用来记录这种操作发生的次数!
    	modCount++;

        // overflow-conscious code
        if (minCapacity - elementData.length > 0)
            grow(minCapacity);
    }
    
    /**
     * 扩容的核心方法
     * @param minCapacity 期望最小扩容后的大小
     */
    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        //新容量为旧容量的1.5倍.为什么是1.5倍？
        //如果一次扩容太大，则会造成内存浪费；如果一次扩容太小，则势必很快又需要扩容，而扩容是一项很损耗性能的操作
        //所以JDK开发人员进行了时间和空间的折衷，以旧容量的1.5倍为新容量（注意是大小是int类型，即 3扩容后不是4.5,而是4!）
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
        	//若期望的最小扩容后的大小比这次扩容操作后的大小要来的大，则说明这次扩容操作不足以满足期望大小，直接将list的新容量设置为期望容量
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
        	//如果新容量即将达到Integer最大值*(2^31-1)，则调用超大扩容方法,避免扩容后数组大小超过Integer最大值
            newCapacity = hugeCapacity(minCapacity);
        // 通常来讲用户期望的扩容后大小一般为list当前元素个数的2倍，所以新容量为旧容量的1.5倍是一种非常棒的策略
        // 这里的操作其实分为2步：
        // 1.先将原数组扩容，返回一个新的、扩容后的空数组
        // 2.调用Arrays的copyOf方法，将原数组对应索引处的值拷贝到新数组对应索引处
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
    
    /**
     * 超大扩容
     * @param 期望最小扩容后的大小
     * @return
     */
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // 内存溢出
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?  //用户传入的期望扩容后大小超过Integer最大值，则直接返回Integer最大值
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }
	
    /**
     * 返回list中元素的个数.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return size;
    }

    /**
     * 如果list中没有元素，返回true;否则返回false.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    
    /**
     * 检查用户传入的索引值是否超出list当前元素总个数,若超出则抛异常
     */
    private void rangeCheck(int index) {
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
    

    /**
     * 返回list底层数组中指定索引处的元素
     *
     * @param  index 需要返回元素的索引
     * @return 数组中指定索引处的元素
     * @throws IndexOutOfBoundsException 数组越界异常
     */
    public E get(int index) {
        rangeCheck(index);  //先检查用户指定索引是否超出list当前元素数量

        return elementData(index);
    }
    
    @SuppressWarnings("unchecked")
    E elementData(int index) {
    	//直接取底层数组中index处的元素值返回
        return (E) elementData[index];
    }
    
    
    /**
     * 以用户给定的值替换list底层数组index处的元素.
     *
     * @param index 需要替换list中元素所在的数组索引
     * @param element 用来替换的新值
     * @return 数组该索引处原先的值
     * @throws IndexOutOfBoundsException 数组越界异常
     */
    public E set(int index, E element) {
        //注意set操作并不是"将list结构变化的操作",所以不需要记录ConcurrentModificationException的变化次数!
    	
    	rangeCheck(index); //先检查用户指定索引是否超出list当前元素数量

        E oldValue = elementData(index);  //获取旧值
        elementData[index] = element;  //替换为新值
        return oldValue;  //返回该索引处原先的旧值
    }
    
    
    /**
     * 将用户传入的元素添加到当前list末尾元素所在数组索引的下一位置
     *
     * @param e 要添加的元素
     * @return 成功返回true，失败返回false
     */
    public boolean add(E e) {
    	//保证当前list的数组大小能容纳再添加一个元素进去,如果不能，则扩容
        ensureCapacityInternal(size + 1);  // 该操作会引起ConcurrentModificationException!
        //数组索引从0开始，size从1开始，所以当前size处的数组索引就是list末尾元素的后一个位置，也即这个新元素应该存放的位置
        //新元素添加至末尾后，list的总元素个数加1
        elementData[size++] = e;
        return true;
    }

    /**
     * 将指定元素插入当前list的数组中自定义索引位置处
     * 并把该索引位置处原先的元素及其后位置的元素全部往右边移动一个索引的位置
     *
     * @param index 要插入新元素的数组索引位置
     * @param element 插入的新元素
     * @throws IndexOutOfBoundsException  数组越界异常
     */
    public void add(int index, E element) {
        rangeCheckForAdd(index);  //检查用户传入的index是否在list元素总个数范围内
        //保证当前list的数组大小能容纳再添加一个元素进去,如果不能，则扩容
        ensureCapacityInternal(size + 1); // 该操作会引起ConcurrentModificationException!
        //先将该位置原先的元素及其后所有元素全部往右移动一个索引的位置
        System.arraycopy(elementData, index, elementData, index + 1,
                         size - index);
        //然后用新元素替换该索引处原先的元素
        elementData[index] = element;
        //全部完成后list的总元素个数加1
        size++;
    }
    
    private void ensureCapacityInternal(int minCapacity) {
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        ensureExplicitCapacity(minCapacity);
    }
    
    
    /**
     * 删除list的数组指定index处的元素
     * 并将该index向后的元素全部往左移动一个index位置
     *
     * @param index 需要删除的元素所在数组的索引位置
     * @return 该索引位置原先被删除的元素值
     * @throws IndexOutOfBoundsException 数组越界异常
     */
    public E remove(int index) {
        rangeCheck(index); //先检查用户指定索引是否超出list当前元素数量

        modCount++;  //注意这个方法是"改变list结构的操作",计数器+1
        E oldValue = elementData(index);  //获取原先数组index位置的元素值

        int numMoved = size - index - 1; //计算需要往左移动的元素个数
        if (numMoved > 0)  
        	//如果有元素需要移动（即用户指定要删除的index并不是list末尾元素所在的index），则将需要移动的元素全部向左移动一个index位置
        	//如果为0，则表示用户指定要删除的index位置就是当前list末尾元素所在的index，则无需做移动操作
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        //先将原来list末尾元素所在index处的值置为null，以便让GC发现对其进行回收
        //再将list总元素个数-1
        elementData[--size] = null;

        return oldValue;  //返回被删除的原先数组index位置的元素值
    }

    /**
     * 删除list中第一个符合用户期望删除的元素(支持删除null)
     * 如果用户期望删除的元素在list中不存在，则不做任何事,返回false
     * 如果用户期望删除的元素在list中存在，则返回true.
     *
     * @param o 用户期望在list中删除的元素(如果存在的话)
     * @return  若list中存在用户期望删除的元素，返回true
     */
    public boolean remove(Object o) {
        if (o == null) {
            for (int index = 0; index < size; index++)
                if (elementData[index] == null) { //如果用户期望删除null，则循环数组，找到第一个值为null的索引位置，删除该位置的值
                    fastRemove(index);
                    return true;
                }
        } else {
            for (int index = 0; index < size; index++)
                if (o.equals(elementData[index])) { //循环数组,找到第一个符合用户期望删除的元素所处索引位置，删除该位置的值
                    fastRemove(index);
                    return true;
                }
        }
        return false;
    }

    /*
     * 内部私有的删除方法，不做边界检查也不返回任何值
     */
    private void fastRemove(int index) {
        modCount++;   //注意这个方法是"改变list结构的操作",计数器+1
        int numMoved = size - index - 1;  //计算需要往左移动的元素个数
        if (numMoved > 0)
        	//如果有元素需要移动（即用户指定要删除的index并不是list末尾元素所在的index），则将需要移动的元素全部向左移动一个index位置
        	//如果为0，则表示用户指定要删除的index位置就是当前list末尾元素所在的index，则无需做移动操作
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);
        //先将原来list末尾元素所在index处的值置为null，以便让GC发现对其进行回收
        //再将list总元素个数-1
        elementData[--size] = null;
    }

    
    /**
     * 删除list中所有元素，这个list在方法结束后会变成空的list
     * 空的数组是否会触发GC???
     * 
     * 
     */
    public void clear() {
        modCount++;  //注意这个方法是"改变list结构的操作",计数器+1

        //循环遍历数组，把list每个元素所在数组索引处的值置为null
        for (int i = 0; i < size; i++)
            elementData[i] = null;

        size = 0;  //将list中元素总个数置为0
    }
    
    
    /**
     * 将ArrayList底层数组中的元素按顺序序列化到流中
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{
        //记录序列化之前的计数器数量
        int expectedModCount = modCount;
        s.defaultWriteObject();

        //序列化list中元素的总个数
        s.writeInt(size);

        // 按list中元素顺序将每一个元素序列化到流中
        for (int i=0; i<size; i++) {
            s.writeObject(elementData[i]);
        }
        
        //如果方法结束时，计数器数量不等于序列化方法开始时计数器的数量，说明在序列化过程中有别的线程对list做了"改变list结构的操作"
        //此时序列化的list元素个数是不准确的，所以方法执行失败，抛出 ConcurrentModificationException
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * 将流中的数据按顺序反序列化到list底层的数组中
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        elementData = EMPTY_ELEMENTDATA;  //初始数组为空

        s.defaultReadObject();

        //读取反序列化的元素个数
        s.readInt();

        if (size > 0) {
            //如果需要反序列化的元素个数>0,则需要先扩容，以保证数组能够容纳这些元素
            ensureCapacityInternal(size);

            Object[] a = elementData;
            // 按流中的顺序将每个元素反序列化到数组中
            for (int i=0; i<size; i++) {
                a[i] = s.readObject();
            }
        }
    }
    
    
    /**
     * 返回一个普通的迭代器
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * 普通迭代器，只有hasNext()、Next()、 Remove()、checkForComodification()四个方法
     */
    private class Itr implements Iterator<E> {
        int cursor;       //下一个元素的索引,默认为第一个元素的索引
        int lastRet = -1; // 上一个元素的索引，若没有则为-1
        int expectedModCount = modCount;  //计数器，记录list有过"改变list结构"操作的次数

        public boolean hasNext() {  //判断是否还有下一个元素
            return cursor != size;  //如果下一个元素的索引不为list末尾元素的索引，返回true;否则返回false
        }

        @SuppressWarnings("unchecked")
        public E next() {  //获取下一个元素
            checkForComodification();  //先检查是否在遍历时有其他线程做了"改变list结构的操作"
            int i = cursor;  //获取下一个元素的索引
            if (i >= size)   //如果下一个元素的索引超出了list末尾元素的索引，报错
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;  //获取list底层数组
            if (i >= elementData.length)  //如果下一个元素的索引超出了数组当前最大capacity,报错(有别的线程做了"改变list结构的操作")
                throw new ConcurrentModificationException();
            cursor = i + 1;  //把迭代器的光标再往后移动一个索引位置
            return (E) elementData[lastRet = i];  //返回下一个元素的值
        }

        public void remove() {   //默认删除的是迭代器当前光标处上一个元素
            if (lastRet < 0)   //没有上一个元素的索引，报错
                throw new IllegalStateException();
            checkForComodification();  //先检查是否在遍历时有其他线程做了"改变list结构的操作"

            try {
            	ArrayList.this.remove(lastRet);  //调用ArrayList自己的Remove(int index)方法
                cursor = lastRet;  //删除上一个元素后，当前光标所指的元素就移到被删除元素的位置了，所以迭代器的光标也要跟着往上移一个索引位置
                lastRet = -1;
                expectedModCount = modCount;  //重新记录一下当前计数器的值,方便迭代器中别的方法执行前判断是否有别的操作在这期间做了"改变list结构的操作"
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        //若当前计数器数量和初始化迭代器时计数器数量不一致，则抛出异常
        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }
    
    
    /**
     * 返回一个ArrayList专有迭代器，该迭代器支持双向移动
     * 注意这个迭代器也是fail-fast的
     *
     */
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }
    
    
    /**
     * An optimized version of AbstractList.ListItr
     * 继承了普通迭代器，所以也拥有它的 hasNext()、Next()、 Remove()、checkForComodification()四个方法
     */
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {  //list迭代器构造方法
            super();
            cursor = index;
        }

        public boolean hasPrevious() {  //是否有前一个元素
            return cursor != 0;   //cursor为0时表示此时迭代器游标正指向第一个元素
        }

        public int nextIndex() {  //获取下一个元素的索引
            return cursor;
        }

        public int previousIndex() {   //返回的是当前元素的索引（此时cursor正指向下一个元素）
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public E previous() {  //返回上一个元素
            checkForComodification();
            int i = cursor - 1;  //返回当前光标索引的前一个位置（即上一个元素的索引）
            if (i < 0)  //边界值检查
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;  //获得底层数组
            if (i >= elementData.length) //上一个元素索引大于数组最大索引，说明期间有别的线程做了"更改list结构的操作"，报异常
                throw new ConcurrentModificationException();
            cursor = i;
            return (E) elementData[lastRet = i];  //返回光标索引处的元素
        }

        public void set(E e) {   //设置当前索引处的值为用户自定义的值
            if (lastRet < 0)  //如果没有调用过迭代器的 next()或previous()方法就设置值，报错
                throw new IllegalStateException();
            checkForComodification();  //检查是否有别的线程做了 "更改list结构"的操作

            try {
                ArrayList.this.set(lastRet, e);  //调用ArrayList自己的set方法，设置值
            } catch (IndexOutOfBoundsException ex) {  //越界，报异常
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {  //在迭代器当前位置添加一个元素
            checkForComodification(); //检查是否有别的线程做了 "更改list结构"的操作

            try {
                int i = cursor;  //获取原先下一个元素的索引位置
                ArrayList.this.add(i, e);  //调用ArrayList底层add方法，在该位置新增一个元素
                cursor = i + 1;  //将光标移动到移动到原先元素所在的位置，因为此时调用next()方法应该返回的是新增的这个元素！
                lastRet = -1;  //没有上一个元素
                expectedModCount = modCount; //重新记录一下当前计数器的值,方便迭代器中别的方法执行前判断是否有别的操作在这期间做了"改变list结构的操作"
            } catch (IndexOutOfBoundsException ex) {  //越界，报异常
                throw new ConcurrentModificationException();
            }
        }
    }
    	
}

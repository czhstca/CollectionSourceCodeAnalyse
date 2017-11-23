package examples;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HashMap在多线程环境下因扩容会引起死循环示例(多运行几次才能遇到死循环情况)
 * 
 * 导致这个情况的原因是多线程进行扩容时,如果线程A在执行resize过程中被另一个线程B抢占了执行机会
 * 当线程B完成resize后，可能此时主内存中链表的指向顺序和线程A此时链表的指向顺序存在正好相反的情况(A->B,B->A)
 * 那么当下次另一个线程再resize时需要先遍历这个链表，当碰到这个"形成环"的指向结点时，就会发生死循环。
 * 
 * 所以切记区分清楚当前业务是否可能在多线程中使用，若需要，则可以使用JUC包下的ConcurrentHashMap类来代替HashMap.
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class HashMapLoopThread extends Thread {

	private static AtomicInteger ai = new AtomicInteger(0);
    private static Map<Integer, Integer> map = new HashMap<Integer, Integer>(1);
	
	@Override
	public void run() {
		while (ai.get() < 1000000)
        {
            map.put(ai.get(), ai.get());
            ai.incrementAndGet();
        }
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		HashMapLoopThread hmt0 = new HashMapLoopThread();
		HashMapLoopThread hmt1 = new HashMapLoopThread();
		HashMapLoopThread hmt2 = new HashMapLoopThread();
		HashMapLoopThread hmt3 = new HashMapLoopThread();
		HashMapLoopThread hmt4 = new HashMapLoopThread();
	    hmt0.start();
	    hmt1.start();
	    hmt2.start();
	    hmt3.start();
	    hmt4.start();
		
	}

}

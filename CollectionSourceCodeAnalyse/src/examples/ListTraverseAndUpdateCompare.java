package examples;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * 比较ArrayList和LinkedList的查找和更新元素效率的示例类
 * @author EX_WLJR_CHENZEHUA
 *
 * 结论: 查询数据始终应该使用 ArrayList;
 *	         对于更新数据，有如下结论:
 *     1.若每次都是往尾部插入数据，则无论选择ArrayList还是LinkedList效果都差不多;
 *     2.若每次都是在头部插入数据，则应该选择LinkedList (ArrayList需要频繁移动数据);
 *	   3.若每次都是在中间位置插入数据，则应该选择ArrayList (此时LinkedList的插入优势已经完全被其遍历劣势所掩盖);
 *
 */
public class ListTraverseAndUpdateCompare {

	private List<Integer> arrayList = new ArrayList<Integer>(10000010);
	private List<Integer> linkedList = new LinkedList<Integer>();
	private long beginTime;
	private long endTime;
	
	{
		//初始化两个List
		for(int i=0;i<10000000;i++){
			arrayList.add(i, i);
		}
		
		for(int j=0;j<10000000;j++){
			linkedList.add(j, j);
		}
	}
	
	/**
	 * 比较查询元素的效率
	 */
	public void queryCompare(){
		int temp = 0;
		
		//ArrayList底层数组维护了索引，所以无论查询哪个位置的值，只需通过索引就能快速定位并取得值，理论上消耗时间无限接近0ms
		//LinkedList底层是双向链表，所以查询元素时可以从头也可以从尾开始查找,理论上链表中间值花费的时间应该最多
		//此处以10000000个元素为基础，通过查找第1个、第10000000个和第5000000个元素来比较两个List的查找效率
		
		//两个List都查询第一个元素
		//测试结果：两个List获取第一个元素耗时都为0ms.
		beginTime = System.currentTimeMillis();
		temp = arrayList.get(0);
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList获取第一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		beginTime = System.currentTimeMillis();
		temp = linkedList.get(0);
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList获取第一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		//两个List都查询最后一个元素
		//测试结果：两个List获取最后一个元素耗时都为0ms.
		beginTime = System.currentTimeMillis();
		temp = arrayList.get(9999999);
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList获取最后一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		beginTime = System.currentTimeMillis();
		temp = linkedList.get(9999999);
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList获取最后一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		//两个List都查询中间结果，第5000000个元素
		//测试结果：ArrayList获取始终耗时0ms
		//        LinkedList获取平均耗费时间在15ms
		beginTime = System.currentTimeMillis();
		temp = arrayList.get(5000000);
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList获取最中间一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		beginTime = System.currentTimeMillis();
		temp = linkedList.get(5000000);
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList获取最中间一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
	}
	
	
	/**
	 * 比较更新元素的效率(以插入操作为例)
	 */
	public void updateCompare(){
		//ArrayList底层是数组，如果需要在某个位置插入元素，需要把该位置后的元素全都向右移动一个索引位置，效率很低(插入位置为第一个效率最低，往后效率越来越好)
		//LinkList底层为链表，插入操作只需要断开原来两个节点前驱和后驱引用，分别和这个新的节点建立引用即可，效率很高
		//但是插入前需要先遍历到指定位置，遍历的效率其实并不高，所以具体要看插入的位置在哪里
		
		//两个List都在第一个位置插入元素
		//测试结果:  LinkedList始终花费0ms
		//		   ArrayList有时花费0ms，有时花费15ms，结果不稳定 (与现代CPU性能过强有关)
		beginTime = System.currentTimeMillis();
		arrayList.add(0, -1);
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList插入位置为第一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		beginTime = System.currentTimeMillis();
		linkedList.add(0, -1);
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList插入位置为第一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
	
		//两个List都在最后一个位置插入元素
		//测试结果：两个List在最后一个位置插入元素耗费时间都为0ms
		beginTime = System.currentTimeMillis();
		arrayList.add(9999999, -1);
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList插入位置为最后一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		beginTime = System.currentTimeMillis();
		linkedList.add(9999999, -1);
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList插入位置为最后一个元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		
		//两个List都在中间位置插入元素
		//测试结果：ArrayList始终耗费0ms
		//		 LinkedList耗费48ms,此时LinkedList的插入优势已经被其遍历劣势给完全盖过
		beginTime = System.currentTimeMillis();
		arrayList.add(5000000, -1);
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList插入位置为中间元素耗费时间: " + (endTime - beginTime) + " ms........");
		
		beginTime = System.currentTimeMillis();
		linkedList.add(5000000, -1);
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList插入位置为中间元素耗费时间: " + (endTime - beginTime) + " ms........");
		
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ListTraverseAndUpdateCompare ltac = new ListTraverseAndUpdateCompare();
		
		//ltac.queryCompare();
		
		//ltac.updateCompare();
	}

}

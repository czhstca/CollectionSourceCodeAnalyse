package examples;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 比较普通for循环和foreach循环对ArrayList和LinkedList的效率区别
 * 
 * 结论:  ArrayList使用普通for循环遍历
 *	 	 特别注意： LinkedList千万不能用普通for循环遍历，会悲剧的!!!!
 * 
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class ListTraverseWaysCompare {

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
	 * 普通 for循环遍历
	 * 测试结果：ArrayList耗费16ms
	 * 		   LinkedList耗费时间无限长！！！
	 */
	public void commonForTraverse(){
		int temp = 0;
		
		beginTime  = System.currentTimeMillis();
		for(int i=0;i<arrayList.size();i++){
			arrayList.get(i);
		}
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList使用普通for循环遍历，耗费时间  " + (endTime - beginTime) + "  ms...............");
		
		
		beginTime  = System.currentTimeMillis();
		for(int i=0;i<linkedList.size();i++){
			linkedList.get(i);
		}
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList使用普通for循环遍历，耗费时间  " + (endTime - beginTime) + "  ms...............");
		
	}
	
	/**
	 * 迭代器(for-each)遍历
	 * 测试结果: ArrayList花费16ms左右
	 *         LinkedList花费110ms左右
	 */			
	public void forEachTraverse(){
		int temp = 0;
		beginTime  = System.currentTimeMillis();
		for(Integer i : arrayList){
			temp = i;
		}
		endTime = System.currentTimeMillis();
		System.out.println("ArrayList使用foreach循环遍历，耗费时间  " + (endTime - beginTime) + "  ms...............");
		
		beginTime  = System.currentTimeMillis();
		for(Integer i : linkedList){
			temp = i;
		}
		endTime = System.currentTimeMillis();
		System.out.println("LinkedList使用foreach循环遍历，耗费时间  " + (endTime - beginTime) + "  ms...............");
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ListTraverseWaysCompare ltw = new ListTraverseWaysCompare();
		
		//ltw.commonForTraverse();
		
		//ltw.forEachTraverse();
	}

}

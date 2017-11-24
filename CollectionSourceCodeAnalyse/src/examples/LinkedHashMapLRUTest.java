package examples;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 使用LinkedHashMap实现LRU的测试类
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class LinkedHashMapLRUTest {

	/**
	 * 遍历map，输出每一项键值对
	 */
	public static void loopMap(LinkedHashMap<String, String> linkedHashMap){
		Set<Map.Entry<String, String>> set = linkedHashMap.entrySet();
	    Iterator<Map.Entry<String, String>> iterator = set.iterator();
	    
	    while (iterator.hasNext())
	    {
	        System.out.print(iterator.next() + "\t");
	    }
	    System.out.println();
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		LinkedHashMap<String, String> linkedHashMap =
	            new LinkedHashMap<String, String>(16, 0.75f, true);
	    linkedHashMap.put("111", "111");
	    linkedHashMap.put("222", "222");
	    linkedHashMap.put("333", "333");
	    linkedHashMap.put("444", "444");
	    loopMap(linkedHashMap);
	    linkedHashMap.get("111");
	    loopMap(linkedHashMap);
	    linkedHashMap.put("222", "2222");
	    loopMap(linkedHashMap);
		
	}

}

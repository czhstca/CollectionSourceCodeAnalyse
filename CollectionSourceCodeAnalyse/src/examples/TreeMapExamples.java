package examples;

import java.util.Map;
import java.util.TreeMap;

/**
 * 红黑树的示例 (可参考红黑树案例:相关图片)
 * 
 * 红黑树的特性(只要是红黑树就必须遵守的约定):
 * 1.根节点与叶节点都是黑色节点，其中叶节点为Null节点
 * 2.每个红色节点的左右两个子节点都是黑色节点，换句话说就是不能有连续两个红色节点
 * 3.从根节点到所有叶子节点上的黑色节点数量是相同的
 * 
 * 红黑树具体生成过程可以参考resources的 "红黑树生成样例".doc文件
 * @author EX_WLJR_CHENZEHUA
 *
 */
public class TreeMapExamples {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		TreeMap<Integer, String> treeMap = new TreeMap<Integer, String>();//无参数构造，使用key的自然顺序进行排序
		treeMap.put(10, "10");
		treeMap.put(85, "85");
		treeMap.put(15, "15");
		treeMap.put(70, "70");
		treeMap.put(20, "20");
		treeMap.put(60, "60");
		treeMap.put(30, "30");
		treeMap.put(50, "50");
	 
		for (Map.Entry<Integer, String> entry : treeMap.entrySet()) {
		System.out.println(entry.getKey() + ":" + entry.getValue());
		}
		
	}

}

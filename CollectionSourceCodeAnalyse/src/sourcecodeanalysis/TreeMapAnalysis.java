package sourcecodeanalysis;

import java.util.AbstractMap;
import java.util.NavigableMap;

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
	 * 4.
	 * 
	 * 
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
	
	
	
	
	
	
	
	/**   TreeMap用到的核心方法  */
	
}

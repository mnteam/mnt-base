package com.mnt.base.evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mnt.base.json.JSONTool;
import com.mnt.base.util.CommonUtil;


/**
 * Simple get value evalutaor
 * 
 * @author pengpeng01
 *
 */
public class GetValueEvaluator extends AbstractEvaluator {
	
	private Integer layer;
	private Object[] keys;
	private Object constantValue;

	public GetValueEvaluator(String expression) {
		super(expression);
	}
	
	public void parse() {
		
		if(expression.charAt(0) == S_QUOT && expression.charAt(expression.length() - 1) == S_QUOT) {
			constantValue = expression.substring(1, expression.length() - 1);
		} else if(expression.matches(NUMBER_REGEX)) {
			constantValue = CommonUtil.parseAsDouble(expression);
		} else if(expression.matches(BOOLEAN_REGEX)) { 
			constantValue = Boolean.valueOf(expression);
		} else {
			String[] tokens = expression.split(PERIOD_REGEX);
			
			if(tokens[0].startsWith(MAP_INDEX_PREFIX)) {
				layer = CommonUtil.parseAsInt(tokens[0].substring(MAP_INDEX_PREFIX.length()), -1) - 1;
				if(layer < 0) {
					layer = null;
				}
			}
			
			keys = Arrays.copyOfRange(tokens, layer == null ? 0 : 1, tokens.length);
		}
	}
	
	@Override
	public Object eval(Object[] ms) {
		
		if(constantValue != null) {
			return constantValue;
		} else if(ms != null){
			int i = layer == null ? ms.length - 1 : layer;
			Map<String, Object> m = CommonUtil.uncheckedMapCast(ms.length > i ? ms[i] : null); 
			return format(m != null ? getValue(m) : null);
		} else {
			return null;
		}
	}
	
	private Object getValue(Map<String, Object> map) {
		Object result = map;
		
		for(int i = 0; i < keys.length; i++) {
			
			if(result == null) break;
			
			if(result instanceof Map) {
				map = CommonUtil.uncheckedMapCast(result);
				
				if(map == null) {
					result = null; 
					break;
				} else {
					result = map.get(keys[i]);
				}
				
			} else if(result instanceof List) {
				int index = CommonUtil.parseAsInt(keys[i], -1);
				if(index != -1) {
					List<Object> list = CommonUtil.uncheckedListCast(result);
					if(list != null && index < list.size()) {
						result = list.get(index);
					} else {
						result = null;
						break;
					}
				} else {
					result = null;
					break;
				}
			} else if(result != null && result.getClass().isArray()) {
				int index = CommonUtil.parseAsInt(keys[i], -1);
				if(index != -1) {
					Object[] array = CommonUtil.uncheckedCast(result);
					if(array != null && index < array.length) {
						result = array[index];
					} else {
						result = null;
						break;
					}
				} else {
					result = null;
					break;
				}
			} else if(result != null && result instanceof String) {
				result = JSONTool.convertJsonToObject((String)result);
				i--;
				continue;
			}
		}
		
		return result;
	}
	
	public static void main(String[] args) {
		Map<String, Object> vMap = new HashMap<>();
		
		List<Object> vList = new ArrayList<>();
		vList.add("{'subKey':'asd'}");
		
		vMap.put("a", vList);
		
		Evaluator eval = AbstractEvaluator.parseEvaluator("a.0.subKey");
		System.out.println(eval.eval(vMap));
	}
}

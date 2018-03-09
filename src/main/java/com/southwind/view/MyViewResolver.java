package com.southwind.view;

/**
 * 自定义视图解析器
 * @author southwind
 *
 */
public class MyViewResolver {
	private String prefix;
	private String suffix;
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	@Override
	public String toString() {
		return "MyViewResolver [prefix=" + prefix + ", suffix=" + suffix + "]";
	}
	
	public String jspMapping(String value){
		return this.prefix+value+this.suffix;
	}
}

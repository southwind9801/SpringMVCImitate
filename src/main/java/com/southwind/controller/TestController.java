package com.southwind.controller;

import com.southwind.annotation.MyController;
import com.southwind.annotation.MyRequestMapping;

@MyController
@MyRequestMapping(value = "/testController")
public class TestController {

	@MyRequestMapping(value = "/test")
	public String test(){
		System.out.println("执行test相关业务");
		return "index";
	}
	
}
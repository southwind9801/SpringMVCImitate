package com.southwind.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpRetryException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.southwind.annotation.MyController;
import com.southwind.annotation.MyRequestMapping;
import com.southwind.view.MyViewResolver;

/**
 * DispatcherServlet
 * @author southwind
 *
 */
public class MyDispatcherServlet extends HttpServlet{
	
	//模拟IOC容器，保存Controller实例对象
	private Map<String,Object> iocContainer = new HashMap<String,Object>();
	//保存handler映射
	private Map<String,Method> handlerMapping = new HashMap<String,Method>();
	//自定视图解析器
	private MyViewResolver myViewResolver;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		//扫描Controller，创建实例对象，并存入iocContainer
		scanController(config);
		//初始化handler映射
		initHandlerMapping();
		//加载视图解析器
		loadViewResolver(config);
	}
	
	/**
	 * 扫描Controller
	 * @param config
	 */
	public void scanController(ServletConfig config){
		SAXReader reader = new SAXReader();
		try {
			//解析springmvc.xml
			String path = config.getServletContext().getRealPath("")+"\\WEB-INF\\classes\\"+config.getInitParameter("contextConfigLocation");	
			Document document = reader.read(path);
			Element root = document.getRootElement();
			Iterator iter = root.elementIterator();
			while(iter.hasNext()){
				Element ele = (Element) iter.next();
				if(ele.getName().equals("component-scan")){
					String packageName = ele.attributeValue("base-package");
					//获取base-package包下的所有类名
					List<String> list = getClassNames(packageName);
					for(String str:list){
						Class clazz = Class.forName(str);
						//判断是否有MyController注解
						if(clazz.isAnnotationPresent(MyController.class)){
							//获取Controller中MyRequestMapping注解的value
							MyRequestMapping annotation = (MyRequestMapping) clazz.getAnnotation(MyRequestMapping.class);
							String value = annotation.value().substring(1);
							//Controller实例对象存入iocContainer
							iocContainer.put(value, clazz.newInstance());
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 获取包下的所有类名
	 * @param packageName
	 * @return
	 */
	public List<String> getClassNames(String packageName){
		List<String> classNameList = new ArrayList<String>();
		String packagePath = packageName.replace(".", "/");  
		ClassLoader loader = Thread.currentThread().getContextClassLoader();  
		URL url = loader.getResource(packagePath);  
		if(url != null){
			File file = new File(url.getPath());  
	        File[] childFiles = file.listFiles();
	        for(File childFile : childFiles){
	        	String className = packageName+"."+childFile.getName().replace(".class", "");
	        	classNameList.add(className);
	        }
		}
		return classNameList;
	}
	
	/**
	 * 初始化handler映射
	 */
	public void initHandlerMapping(){
		for(String str:iocContainer.keySet()){
			Class clazz = iocContainer.get(str).getClass();
			Method[] methods = clazz.getMethods();
		       for (Method method : methods) {
		    	 //判断方式是否添加MyRequestMapping注解
		    	 if(method.isAnnotationPresent(MyRequestMapping.class)){
		    		 //获取Method中MyRequestMapping注解的value
		    		 MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
		             String value = annotation.value().substring(1);
		             //method存入methodMapping
		             handlerMapping.put(value, method);
		    	 }
		     }
		}
	}
	
	/**
	 * 加载自定义视图解析器
	 * @param config
	 */
	public void loadViewResolver(ServletConfig config){
		SAXReader reader = new SAXReader();
		try {
			//解析springmvc.xml
			String path = config.getServletContext().getRealPath("")+"\\WEB-INF\\classes\\"+config.getInitParameter("contextConfigLocation");	
			Document document = reader.read(path);
			Element root = document.getRootElement();
			Iterator iter = root.elementIterator();
			while(iter.hasNext()){
				Element ele = (Element) iter.next();
				if(ele.getName().equals("bean")){
					String className = ele.attributeValue("class");
					Class clazz = Class.forName(className);
					Object obj = clazz.newInstance();
					//获取setter方法
					Method prefixMethod = clazz.getMethod("setPrefix", String.class);
					Method suffixMethod = clazz.getMethod("setSuffix", String.class);
					Iterator beanIter = ele.elementIterator();
					//获取property值
					Map<String,String> propertyMap = new HashMap<String,String>();
					while(beanIter.hasNext()){
						Element beanEle = (Element) beanIter.next();
						String name = beanEle.attributeValue("name");
						String value = beanEle.attributeValue("value");
						propertyMap.put(name, value);
					}
					for(String str:propertyMap.keySet()){
						//反射机制调用setter方法，完成赋值。
						if(str.equals("prefix")){
							prefixMethod.invoke(obj, propertyMap.get(str));
						}
						if(str.equals("suffix")){
							suffixMethod.invoke(obj, propertyMap.get(str));
						}
					}
					myViewResolver = (MyViewResolver) obj;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		//获取请求
		String handlerUri = req.getRequestURI().split("/")[2];
		//获取Controller实例
		Object obj = iocContainer.get(handlerUri);
		String methodUri = req.getRequestURI().split("/")[3];
		//获取业务方法
		Method method = handlerMapping.get(methodUri);
		try {
			//反射机制调用业务方法
			String value = (String) method.invoke(obj);
			//视图解析器将逻辑视图转换为物理视图
			String result = myViewResolver.jspMapping(value);
			//页面跳转
			req.getRequestDispatcher(result).forward(req, resp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
package hu.vidyavana.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.google.gson.GsonBuilder;

public class RequestInfo
{
	private static ThreadLocal<RequestInfo> instance = new ThreadLocal<RequestInfo>();
	
	public HttpServletRequest req;
	public HttpServletResponse resp;
	public HttpSession ses;
	public String uri;
	public String[] args;
	public boolean ajax;
	public Object ajaxResult;
	public String ajaxText;

	
	public static RequestInfo create()
	{
		RequestInfo o = new RequestInfo();
		instance.set(o);
		return o;
	}

	
	public static RequestInfo get()
	{
		 RequestInfo o = instance.get();
		 if(o == null)
			 throw new IllegalStateException("Thread local not created");
		 return o;
	}

	
	public static void reset()
	{
		 instance.remove();
	}
	
	
	public void check()
	{
		// that the object exists
	}
	
	
	public void ajaxText()
	{
		ajaxText = new GsonBuilder().disableHtmlEscaping().create().toJson(ajaxResult);
	}
	
	
	public void renderJsp(String path) throws ServletException, IOException
	{
		// dispatcher may run on another thread
		req.setAttribute("_ri", this);
		req.getRequestDispatcher(path).forward(req, resp);
	}
}

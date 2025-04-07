package com.tomatosystem.exbuilder.web;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import com.cleopatra.spring.UIView;
import com.cleopatra.ui.PageGenerator;
import com.cleopatra.ui.RuntimeCSSFilter;
import com.cleopatra.ui.RuntimeLibFilter;

@Controller
public class CleopatraUIController {

	@PostConstruct
	private void initPageGenerator() {
		PageGenerator instance = PageGenerator.getInstance();
		instance.setURLSuffix(".clx");
		
		instance.setRuntimeLibFilter(new RuntimeLibFilter() {
			@Override
			public List<String> filterRuntimeLib(ServletContext context, HttpServletRequest request, List<String> runtimeLib, List<String> categorizedLib) {
				return runtimeLib;
			}
		});
		
		instance.setRuntimeCSSFilter(new RuntimeCSSFilter() {
			@Override
			public List<String> filterRuntimeCSS(ServletContext context, HttpServletRequest request,
					List<String> runtimeCSS) {
				return runtimeCSS;
			}
		});
	}

	@RequestMapping("/**/*.clx")
	public View index(HttpServletRequest request, HttpServletResponse response) throws IOException {
		return new UIView();
	}

}

package com.tomatosystem.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.mobile.device.Device;
import org.springframework.mobile.device.DeviceUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.View;

import com.cleopatra.XBConfig;
import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.spring.UIView;
import com.cleopatra.ui.PageConfig;



@Controller
public class IndexController {
	@RequestMapping("/index.do")
	public View index(HttpServletRequest request, HttpServletResponse response, 
			DataRequest reqData) throws Exception {
		
	     return new UIView("/ui/design/index.clx");
	
	}
//	public View index(HttpServletRequest request, HttpServletResponse response, 
//			DataRequest reqData) throws Exception {
//		
//		List<String> pathList = XBConfig.getInstance().getDeployPath(); //eXbuilder6 deploy path
//		
//		String deployPath = pathList.get(0);
//		
//		String mainPageUrl = deployPath+"/";   //메인 페이지 URL
//		
//	
//		UIView uiView = new UIView(mainPageUrl);
//		
//		Device device = DeviceUtils.getCurrentDevice(request); 
//		PageConfig config = uiView.getPageConfig();
//		
//		if(device.isMobile()) {
//			//config.setMetaTag("viewport", "width=600");
//			//<meta name="viewport" content="width=device-width,initial-scale=1.0">
//		}
//		config.setTitle("eXCFrame-eXBuilder6 기능 예제");
//		config.setMetaTag("description", "eXBuilder6, 엑스빌더6, 각종 기능 예제, 유형별 화면 템플릿, 공통모듈 예제 데모를 확인 할 수 있습니다.");
//		config.setMetaTag("og:title", "eXCFrame");
//		config.setMetaTag("og:url", "http://edu.tomatosystem.co.kr");
//		config.setMetaTag("og:image", "http://edu.tomatosystem.co.kr/theme/images/com/exb6-logo-og-image.png");
//		config.setMetaTag("og:description", "eXBuilder6 각종 기능 예제, 유형별 화면 템플릿, 공통모듈 예제 데모를 확인 할 수 있습니다.");
//		return uiView; 
//	}
	
//	@RequestMapping("/index.do")
//	public ModelAndView index(HttpServletRequest request, HttpServletResponse response, 
//			DataRequest reqData) throws Exception {
//
//		ModelAndView mv = new ModelAndView();
//		mv.setViewName("redirect:/index_exb6.jsp");
//		mv.setView(new UIView(mainPageUrl));
//		return mv; 
//		
//	}
}

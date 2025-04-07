package com.tomatosystem.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;

import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.spring.JSONDataView;
import com.tomatosystem.service.FigmaToHtmlService;



@Controller
@RequestMapping("/design")
public class DesignController {

    @Autowired
    private FigmaToHtmlService figmaToHtmlService;

//    @GetMapping("/convert")
//    public ResponseEntity<InputStreamResource> downloadHtml() throws Exception {
//
//       String url = "https://api.figma.com/v1/files/OQlnNJhTFmJhozlEVQshyl";
//       String token = "사용자 토큰";
//        Map<String, Object> rawData = fetchFigmaData(url, token);
//        if (rawData == null) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//
//        // HTML 변환 및 저장
//        String htmlContent = figmaToHtmlService.convertToHtml(rawData);
//        File htmlFile = figmaToHtmlService.saveHtmlToFile(htmlContent);
//
//        FileInputStream fileInputStream = new FileInputStream(htmlFile);
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + htmlFile.getName() + "\"")
//                .contentType(MediaType.TEXT_HTML)
//                .body(new InputStreamResource(fileInputStream));
//    }
//
//    private Map<String, Object> fetchFigmaData(String url, String token) throws Exception {
//        HttpGet getRequest = new HttpGet(url);
//        getRequest.addHeader("X-Figma-Token", token);
//
//        try (CloseableHttpClient client = HttpClients.createDefault();
//             CloseableHttpResponse response = client.execute(getRequest)) {
//
//            if (response.getStatusLine().getStatusCode() == 200) {
//                String body = EntityUtils.toString(response.getEntity());
//                ObjectMapper objectMapper = new ObjectMapper();
//                Map<String, Object> data = objectMapper.readValue(body, Map.class);
//
//                System.out.println("🔹 Figma API 응답 데이터: " + data); // 디버깅 출력
//
//                return data;
//            }
//        }
//        return null;
//    }
    
    
    @GetMapping("/convert.do")
    public ResponseEntity<String> convertAndSaveClx() {
        String url = "https://api.figma.com/v1/files/OQlnNJhTFmJhozlEVQshyl";
        String token = "사용자 토큰";

        try {
            Map<String, Object> rawData = fetchFigmaData(url, token);
            if (rawData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Figma data.");
            }

            File clxFile = figmaToHtmlService.convertToClx(rawData);
            if (clxFile == null || !clxFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CLX file creation failed.");
            }

            return ResponseEntity.ok("CLX file saved successfully at: " + clxFile.getAbsolutePath());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing the request.");
        }
    }
    
    private Map<String, Object> fetchFigmaData(String url, String token) {
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("X-Figma-Token", token);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(getRequest)) {

            if (response.getStatusLine().getStatusCode() == 200) {
                String body = EntityUtils.toString(response.getEntity());
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readValue(body, Map.class);
            } else {
                // 응답 코드가 200이 아니면 오류 처리
                throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
            }
        } catch (Exception e) {
            // 예외 발생 시 처리
            throw new RuntimeException("Figma 데이터 가져오기 실패", e);
        }
    }


    
//    //2번쨰 
//    @RequestMapping("/convert.do")
//    public ResponseEntity<InputStreamResource> downloadClx() throws Exception {
//
//  String url = "https://api.figma.com/v1/files/OQlnNJhTFmJhozlEVQshyl";
//  String token = "사용자 토큰";
    
//        // 🔹 Figma 데이터 가져오기
//        Map<String, Object> rawData = fetchFigmaData(url, token);
//        if (rawData == null) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//
//        // 🔹 .clx 변환 및 저장
//        File clxFile = figmaToHtmlService.convertToClx(rawData);
//
//        // 🔹 HTTP 응답으로 .clx 파일 다운로드
//        try (FileInputStream fileInputStream = new FileInputStream(clxFile)) {
//            InputStreamResource resource = new InputStreamResource(fileInputStream);
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + clxFile.getName() + "\"")
//                    .contentType(MediaType.APPLICATION_OCTET_STREAM)  // 바이너리 파일이므로 'application/octet-stream' 사용
//                    .body(resource);  // InputStreamResource를 body로 직접 설정
//        } catch (IOException e) {
//            // 파일 읽기 오류 처리
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }
//
//    private Map<String, Object> fetchFigmaData(String url, String token) throws Exception {
//        HttpGet getRequest = new HttpGet(url);
//        getRequest.addHeader("X-Figma-Token", token);
//
//        try (CloseableHttpClient client = HttpClients.createDefault();
//             CloseableHttpResponse response = client.execute(getRequest)) {
//
//            if (response.getStatusLine().getStatusCode() == 200) {
//                String body = EntityUtils.toString(response.getEntity());
//                ObjectMapper objectMapper = new ObjectMapper();
//                return objectMapper.readValue(body, Map.class);
//            } else {
//                // Figma API 오류 처리
//                throw new Exception("Failed to fetch data from Figma API. Status code: " +
//                                     response.getStatusLine().getStatusCode());
//            }
//        } catch (IOException e) {
//            // 예외 처리 및 로깅
//            throw new Exception("Error while fetching Figma data", e);
//        }
//    }
    
	@RequestMapping("/test.do")
	public View saveDtl3(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) throws Exception {
		BigDecimal bd = new BigDecimal("9999999999999999");
		//System.out.println(new DecimalFormat("#.##").format(bd));

		
		//데이터셋에 데이터를 담을 list를 생성합니다. 
		List<Map<String, Object>> list = new ArrayList<>();
		
		// 반복문을 통해 데이터를 추가합니다. 
		// list에 들어갈 map을 만들어서 선행데이터를 추가했습니다. 
		for(int i=0; i<5; i++) {
			Map<String, Object> map = new HashMap<>();
			
			// 선행 데이터
	        String value = "test"+i;
	        String value2 = "test2"+i;
	        
			map.put("column1", value);
			map.put("column2", value2);
			map.put("column3", bd);
            

			list.add(i,map);
		}

		dataRequest.setResponse("ds1", list);
		return new JSONDataView();
	}
}
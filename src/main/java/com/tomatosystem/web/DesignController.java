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
    
    
//    @GetMapping("/convert.do")
//    public ResponseEntity<String> convertAndSaveClx() {
//        String url = "https://api.figma.com/v1/files/3PRYK752FpfAXu5Ypp9QWL";
//        //사용자 토큰
//        String token = "사용자 토큰";
//
//        try {
//            Map<String, Object> rawData = fetchFigmaData(url, token);
//            if (rawData == null) {
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Figma data.");
//            }
//
//            File clxFile = figmaToHtmlService.convertToClx(rawData);
//            if (clxFile == null || !clxFile.exists()) {
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CLX file creation failed.");
//            }
//
//            return ResponseEntity.ok("CLX file saved successfully at: " + clxFile.getAbsolutePath());
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing the request.");
//        }
//    }
//    
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


// 자동 확인
    @GetMapping("/convert.do")
    public ResponseEntity<String> convertAndSaveClx() {
        String teamId = "1420657369280493518"; // 팀 ID
        String token = "사용자 토큰";//사용자 토큰

        try {
            // 1. 팀 → 프로젝트 목록 요청
            String projectId = fetchFirstProjectIdFromTeam(teamId, token);
            if (projectId == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No project found in team.");
            }

            // 2. 프로젝트 → 파일 목록 요청
            String fileKey = fetchFirstFileKeyFromProject(projectId, token);
            if (fileKey == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No files found in project.");
            }
            System.out.println("projectId ==" + projectId);
            System.out.println("fileKey ==" + fileKey );

            // 3. 파일 → 실제 데이터 요청
            String fileUrl = "https://api.figma.com/v1/files/" + fileKey;
            Map<String, Object> rawData = fetchFigmaData(fileUrl, token);

            if (rawData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Figma data.");
            }

            File clxFile = figmaToHtmlService.convertToClx(rawData);
            if (clxFile == null || !clxFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CLX file creation failed.");
            }

            return ResponseEntity.ok("CLX file saved successfully at: " + clxFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing the request.");
        }
    }
    
    private String fetchFirstProjectIdFromTeam(String teamId, String token) {
        String url = "https://api.figma.com/v1/teams/" + teamId + "/projects";
        try {
            String body = sendFigmaGetRequest(url, token);
            Map<String, Object> map = new ObjectMapper().readValue(body, Map.class);
            List<Map<String, Object>> projects = (List<Map<String, Object>>) map.get("projects");
            if (projects != null && !projects.isEmpty()) {
                return String.valueOf(projects.get(0).get("id")); // 첫 번째 프로젝트 ID
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String fetchFirstFileKeyFromProject(String projectId, String token) {
        String url = "https://api.figma.com/v1/projects/" + projectId + "/files";
        try {
            String body = sendFigmaGetRequest(url, token);
            Map<String, Object> map = new ObjectMapper().readValue(body, Map.class);
            List<Map<String, Object>> files = (List<Map<String, Object>>) map.get("files");
            if (files != null && !files.isEmpty()) {
                return String.valueOf(files.get(0).get("key")); // 첫 번째 파일 key
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private String sendFigmaGetRequest(String url, String token) throws IOException {
        HttpGet request = new HttpGet(url);
        request.addHeader("X-Figma-Token", token);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {

            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            } else {
                throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
            }
        }
    }
    
    
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
package com.tomatosystem.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.cleopatra.protocol.data.ParameterGroup;
import com.cleopatra.protocol.data.UploadFile;
import com.cleopatra.spring.JSONDataView;
import com.tomatosystem.service.FigmaToClxService;
import com.tomatosystem.service.FigmaToHtmlService;



@Controller
@RequestMapping("/design")
public class DesignController {

    @Autowired
    private FigmaToHtmlService figmaToHtmlService;

    @Autowired
    private FigmaToClxService figmaToClxService;
    // html 전환 테스트
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
    
    // 기존 파일 및 토큰 직접 수동입력방식
//    @GetMapping("/convertDirect.do")
//    public ResponseEntity<String> convertAndSaveClxDirect() {
//    	//kSWLpqp877HduvBHA9EZnp ,x5gR79q0HUZ567W3CjCuCJ(원그리드)
//    	//DFNwtibCb59d2U1ayicShM(로그인)
//        String url = "https://api.figma.com/v1/files/kSWLpqp877HduvBHA9EZnp";
//        //사용자 토큰
//        String token = "사용자 토큰";
//
//        try {
//            Map<String, Object> rawData = fetchFigmaDataDirect(url, token);
//            if (rawData == null) {
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Figma data.");
//            }
//            //JSON 작성
//            saveJsonToFile(rawData);
//
//            //기본 동작
//            //File clxFile = figmaToHtmlService.convertToClx(rawData);
//            
//            //인스턴스 타입 Class 테스트
//            File clxFile = figmaToClxService.convertToClx(rawData);
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
    @GetMapping("/convertDirect.do")
    public ResponseEntity<String> convertAndSaveClxDirect() {
        String token = "사용자 토큰";
        //lR8UApS86cMUi3ExqkbPpK,x5gR79q0HUZ567W3CjCuCJ
        String fileKey = "x5gR79q0HUZ567W3CjCuCJ";
        String url = "https://api.figma.com/v1/files/" + fileKey;

        try {
            Map<String, Object> rawData = fetchFigmaDataDirect(url, token);
            if (rawData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Figma data.");
            }
            //JSON 파일 저장
            saveJsonToFile(rawData);

            // token, fileKey 함께 전달
            //File clxFile = figmaToClxService.convertToClx(rawData);
            //이미지 전달
            File clxFile = figmaToClxService.convertToClxImg(rawData, token, fileKey);
            
            if (clxFile == null || !clxFile.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CLX file creation failed.");
            }

            return ResponseEntity.ok("CLX file saved successfully at: " + clxFile.getAbsolutePath());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing the request.");
        }
    }
    
    private Map<String, Object> fetchFigmaDataDirect(String url, String token) {
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
//    private String sendFigmaGetRequest(String url, String token) throws IOException {
//        HttpGet request = new HttpGet(url);
//        request.addHeader("X-Figma-Token", token);
//
//        try (CloseableHttpClient client = HttpClients.createDefault();
//             CloseableHttpResponse response = client.execute(request)) {
//
//            if (response.getStatusLine().getStatusCode() == 200) {
//                return EntityUtils.toString(response.getEntity());
//            } else {
//                throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
//            }
//        }
//    }
    
    private Map<String, Object> fetchFigmaData(String url, String token) {
        HttpGet getRequest = new HttpGet(url);
        getRequest.addHeader("Authorization", "Bearer " + token); // ✅ 변경된 부분

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


    // 자동 토큰 및 프로젝트
    @RequestMapping("/convert.do")
    public ResponseEntity<String> convertAndSaveClx(DataRequest dataRequest) {
    	
    	//Figma의 공식 REST API에서는 다음과 같은 이유로 토큰만으로 팀 ID를 가져오는 기능을 제공하지 않음
    	//GET /v1/teams 같은 유저의 팀 리스트를 가져오는 API가 존재하지 않음
    	//토큰은 리소스에 대한 접근 권한만 제공할 뿐, 유저 소속 팀 전체 목록은 제공하지 않음
        //보안 및 개인정보 보호 차원에서 제한
        String teamId = "1420657369280493518"; // 팀 ID
        
        ParameterGroup dm = dataRequest.getParameterGroup("dmParam");
        String token = dm.getValue("token");
        

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
            //JSON 작성
            saveJsonToFile(rawData);

            //File clxFile = figmaToHtmlService.convertToClx(rawData);
            
            //이미지 활용(변환)
            File clxFile = figmaToClxService.convertToClxImg(rawData, token, fileKey);
            
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
        request.addHeader("Authorization", "Bearer " + token); // ✅ 변경된 부분

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {

            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            } else {
                throw new RuntimeException("Figma API 호출 실패: " + response.getStatusLine());
            }
        }
    }
    
    
    @RequestMapping("/convertAll.do")
    public ResponseEntity<String> convertAndSaveClxAll(DataRequest dataRequest) {
    	//   Figma의 공식 REST API에서는 다음과 같은 이유로 토큰만으로 팀 ID를 가져오는 기능을 제공하지 않음
    	//    GET /v1/teams 같은 유저의 팀 리스트를 가져오는 API가 존재하지 않음
    	//    토큰은 리소스에 대한 접근 권한만 제공할 뿐, 유저 소속 팀 전체 목록은 제공하지 않음
    	//    보안 및 개인정보 보호 차원에서 제한


        String teamId = "1420657369280493518"; // 팀 ID

        ParameterGroup dm = dataRequest.getParameterGroup("dmParam");
        String token = dm.getValue("token");

        StringBuilder resultLog = new StringBuilder();

        try {
            // ✅ [수정] 전체 프로젝트 목록 가져오기
            List<Map<String, Object>> projects = fetchAllProjectsFromTeam(teamId, token);
            if (projects == null || projects.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No projects found in team.");
            }

            for (Map<String, Object> project : projects) {
                String projectId = String.valueOf(project.get("id"));
                resultLog.append("Project ID: ").append(projectId).append("\n");

                // ✅ [수정] 해당 프로젝트의 모든 파일 가져오기
                List<Map<String, Object>> files = fetchAllFilesFromProject(projectId, token);
                if (files == null || files.isEmpty()) {
                    resultLog.append("  No files found in project.\n");
                    continue;
                }

                for (Map<String, Object> file : files) {
                    String fileKey = String.valueOf(file.get("key"));
                    String fileName = String.valueOf(file.get("name"));
                    String fileUrl = "https://api.figma.com/v1/files/" + fileKey;

                    resultLog.append("  File: ").append(fileName).append(" (").append(fileKey).append(")\n");

                    try {
                        Map<String, Object> rawData = fetchFigmaData(fileUrl, token);
                        if (rawData == null) {
                            resultLog.append("    ❌ Failed to fetch data\n");
                            continue;
                        }
                        //JSON 작성
                        saveJsonToFile(rawData);
                        

                        //File clxFile = figmaToHtmlService.convertToClx(rawData);
                        
                        //이미지 활용(변환)
                        File clxFile = figmaToClxService.convertToClxImg(rawData, token, fileKey);
                        
                        if (clxFile == null || !clxFile.exists()) {
                            resultLog.append("    ❌ CLX file creation failed.\n");
                            continue;
                        }

                        resultLog.append("    ✅ Saved: ").append(clxFile.getAbsolutePath()).append("\n");

                    } catch (Exception e) {
                        resultLog.append("    ❌ Exception while processing file: ").append(e.getMessage()).append("\n");
                        e.printStackTrace();
                    }
                }
            }

            return ResponseEntity.ok(resultLog.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing request.");
        }
    }    
 // ✅ [수정] 팀의 전체 프로젝트 가져오기
    private List<Map<String, Object>> fetchAllProjectsFromTeam(String teamId, String token) {
        String url = "https://api.figma.com/v1/teams/" + teamId + "/projects";
        try {
            String body = sendFigmaGetRequest(url, token);
            Map<String, Object> map = new ObjectMapper().readValue(body, Map.class);
            return (List<Map<String, Object>>) map.get("projects");
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    // ✅ [수정] 프로젝트의 전체 파일 가져오기
    private List<Map<String, Object>> fetchAllFilesFromProject(String projectId, String token) {
        String url = "https://api.figma.com/v1/projects/" + projectId + "/files";
        try {
            String body = sendFigmaGetRequest(url, token);
            Map<String, Object> map = new ObjectMapper().readValue(body, Map.class);
            return (List<Map<String, Object>>) map.get("files");
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
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
	public File saveJsonToFile(Map<String, Object> jsonData) throws IOException {
	    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
	    String outputDir = "C:\\Users\\LCM\\git\\Converter-Figma\\clx-src\\json\\" + today;

	    // 디렉토리 생성
	    Files.createDirectories(Paths.get(outputDir));

	    // 랜덤 문자열 생성 (숫자 + 문자 조합)
	    String randomStr = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	    String fileName = today + "_" + randomStr + ".json";
	    Path filePath = Paths.get(outputDir, fileName);

	    // ObjectMapper를 사용해 JSON 파일로 저장
	    ObjectMapper objectMapper = new ObjectMapper();
	    objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), jsonData);

	    return filePath.toFile();
	}
	
    @RequestMapping("/jsonConvert.do")
    public ResponseEntity<String> jsonConvert(HttpServletRequest request, HttpServletResponse response, DataRequest dataRequest) {


        try {


            // 업로드된 파일 처리 (JSON 파일)
            Map<String, UploadFile[]> uploadFiles = dataRequest.getUploadFiles();
            System.out.println("업로드 파일 == " + uploadFiles);
            boolean hasJsonFile = false;

            if (uploadFiles != null && !uploadFiles.isEmpty()) {
                for (UploadFile[] uFiles : uploadFiles.values()) {
                    for (UploadFile uFile : uFiles) {
                        File file = uFile.getFile();
                        System.out.println("파일 객체: " + file);
                        if (file != null) {
                            String uploadedFileName  = file.getName();
                            System.out.println("파일 이름: " + uploadedFileName );
                            if (uploadedFileName .toLowerCase().contains(".json")) {
                                hasJsonFile = true;

                                Map<String, Object> uploadedJsonData = readJsonFromFile(file);
                                if (uploadedJsonData == null) {
                                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("업로드된 JSON 파일 읽기 실패");
                                }
                                // JSON 파일 저장 (기존 로직과 동일)
                                saveJsonToFile(uploadedJsonData);
                                
                                // CLX 변환 (토큰과 fileKey 없이 진행)
                                File clxFile = figmaToClxService.convertToClx(uploadedJsonData);
                                
                                if (clxFile == null || !clxFile.exists()) {
                                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("CLX file creation failed.");
                                }
                                System.out.println("업로드 json == " + uploadedJsonData);
                                return ResponseEntity.ok("CLX file saved successfully at: " + clxFile.getAbsolutePath());
                            

                            }
                        }
                    }
                }
            }

            // JSON 파일이 아예 없었을 경우
            if (!hasJsonFile) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("JSON 형식의 업로드 파일이 없습니다.");
            }

            return ResponseEntity.ok("Figma 데이터 분석 완료."); // 이건 사실상 도달하지 않음

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("요청 처리 중 오류 발생: " + e.getMessage());
        }
    }
    
    private Map<String, Object> readJsonFromFile(File file) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(file, Map.class); // JSON을 Map 형식으로 변환
        } catch (IOException e) {
            return null;
        }
    }
}
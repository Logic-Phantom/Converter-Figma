package com.tomatosystem.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FigmaApiService {
	  private static final String FIGMA_API_URL = "https://api.figma.com/v1/files/";

	    private String accessToken;

//	    public FigmaApiService(String accessToken) {
//	        this.accessToken = accessToken;
//	    }

	    // Figma 파일의 메타데이터 및 lastModified 시간을 가져오는 메서드
	    public JsonNode getFileMetadata(String fileId) throws IOException {
	    	accessToken = "사용자 토큰";
	        URL url = new URL(FIGMA_API_URL + fileId);
	        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.setRequestMethod("GET");
	        connection.setRequestProperty("X-Figma-Token", accessToken);

	        ObjectMapper objectMapper = new ObjectMapper();
	        return objectMapper.readTree(connection.getInputStream());
	    }
//	    public long getLastModifiedTime(String fileId) throws IOException {
//	        JsonNode metadata = getFileMetadata(fileId);
//	        
//	        // 'lastModified'가 meta 노드에 있을 경우
//	        JsonNode metaNode = metadata.path("meta");
//	        if (metaNode.has("lastModified")) {
//	            return metaNode.path("lastModified").asLong();
//	        } else {
//	            System.out.println("lastModified 값이 없습니다. 파일 ID: " + fileId);
//	            return 0;  // 또는 적절한 기본값을 설정
//	        }
//	    }
	    // Figma 파일의 마지막 수정 시간(lastModified) 가져오기
	    public long getLastModifiedTime(String fileId) throws IOException {
	        JsonNode metadata = getFileMetadata(fileId);

	        // API 응답 데이터 로그로 확인
	        System.out.println("Figma metadata response: " + metadata.toString());

	        // 'lastModified' 값이 metadata에 있는지 확인
	        JsonNode lastModifiedNode = metadata.get("lastModified");

	        if (lastModifiedNode != null) {
	            String lastModifiedStr = lastModifiedNode.asText(); // 문자열로 변환

	            // 날짜 형식에 맞는 SimpleDateFormat을 사용하여 문자열을 Date로 변환
	            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	            try {
	                Date date = sdf.parse(lastModifiedStr);
	                return date.getTime(); // 밀리초 단위로 반환
	            } catch (ParseException e) {
	                System.out.println("날짜 파싱 오류: " + e.getMessage());
	                return 0;  // 파싱 실패 시 0 반환
	            }
	        } else {
	            System.out.println("lastModified 값이 없습니다. 파일 ID: " + fileId);
	            return 0;  // 적절한 기본값을 설정
	        }
	    }
	}


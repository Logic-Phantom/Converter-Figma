package com.tomatosystem.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

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

	    // Figma 파일의 마지막 수정 시간(lastModified) 가져오기
	    public long getLastModifiedTime(String fileId) throws IOException {
	        JsonNode metadata = getFileMetadata(fileId);
	        JsonNode documentNode = metadata.path("document");
	        // lastModified 정보는 API에서 문서의 메타데이터에 포함되어 있어야 합니다.
	        return documentNode.path("lastModified").asLong(); // 예시로 lastModified를 long으로 반환
	    }
	}


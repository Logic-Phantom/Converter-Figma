package com.tomatosystem.web;

import com.tomatosystem.service.AdvancedFigmaToClxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cleopatra.protocol.data.DataRequest;
import com.cleopatra.protocol.data.ParameterGroup;

@RestController
@RequestMapping("/design")
public class AdvancedDesignController {
    @Autowired
    private AdvancedFigmaToClxService advancedFigmaToClxService;

    @RequestMapping("/convertAdvanced.do")
    public ResponseEntity<String> convertAdvancedClx(DataRequest dataRequest) {
        ParameterGroup dm = dataRequest.getParameterGroup("dmParam");
        String token = dm.getValue("token");
        String fileKey = dm.getValue("fileKey");
        try {
            String resultPath = advancedFigmaToClxService.convertFigmaJsonToClx(token, fileKey);
            return ResponseEntity.ok("Advanced CLX file saved at: " + resultPath);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
} 
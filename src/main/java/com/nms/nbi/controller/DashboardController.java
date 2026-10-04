package com.nms.nbi.controller;

import com.nms.nbi.client.NmsGatewayClientService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DashboardController {

    private final NmsGatewayClientService nmsGatewayClientService;

    public DashboardController(
            NmsGatewayClientService nmsGatewayClientService
    ) {
        this.nmsGatewayClientService = nmsGatewayClientService;
    }

    @GetMapping(
            value = "/dashboard/api/devices",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<String> getDashboardDevices() {

        try {
            return ResponseEntity.ok(
                    nmsGatewayClientService.getDevices()
            );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "{\"error\":\"Unable to retrieve NMS device data\"}"
                    );
        }
    }
}

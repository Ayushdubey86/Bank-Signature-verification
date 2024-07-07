package com.mercedes.pris.diagnosticorder.controller;

import com.mercedes.pris.diagnosticorder.exception.DiagnosticOrderValidationException;
import com.mercedes.pris.diagnosticorder.model.ActionRequestDTO;
import com.mercedes.pris.diagnosticorder.model.DiagnosticOrderDTO;
import com.mercedes.pris.diagnosticorder.model.DiagnosticOrderResponseDTO;
import com.mercedes.pris.diagnosticorder.persistence.DiagnosticOrderEntity;
import com.mercedes.pris.diagnosticorder.service.DiagnosticOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.ResponseEntity.status;


@RestController
@RequestMapping("/pris/diagnosticOrder")
public class DiagnosticRestController {
    Logger logger = LoggerFactory.getLogger(DiagnosticRestController.class);
    private final DiagnosticOrderService diagnosticOrderService;

    @Autowired
    public DiagnosticRestController(DiagnosticOrderService diagnosticOrderService) {
        this.diagnosticOrderService = diagnosticOrderService;
    }

    @Operation(summary = "Create diagnostic order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Create the Diagnostic Order",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Bad Request input",
                    content = @Content)})
    @PostMapping(path = "/create",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> createDiagnosticOrder(@Valid @RequestBody DiagnosticOrderDTO diagnosticOrderDTO) {
        logger.debug("Incoming request for diagnostic order creation: {}", diagnosticOrderDTO);
        DiagnosticOrderEntity diagnosticOrder;
        try {

            diagnosticOrder = diagnosticOrderService.saveDiagnosticOrder(diagnosticOrderDTO);

        } catch (Exception exception) {
            logger.error("Exception occurred while creating diagnostic order", exception);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return status(HttpStatus.CREATED)
                .body(diagnosticOrder);

    }

    @Operation(summary = "Edit diagnostic order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Edit the Diagnostic Order",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Bad Request input",
                    content = @Content)})
    @PutMapping(path = "/edit/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> editDiagnosticOrder(@Valid @RequestBody DiagnosticOrderDTO diagnosticOrderDTO, @PathVariable("id") Long id) {
        logger.debug("Incoming request for diagnostic order editing: {}", diagnosticOrderDTO);
        DiagnosticOrderEntity editDiagnosticOrder;
        try {
            diagnosticOrderDTO.setId(id);
            editDiagnosticOrder = diagnosticOrderService.editDiagnosticOrder(diagnosticOrderDTO);
        } catch (DiagnosticOrderValidationException ex) {
            throw ex;
        } catch (Exception exception) {
            logger.error("Exception occurred while editing diagnostic order", exception);
            return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return status(HttpStatus.CREATED).body(editDiagnosticOrder);

    }


    @Operation(summary = "Get all diagnostic order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the list of Diagnostic Orders",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "204", description = "Diagnostic Order list is empty",
                    content = @Content)})
    @GetMapping("/getAll")
    public ResponseEntity<List<DiagnosticOrderResponseDTO>> getAllDiagnosticOrder() {
        List<DiagnosticOrderResponseDTO> diagnosticOrder = diagnosticOrderService.getAllDiagnosticOrders();
        return ResponseEntity.ok(diagnosticOrder);
    }

    @Operation(summary = "Get diagnostic order by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the Diagnostic Orders by Id",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Diagnostic Order is Not Found",
                    content = @Content)})
    @GetMapping("/{id}")
    public ResponseEntity<DiagnosticOrderResponseDTO> getDiagnosticOrderById(@PathVariable Long id) {
        DiagnosticOrderResponseDTO diagnosticOrder = diagnosticOrderService.getDiagnosticOrderById(id);
        return new ResponseEntity<>(diagnosticOrder, HttpStatus.OK);
    }


    @Operation(summary = "Update Status of diagnostic order by Id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully Updated the Status",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "404", description = "Diagnostic Order is Not Found",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = " Invalid Request ",
                    content = @Content)})
    @PutMapping("/{id}")
    public ResponseEntity<DiagnosticOrderEntity> performAction(
            @PathVariable Long id,
            @RequestBody ActionRequestDTO actionRequest) {

        DiagnosticOrderEntity updatedOrder;

        switch (actionRequest.getAction().toLowerCase()) {
            case "approve":
                updatedOrder = diagnosticOrderService.updateDiagnosticOrder(id, actionRequest.getUser(), actionRequest.getComment(), true);
                break;
            case "decline":
                updatedOrder = diagnosticOrderService.updateDiagnosticOrder(id, actionRequest.getUser(), actionRequest.getComment(), false);
                break;
            default:
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
    }

    @Operation(summary = "Search Diagnostic Order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search Orders",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "404", description = "No Orders Found",
                    content = @Content)})
    @GetMapping("/search/{search}")
    public ResponseEntity<List<DiagnosticOrderResponseDTO>> searchDiagnosticOrder(@PathVariable String search) {
        return ResponseEntity.ok(diagnosticOrderService.searchDiagnosticOrder(search));
    }

    @Operation(summary = "Filter Diagnostic Orders")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter Diagnostic Order by Parameters",
                    content = {@Content(mediaType = "application/json",
                            schema = @Schema(implementation = DiagnosticOrderDTO.class))}),
            @ApiResponse(responseCode = "404", description = "NO Order Found",
                    content = @Content)})
    @GetMapping("/filter")
    public ResponseEntity<List<DiagnosticOrderResponseDTO>> filterGalaRules(@RequestParam(required = false) List<Long> id, @RequestParam(required = false) List<String> requesterName,
                                                                       @RequestParam(required = false) List<String> uploadingPoint, @RequestParam(required = false) List<String> finVin,
                                                                       @RequestParam(required = false) List<String> partNumber, @RequestParam(required = false) List<String> damageCode,
                                                                       @RequestParam(required = false) List<String> status, @RequestParam(required = false) List<String> createdBy,
                                                                       @RequestParam(required = false) List<String> createdDate) {
        return ResponseEntity.ok(diagnosticOrderService.filterDiagnosticOrder(id, requesterName, uploadingPoint, finVin, partNumber, damageCode, status, createdBy, createdDate));
    }

}




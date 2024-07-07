package com.mercedes.pris.diagnosticorder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mercedes.pris.auditlogs.exception.AuditLogException;
import com.mercedes.pris.auditlogs.model.AuditLogCreate;
import com.mercedes.pris.auditlogs.persistence.AuditLogEntity;
import com.mercedes.pris.auditlogs.persistence.AuditLogRepository;
import com.mercedes.pris.auditlogs.service.AuditLogService;
import com.mercedes.pris.common.persistence.PartNumbersEntity;
import com.mercedes.pris.common.persistence.PartNumbersRepository;
import com.mercedes.pris.diagnosticorder.exception.DiagnosticOrderException;
import com.mercedes.pris.diagnosticorder.exception.DiagnosticOrderValidationException;
import com.mercedes.pris.diagnosticorder.model.DiagnosticOrderDTO;
import com.mercedes.pris.diagnosticorder.model.DiagnosticOrderResponseDTO;
import com.mercedes.pris.diagnosticorder.model.DOPartNumberDTO;
import com.mercedes.pris.common.model.PartNumbersResponseDTO;
import com.mercedes.pris.diagnosticorder.persistence.*;
import com.mercedes.pris.diagnosticorder.persistence.specification.DiagnosticOrderSpecification;
import com.mercedes.pris.manualrules.persistence.ManualRulesEntity;
import com.mercedes.pris.utils.AuditActionEnum;
import com.mercedes.pris.utils.AuditSourceTypeEnum;
import com.mercedes.pris.utils.OrderStatus;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.clazz.ValueObjectDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.mercedes.pris.utils.CommonConstants.DIAGNOSTIC_ORDER_MSG;


@Service
public class DiagnosticOrderService {
    private final DiagnosticRepository diagnosticRepository;

    private final DiagnosticOrderAuditLogRepository diagnosticOrderAuditLogRepository;

    private final DiagnosticOrderSpecification diagnosticOrderSpecification;

    private final PartNumbersRepository partNumbersRepository;

    private final AuditLogRepository auditLogRepository;

    private final AuditLogService auditLogService;

    private ObjectMapper om = new ObjectMapper();

    @Autowired
    public DiagnosticOrderService(DiagnosticRepository diagnosticRepository, DiagnosticOrderAuditLogRepository diagnosticOrderAuditLogRepository, DiagnosticOrderSpecification diagnosticOrderSpecification, PartNumbersRepository partNumbersRepository, AuditLogRepository auditLogRepository, AuditLogService auditLogService) {
        this.diagnosticRepository = diagnosticRepository;
        this.diagnosticOrderAuditLogRepository = diagnosticOrderAuditLogRepository;
        this.diagnosticOrderSpecification = diagnosticOrderSpecification;
        this.partNumbersRepository = partNumbersRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditLogService = auditLogService;
    }

    public DiagnosticOrderEntity saveDiagnosticOrder(DiagnosticOrderDTO diagnosticOrderDTO) {
        DiagnosticOrderEntity diagnosticOrderEntity = new DiagnosticOrderEntity();
        try {
            diagnosticOrderEntity.setRequesterName(diagnosticOrderDTO.getRequesterName());
            diagnosticOrderEntity.setUploadingPoint(diagnosticOrderDTO.getUploadingPoint());
            diagnosticOrderEntity.setFinVin(diagnosticOrderDTO.getFinVin());
            diagnosticOrderEntity.setDamageCode(diagnosticOrderDTO.getDamageCode());
            diagnosticOrderEntity.setStatus(diagnosticOrderDTO.getStatus());
            diagnosticOrderEntity.setInternalComment(diagnosticOrderDTO.getInternalComment());
            diagnosticOrderEntity.setExternalComment(diagnosticOrderDTO.getExternalComment());
            diagnosticOrderEntity.setCreatedBy(diagnosticOrderDTO.getCreatedBy());
            diagnosticOrderEntity.setComment(diagnosticOrderDTO.getComment());
            diagnosticOrderEntity.setNotes(diagnosticOrderDTO.getNotes());
            diagnosticOrderEntity.setCreatedDate(Timestamp.from(Instant.now()));

            List<DOPartNumberEntity> partNumbers = new ArrayList<>();
            for (DOPartNumberDTO partsNumberDTO : diagnosticOrderDTO.getPartNumbers()) {
                PartNumbersEntity allPartsNumber = partNumbersRepository.findById(partsNumberDTO.getPartId()).orElse(null);
                if (allPartsNumber != null) {
                    DOPartNumberEntity partsNumber = new DOPartNumberEntity();
                    partsNumber.setParts(allPartsNumber);
                    partsNumber.setOrder(diagnosticOrderEntity);
                    partsNumber.setArrived(partsNumberDTO.getArrived());
                    partsNumber.setUnits(partsNumberDTO.getUnits());
                    partNumbers.add(partsNumber);
                }
            }
            diagnosticOrderEntity.setPartNumbers(partNumbers);
            diagnosticOrderEntity = diagnosticRepository.save(diagnosticOrderEntity);
            List<AuditLogCreate> auditLogCreateList = new ArrayList<>();
            AuditLogCreate auditLogCreate = AuditLogCreate.builder().auditSourceId(diagnosticOrderEntity.getId()).auditSourceType(AuditSourceTypeEnum.DIAGNOSTIC_ORDER.getDesc())
                    .action(AuditActionEnum.CREATE.getDesc()).description(String.format(DIAGNOSTIC_ORDER_MSG, diagnosticOrderEntity.getId()))
                    .createdBy(diagnosticOrderDTO.getCreatedBy()).createdDate(Timestamp.from(Instant.now())).version(1).build();
            auditLogCreateList.add(auditLogCreate);
            auditLogService.recordAuditLogs(auditLogCreateList);
        } catch (AuditLogException exception) {
            throw new AuditLogException(" Exception occurred creating audit log in Diagnostic Order", exception);
        } catch (Exception exception) {
            throw new DiagnosticOrderException("Exception occurred while saving the data in Diagnostic Order", exception);
        }

        return diagnosticOrderEntity;
    }


    public DiagnosticOrderEntity editDiagnosticOrder(DiagnosticOrderDTO diagnosticOrderDTO) {
        DiagnosticOrderEntity diagnosticOrderEntity;
        try {
            List<DiagnosticOrderAuditLogEntity> auditList = new ArrayList<>();
            DiagnosticOrderEntity doEntity =
                    diagnosticRepository.findById(diagnosticOrderDTO.getId())
                            .orElseThrow(() ->
                                    new DiagnosticOrderException("Diagnostic order is not found for id: " + diagnosticOrderDTO.getId()));


            //validation
            if (doEntity.getStatus() != null && doEntity.getStatus().equals("APPROVED")) {
                throw new DiagnosticOrderValidationException("Item has been a approved and is no longer editable");
            }
          // TODO - Part Number and Part Name  records in Audit Table
            om.registerModule(new JavaTimeModule());
            String existingTrainingJson = om.writeValueAsString(doEntity);
            DiagnosticOrderEntity existingDiagnosticOrder = om.readValue(existingTrainingJson, DiagnosticOrderEntity.class);


            List<AuditLogCreate> auditLogCreateList = new ArrayList<>();
            doEntity = addPartNumbers(diagnosticOrderDTO.getPartNumbers(), doEntity);
            doEntity.setRequesterName(diagnosticOrderDTO.getRequesterName());
            doEntity.setUploadingPoint(diagnosticOrderDTO.getUploadingPoint());
            doEntity.setFinVin(diagnosticOrderDTO.getFinVin());
            doEntity.setDamageCode(diagnosticOrderDTO.getDamageCode());
            doEntity.setStatus(diagnosticOrderDTO.getStatus());
            doEntity.setInternalComment(diagnosticOrderDTO.getInternalComment());
            doEntity.setExternalComment(diagnosticOrderDTO.getExternalComment());
            doEntity.setCreatedBy(diagnosticOrderDTO.getCreatedBy());
            doEntity.setComment(diagnosticOrderDTO.getComment());
            doEntity.setNotes(diagnosticOrderDTO.getNotes());

            AuditLogEntity existAuditLogEntity = auditLogRepository.findFirstByAuditSourceIdOrderByCreatedDateDesc(diagnosticOrderDTO.getId());
            buildAuditLog(doEntity, existingDiagnosticOrder, auditLogCreateList, existAuditLogEntity.getVersion());
            auditLogService.recordAuditLogs(auditLogCreateList);
            diagnosticOrderEntity = diagnosticRepository.saveAndFlush(doEntity);

            //This historization need to be removed once
            diagnosticOrderAuditLogRepository.saveAll(auditList);


        } catch (AuditLogException ex) {
            throw new AuditLogException(" Exception occurred creating audit log in edit DiagnosticOrder", ex);
        } catch (DiagnosticOrderValidationException ex) {
            throw ex;
        } catch (Exception exception) {
            throw new DiagnosticOrderException("Exception occurred while editing the DiagnosticOrder", exception);
        }
        return diagnosticOrderEntity;
    }

    private void buildAuditLog(DiagnosticOrderEntity dbNewEntity, DiagnosticOrderEntity dbEntity, List<AuditLogCreate> auditList, Integer version) {

        List<String> ignore = List.of("createdDate", "modifiedDate", "status", "createdBy"); //Move this to enum as list of ignore properties

        Javers javers = JaversBuilder.javers().registerValueObject(new ValueObjectDefinition(ManualRulesEntity.class, ignore)).build();
        Diff diff = javers.compare(dbEntity, dbNewEntity);
        List<ValueChange> valueChanges = diff.getChangesByType(ValueChange.class);

        valueChanges.forEach(valueChange -> {
            String oldValue = String.valueOf(valueChange.getLeft());
            String newValue = String.valueOf(valueChange.getRight());

            AuditLogCreate auditLog = AuditLogCreate.builder().auditSourceId(dbNewEntity.getId()).auditSourceType(AuditSourceTypeEnum.DIAGNOSTIC_ORDER.getDesc()).action(AuditActionEnum.CHANGE.getDesc())
                    .description(valueChange.getPropertyName()).newValue(newValue).oldValue(oldValue).createdBy(dbNewEntity.getModifiedBy()).createdDate(Timestamp.from(Instant.now())).version(version + 1).build();
            auditList.add(auditLog);
        });
        dbNewEntity.setModifiedDate(Timestamp.from(Instant.now()));
        dbNewEntity.setCreatedDate(dbEntity.getCreatedDate());
        dbNewEntity.setCreatedBy(dbEntity.getCreatedBy());
    }

    public List<DiagnosticOrderResponseDTO> getAllDiagnosticOrders() {
        List<DiagnosticOrderEntity> diagnosticOrderEntityList = diagnosticRepository.findAllByOrderByCreatedDateDesc();
        List<DiagnosticOrderResponseDTO> diagnosticOrderResponseDTOS = new ArrayList<>();
        for (DiagnosticOrderEntity diagnosticOrderEntity : diagnosticOrderEntityList) {
            DiagnosticOrderResponseDTO diagnosticOrderResponseDTO = convertEntityToDTO(diagnosticOrderEntity);
            diagnosticOrderResponseDTOS.add(diagnosticOrderResponseDTO);
        }
        if (diagnosticOrderEntityList.isEmpty()) {
            throw new DiagnosticOrderException("No diagnostic order found");
        }
        return diagnosticOrderResponseDTOS;
    }

    public DiagnosticOrderResponseDTO getDiagnosticOrderById(Long id) {
        DiagnosticOrderEntity doEntity =
                diagnosticRepository.findById(id)
                        .orElseThrow(() ->
                                new DiagnosticOrderException("Diagnostic order is not found for id: " + id));
        return convertEntityToDTO(doEntity);
    }


    public DiagnosticOrderEntity updateDiagnosticOrder(Long orderId, String user, String comment, boolean isApproved) {

        DiagnosticOrderEntity order =
                diagnosticRepository.findById(orderId)
                        .orElseThrow(() ->
                                new DiagnosticOrderException("Diagnostic order is not found for id: " + orderId));
        if (order.getStatus() != null && order.getStatus().equals("APPROVED")) {
            throw new DiagnosticOrderValidationException("Item has been a approved and is no longer editable");
        }

        if (isApproved) {
            order.setStatus(OrderStatus.ORDER_STATUS_APPROVED.getAction());
        } else {
            order.setStatus(OrderStatus.ORDER_STATUS_DECLINED.getAction());
        }
        order.setUpdatedUser(user);
        order.setUpdatedDate(Timestamp.from(Instant.now()));
        order.setComment(comment);

        return diagnosticRepository.save(order);
    }

    public List<DiagnosticOrderResponseDTO> filterDiagnosticOrder(List<Long> id, List<String> requesterName,
                                                             List<String> uploadingPoint, List<String> finVin,
                                                             List<String> partNumber, List<String> damageCode,
                                                             List<String> status, List<String> createdBy, List<String> createdDate) {
        Specification<DiagnosticOrderEntity> spec = diagnosticOrderSpecification.filterDiagnosticOrder(id, requesterName, uploadingPoint, finVin, partNumber, damageCode, status, createdBy, createdDate);
        List<DiagnosticOrderEntity> doEntityList = diagnosticRepository.findAll(spec);
        List<DiagnosticOrderResponseDTO> diagnosticOrderResponseDTOSList = new ArrayList<>();

        for (DiagnosticOrderEntity diagnosticOrderEntity : doEntityList) {
            DiagnosticOrderResponseDTO diagnosticOrderResponseDTO = convertEntityToDTO(diagnosticOrderEntity);
            diagnosticOrderResponseDTOSList.add(diagnosticOrderResponseDTO);
        }
        if (doEntityList.isEmpty()) {
            throw new DiagnosticOrderException("NO Diagnostic Order Found");
        }
        return diagnosticOrderResponseDTOSList ;
    }

    public List<DiagnosticOrderResponseDTO> searchDiagnosticOrder(String search) {
        Specification<DiagnosticOrderEntity> spec = diagnosticOrderSpecification.searchDiagnosticOrder(search);
        List<DiagnosticOrderEntity> doEntityList = diagnosticRepository.findAll(spec);
        List<DiagnosticOrderResponseDTO> diagnosticOrderResponseDTOSList = new ArrayList<>();
        for (DiagnosticOrderEntity diagnosticOrderEntity : doEntityList) {
            DiagnosticOrderResponseDTO diagnosticOrderResponseDTO = convertEntityToDTO(diagnosticOrderEntity);
            diagnosticOrderResponseDTOSList.add(diagnosticOrderResponseDTO);
        }
        if (doEntityList.isEmpty()) {
            throw new DiagnosticOrderException("NO Diagnostic Order Found");
        }
        return diagnosticOrderResponseDTOSList;
    }

    private DiagnosticOrderResponseDTO convertEntityToDTO(DiagnosticOrderEntity diagnosticOrderEntity) {
        return DiagnosticOrderResponseDTO.builder().
                id(diagnosticOrderEntity.getId())
                .requesterName(diagnosticOrderEntity.getRequesterName())
                .uploadingPoint(diagnosticOrderEntity.getUploadingPoint())
                .finVin(diagnosticOrderEntity.getFinVin())
                .damageCode(diagnosticOrderEntity.getDamageCode())
                .status(diagnosticOrderEntity.getStatus())
                .internalComment(diagnosticOrderEntity.getInternalComment())
                .externalComment(diagnosticOrderEntity.getExternalComment())
                .createdBy(diagnosticOrderEntity.getCreatedBy())
                .createdDate(diagnosticOrderEntity.getCreatedDate())
                .comment(diagnosticOrderEntity.getComment())
                .notes(diagnosticOrderEntity.getNotes())
                .partNumbers(getPartsNumberListDTO(diagnosticOrderEntity.getPartNumbers()))
                .build();
    }

    private static List<PartNumbersResponseDTO> getPartsNumberListDTO(List<DOPartNumberEntity> partNumbers) {
        return partNumbers.stream().map(partNumberEntity ->
                PartNumbersResponseDTO.builder()
                        .partId(partNumberEntity.getParts().getId())
                        .partNumber(partNumberEntity.getParts().getPartNumber())
                        .partName(partNumberEntity.getParts().getPartName())
                        .units(partNumberEntity.getUnits())
                        .arrived(partNumberEntity.getArrived()).build()).toList();
    }

    public DiagnosticOrderEntity addPartNumbers(List<DOPartNumberDTO> DOPartNumberDTOS, DiagnosticOrderEntity diagnosticOrderEntity) {
        if (DOPartNumberDTOS != null) {
            List<DOPartNumberEntity> partNumberEntities = DOPartNumberDTOS.stream()
                    .map(pn -> DOPartNumberEntity.builder().parts(getPartNumbersEntity(pn.getPartId())).order(diagnosticOrderEntity).units(pn.getUnits()).arrived(pn.getArrived()).build())
                    .toList();
            diagnosticOrderEntity.getPartNumbers().clear();
            diagnosticOrderEntity.getPartNumbers().addAll(partNumberEntities);
        }
        return diagnosticOrderEntity;
    }

    private PartNumbersEntity getPartNumbersEntity(Long partId) {
        return partNumbersRepository.findById(partId).orElse(null);
    }
}

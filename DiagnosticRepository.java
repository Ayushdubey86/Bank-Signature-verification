package com.mercedes.pris.diagnosticorder.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticRepository extends JpaRepository<DiagnosticOrderEntity, Long>, JpaSpecificationExecutor<DiagnosticOrderEntity> {

    List<DiagnosticOrderEntity> findAllByOrderByCreatedDateDesc();


}

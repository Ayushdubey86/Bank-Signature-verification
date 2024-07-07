package com.mercedes.pris.diagnosticorder.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.javers.core.metamodel.annotation.DiffIgnore;

import java.sql.Timestamp;
import java.util.List;

@Data
@Entity
@Table(name = "diagnostic_order")
@Builder
@AllArgsConstructor
@NoArgsConstructor
//@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosticOrderEntity {

    @Id
    @GeneratedValue(generator = "sequence-generator-diagnostic_order")
    @GenericGenerator(
            name = "sequence-generator-diagnostic_order",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                    @org.hibernate.annotations.Parameter(name = "sequence_name", value = "DIAGNOSTIC_ID_SEQ"),
                    @org.hibernate.annotations.Parameter(name = "initial_value", value = "1"),
                    @org.hibernate.annotations.Parameter(name = "increment_size", value = "1")
            }
    )
    private Long id;
    private String requesterName;
    private String uploadingPoint;
    private String finVin;
    private String damageCode;
    private String status;
    private String internalComment;
    private String externalComment;
    private String createdBy;
    private Timestamp createdDate;
    private String modifiedBy;
    private Timestamp modifiedDate;
    private String updatedUser;
    private Timestamp updatedDate;
    private String comment;
    private String notes;

    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @DiffIgnore
    //@JsonManagedReference
    List<DOPartNumberEntity> partNumbers;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("YourEntityClass{");
        sb.append("id=").append(id);
        sb.append(", requesterName='").append(requesterName).append('\'');
        sb.append(", uploadingPoint='").append(uploadingPoint).append('\'');
        sb.append(", finVin='").append(finVin).append('\'');
        sb.append(", damageCode='").append(damageCode).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append(", internalComment='").append(internalComment).append('\'');
        sb.append(", externalComment='").append(externalComment).append('\'');
        sb.append(", createdBy='").append(createdBy).append('\'');
        sb.append(", createdDate=").append(createdDate);
        sb.append(", modifiedBy='").append(modifiedBy).append('\'');
        sb.append(", modifiedDate=").append(modifiedDate);
        sb.append(", updatedUser='").append(updatedUser).append('\'');
        sb.append(", updatedDate=").append(updatedDate);
        sb.append(", comment='").append(comment).append('\'');
        sb.append(", notes='").append(notes).append('\'');
        sb.append(", partNumbers=").append(partNumbers);
        sb.append('}');
        return sb.toString();
    }
}




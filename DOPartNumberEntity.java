package com.mercedes.pris.diagnosticorder.persistence;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.mercedes.pris.common.persistence.PartNumbersEntity;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Data
@Entity
@Table(name = "do_part_number")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
//@ToString
public class DOPartNumberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private DiagnosticOrderEntity order;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "parts_id")
   // @JsonBackReference
    private PartNumbersEntity parts;
    private Integer arrived;
    private Integer units;
    private String createdBy;
    private Timestamp createdDate;

    @Override
    public String toString() {
        return "PartNumberEntity{" +
                "arrived=" + arrived +
                ", units=" + units +
                ", createdBy='" + createdBy + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }

}

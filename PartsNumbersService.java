package com.mercedes.pris.common.service;

import com.mercedes.pris.common.exception.PartsNumberException;
import com.mercedes.pris.common.persistence.PartNumbersEntity;
import com.mercedes.pris.common.persistence.PartNumbersRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PartsNumbersService {

    private final PartNumbersRepository partsNumbersRepository;

    public PartsNumbersService(PartNumbersRepository partNumbersRepository) {
        this.partsNumbersRepository = partNumbersRepository;
    }

    public List<PartNumbersEntity> getAllParts() {
        List<PartNumbersEntity> partsEntityList = partsNumbersRepository.findAll();
        if (partsEntityList.isEmpty()) {
            throw new PartsNumberException("No PartsNumber found");
        }
        return partsEntityList;
    }


}

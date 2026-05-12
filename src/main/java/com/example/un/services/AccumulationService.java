package com.example.un.services;

import com.example.un.dto.AccumulativeMessage;
import com.example.un.dto.AddCumulativeDto;
import com.example.un.models.Account;
import com.example.un.models.Cumulative;
import com.example.un.models.MountlyAccumulative;
import com.example.un.models.OperationType;
import com.example.un.repository.AccountRepository;
import com.example.un.repository.CumulativeRepository;
import com.example.un.repository.MountlyAccumulativeRepository;
import com.example.un.repository.ServiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AccumulationService {

    private final CumulativeRepository cRepo;
    private final AccountRepository accRepo;
    private final ServiceRepository servRepo;
    private final MountlyAccumulativeRepository mRepo;


    @Transactional
    public void applyAccumulative(AccumulativeMessage message){
        AddCumulativeDto dto = new AddCumulativeDto();
        dto.setAccountId(message.getAccountId());
        dto.setServiceId(message.getServiceId());
        dto.setOperationType(message.getOperationType());
        dto.setAmount(message.getAmount());
        addCumulative(dto.getAccountId(), dto);
    }

    @Transactional
    public AddCumulativeDto addCumulative(Long accountId, AddCumulativeDto dto){
        Account acc = accRepo.findById(accountId).orElseThrow(()->
                new ResponseStatusException(HttpStatus.BAD_REQUEST,"account has not found"));

        LocalDate operationMonth = getOperationMonth(dto);
        LocalDate startOfMonth = operationMonth.withDayOfMonth(1);

        Cumulative cumulative = new Cumulative();

        BigDecimal prevValue = mRepo.findPreviousByAccountIdAndServiceId(
                dto.getAccountId(), dto.getServiceId(), startOfMonth).map(MountlyAccumulative :: getValue)
                .orElse(BigDecimal.ZERO);

        MountlyAccumulative mountly = mRepo.findByAccountIdAndServiceIdAndMonthYear(dto.getAccountId(),
                dto.getServiceId(),
                startOfMonth).orElse(null);

        cumulative.setAccountId(dto.getAccountId());
        cumulative.setServiceId(dto.getServiceId());
        cumulative.setOperationType(dto.getOperationType());

        BigDecimal currentValue = (mountly != null) ? mountly.getValue() : prevValue;

        if (dto.getOperationType() == OperationType.CUMULATIVE_ADD) {
            BigDecimal deltaVolume = dto.getAmount();
            BigDecimal newVolume = currentValue.add(deltaVolume);
            BigDecimal moneyCharge = deltaVolume.multiply(servRepo.getServiceCostById(dto.getServiceId())).setScale(2, RoundingMode.HALF_UP);

            acc.setBalance(acc.getBalance().subtract(moneyCharge));

            if (mountly == null) {
                mountly = new MountlyAccumulative();
                mountly.setAccountId(dto.getAccountId());
                mountly.setServiceId(dto.getServiceId());
                mountly.setMonthYear(startOfMonth);
            }
            mountly.setValue(newVolume);
            mountly.setTarif(servRepo.getServiceCostById(dto.getServiceId()));
            cumulative.setAmount(newVolume);

        } else if (dto.getOperationType() == OperationType.CUMULATIVE_SET) {
            BigDecimal newVolume = dto.getAmount();
            BigDecimal baseValue = (mountly != null) ? mountly.getValue() : prevValue;
            BigDecimal deltaVolume = newVolume.subtract(baseValue);
            BigDecimal moneyChange = deltaVolume.multiply(servRepo.getServiceCostById(dto.getServiceId())).setScale(2, RoundingMode.HALF_UP);

            acc.setBalance(acc.getBalance().subtract(moneyChange));

            if (mountly == null) {
                mountly = new MountlyAccumulative();
                mountly.setAccountId(dto.getAccountId());
                mountly.setServiceId(dto.getServiceId());
                mountly.setMonthYear(startOfMonth);
            }
            mountly.setValue(newVolume);
            mountly.setTarif(servRepo.getServiceCostById(dto.getServiceId()));
            cumulative.setAmount(newVolume);
        }
        mRepo.save(mountly);
        cRepo.save(cumulative);
        accRepo.save(acc);
        return mapToDto(accountId,cumulative);
    }


    public AddCumulativeDto mapToDto(Long accountId, Cumulative cumulative){
        AddCumulativeDto dto = new AddCumulativeDto();
        dto.setAccountId(accountId);
        dto.setServiceId(cumulative.getServiceId());
        dto.setAmount(cumulative.getAmount());
        dto.setOperationType(cumulative.getOperationType());
        return dto;
    }
    private LocalDate getOperationMonth(AddCumulativeDto dto) {
        return dto.getCreateAt() != null ? dto.getCreateAt() : LocalDate.now().withDayOfMonth(1);
    }
}

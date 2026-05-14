package com.example.un.services;

import com.example.un.dto.CreateOperDto;
import com.example.un.dto.RefactorOperDto;
import com.example.un.dto.ServiceDto;
import com.example.un.dto.TransactionMessage;
import com.example.un.models.Account;
import com.example.un.models.Operation;
import com.example.un.models.OperationType;
import com.example.un.models.SnapshotBalance;
import com.example.un.repository.AccountRepository;
import com.example.un.repository.OperationRepository;
import com.example.un.repository.SnapshotBalanceRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accRepo;
    private final OperationRepository operRepo;
    private final SnapshotBalanceRepository sBRepo;

    private final Counter orderCounter;
    private final MeterRegistry registry;


    public TransactionService(AccountRepository accRepo, OperationRepository operRepo, SnapshotBalanceRepository sBRepo, MeterRegistry registry){
        this.accRepo = accRepo;
        this.operRepo = operRepo;
        this.sBRepo = sBRepo;
        this.orderCounter =Counter.builder("operation.orders.total")
                .description("Total number of orders processed")
                .tags("type","premium")
                .register(registry);
        this.registry = registry;
    }

    @Transactional
    public void applyTransaction(TransactionMessage message){
        CreateOperDto dto = new CreateOperDto();
        dto.setAccountId(message.getAccountId());
        dto.setAmount(message.getAmount());
        dto.setOperationType(message.getOperationType());
        createBaseOper(message.getAccountId(), dto);
    }

    //Create base operation(PAYMENT, CHARGE)
    @Transactional
    public CreateOperDto createBaseOper(Long accountId, CreateOperDto dto){
        Timer.Sample sample = Timer.start(registry);
        try {


        Account acc = accRepo.findById(accountId).orElseThrow(()->
                new ResponseStatusException(HttpStatus.BAD_REQUEST,"accountID didn`t find"));
        Operation oper = new Operation();
        oper.setAmount(dto.getAmount());
        oper.setOperationType(dto.getOperationType());
        oper.setAccountId(acc.getId());
        oper.setReferenceId(
                dto.getReferenceId() != null ? dto.getReferenceId() : UUID.randomUUID().toString()
        );
        oper.setCreatedAt(LocalDateTime.now());
        if (acc.getBalance() == null){
            acc.setBalance(BigDecimal.ZERO);
        }
        if (dto.getOperationType() == OperationType.CHARGE){
            if (acc.getBalance().compareTo(dto.getAmount())<0){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"insufficient funds");
            }
            acc.setBalance(acc.getBalance().subtract(oper.getAmount()));
        }
        else if(dto.getOperationType() == OperationType.PAYMENT){
            if (dto.getAmount().compareTo(BigDecimal.ZERO)<=0){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"the replenishment cannot be greater than zero");
            }
            acc.setBalance(acc.getBalance().add(oper.getAmount()));
        }
        orderCounter.increment();
        accRepo.save(acc);
        Operation saved = operRepo.save(oper);
        return mapToDto(saved,acc.getId());
        }finally {
            sample.stop(Timer.builder("my.method.execution.time")
                    .description("Time taken")
                    .register(registry));
        }
    }

    //Edit base operation
    @Transactional
    public RefactorOperDto refactorBaseOper(RefactorOperDto dto,Long accountId,Long operationId){
        Account acc = accRepo.findById(accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "account id didn`t found"));
        Operation oper = operRepo.findById(operationId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "operation is doesn`t exist"));

        BigDecimal oldAmount = oper.getAmount();
        OperationType oldOper = oper.getOperationType();

        BigDecimal newAmount = dto.getAmount();
        OperationType newOper = dto.getOperationType();

        BigDecimal oldE, newE;

        if (newAmount.compareTo(BigDecimal.ZERO) <=0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"amount must be positive");
        }
        if(oldOper == OperationType.PAYMENT) oldE = oldAmount;
        else oldE = oldAmount.negate();

        if (newOper == OperationType.PAYMENT) newE = newAmount;
        else newE = newAmount.negate();

        BigDecimal delta = newE.subtract(oldE);
        BigDecimal newBalance = acc.getBalance().add(delta);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "insufficient funds after operation change");
        acc.setBalance(newBalance);
        oper.setOperationType(newOper);
        oper.setAmount(newAmount);
        accRepo.save(acc);
        Operation saved = operRepo.save(oper);
        return mapToDto(saved,accountId, oper.getId());
    }

    //Delete base operation
    @Transactional
    public void deleteBaseOper(Long accountId, Long operationId){
        Account acc = accRepo.findById(accountId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "account id didn`t found"));
        Operation oper = operRepo.findById(operationId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "operation is doesn`t exist"));
        if (oper.getOperationType() == OperationType.PAYMENT) acc.setBalance(acc.getBalance().subtract(oper.getAmount()));
        else acc.setBalance(acc.getBalance().add(oper.getAmount()));

        operRepo.deleteById(operationId);
        accRepo.save(acc);
    }

    @Transactional
    public BigDecimal getBalanceOnDate(Long accountId, LocalDate targetDate) {
        Optional<SnapshotBalance> lastSnapshotOpt = sBRepo
                .findTopByAccountIdAndSnapshotDateLessThanEqualOrderBySnapshotDateDesc(accountId, targetDate);

        BigDecimal baseBalance;
        LocalDate startDate;

        if (lastSnapshotOpt.isPresent()) {
            SnapshotBalance snapshot = lastSnapshotOpt.get();
            baseBalance = snapshot.getBalance();
            startDate = snapshot.getSnapshotDate().plusDays(1);
        } else {
            LocalDateTime createdAt = accRepo.findCreatedAtById(accountId); // метод без блокировки
            baseBalance = BigDecimal.ZERO;
            startDate = (createdAt != null) ? createdAt.toLocalDate() : LocalDate.of(1970, 1, 1);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = targetDate.atTime(23, 59, 59);
        BigDecimal delta = operRepo.sumAmountByAccountAndDateRange(accountId, startDateTime, endDateTime);
        return baseBalance.add(delta).setScale(2, RoundingMode.HALF_UP);
    }

    public CreateOperDto mapToDto(Operation operation,Long accountId){
        CreateOperDto dto = new CreateOperDto();
        dto.setAccountId(accountId);
        dto.setOperationId(operation.getId());
        dto.setAmount(operation.getAmount());
        dto.setOperationType(operation.getOperationType());
        return dto;
    }
    public RefactorOperDto mapToDto(Operation operation,Long accountId, Long operationId){
        RefactorOperDto dto = new RefactorOperDto();
        dto.setOperationId(operation.getId());
        dto.setAmount(operation.getAmount());
        dto.setOperationType(operation.getOperationType());
        return dto;
    }
}
